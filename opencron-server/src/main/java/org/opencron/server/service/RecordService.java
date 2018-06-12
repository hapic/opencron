/**
 * Copyright 2016 benjobs
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.opencron.server.service;

import com.sun.org.apache.regexp.internal.RE;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.opencron.common.job.Opencron;
import org.opencron.common.utils.ParamUntils;
import org.opencron.common.utils.StringUtils;
import org.opencron.server.DBException;
import org.opencron.server.dao.QueryDao;
import org.opencron.server.domain.JobActionGroup;
import org.opencron.server.domain.Record;
import org.opencron.server.job.OpencronTools;
import org.opencron.server.domain.User;
import org.opencron.server.tag.PageBean;
import org.opencron.server.until.Parser;
import org.opencron.server.vo.ChartVo;
import org.opencron.server.vo.JobVo;
import org.opencron.server.vo.RecordVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

import static org.opencron.common.utils.CommonUtils.notEmpty;

@Service
@Transactional
@Slf4j
public class RecordService {

    @Autowired
    private QueryDao queryDao;

    public PageBean query(HttpSession session, PageBean<RecordVo> pageBean, RecordVo recordVo, String queryTime, boolean status) {
        String sql = "SELECT R.recordId,R.jobId,R.command,R.success,R.startTime,R.status,R.redoCount,R.jobType,R.groupId,R.actionId," +
                "CASE WHEN R.status IN (1,3,5,6) THEN R.endTime WHEN R.status IN (0,2,4,7) THEN NOW() END AS endTime," +
                "R.execType,T.jobName,T.agentId,D.name AS agentName,D.password,D.ip,T.cronExp,U.userName AS operateUname FROM T_RECORD AS R " +
                "LEFT JOIN T_JOB AS T " +
                "ON R.jobId = T.jobId " +
                "LEFT JOIN T_AGENT AS D " +
                "ON R.agentId = D.agentId " +
                "LEFT JOIN T_USER AS U " +
                "ON R.userId = U.userId " +
                "AND CASE R.jobType WHEN 1 THEN R.flowNum=0 WHEN 0 THEN R.parentId IS NULL END " +
                "WHERE 1=1 ";

        if (recordVo != null) {
            if (notEmpty(recordVo.getStatus())) {//执行状态
                sql += " AND R.status = " + recordVo.getStatus() + "";
            }else{
                sql+="AND R.status IN " + (status ? "(1,3,4,5,6)" : "(0,2,4,7)");
            }

            if (notEmpty(recordVo.getSuccess())) {
                sql += " AND R.success = " + recordVo.getSuccess() + "";
            }
            if (notEmpty(recordVo.getAgentId())) {
                sql += " AND R.agentId = " + recordVo.getAgentId() + " ";
            }
            if (notEmpty(recordVo.getJobId())) {
                sql += " AND R.jobId = " + recordVo.getJobId() + " ";
            }
            if (notEmpty(queryTime)) {
                sql += " AND date_format(R.startTime,'%Y-%m-%d')='"+queryTime+"'";
            }
            if (notEmpty(recordVo.getExecType())) {
                sql += " AND R.execType = " + recordVo.getExecType() + " ";
            }
            if (notEmpty(recordVo.getGroupId())) {
                sql += " AND R.groupId = " + recordVo.getGroupId() + " ";
            }
            /*if (status) {
                sql += " AND IFNULL(R.flowNum,0) = 0 ";
            }*/
            if (!OpencronTools.isPermission(session)) {
                User user = OpencronTools.getUser(session);
                sql += " AND R.userId = " + user.getUserId() + " AND R.agentId in (" + user.getAgentIds() + ")";
            }
        }
        sql += " ORDER BY R.startTime DESC";
        queryDao.getPageBySql(pageBean, RecordVo.class, sql);
        /*if (status) {
            //已完成任务的子任务及重跑记录查询
            queryChildrenAndRedo(pageBean);
        }*/
        return pageBean;
    }

    private void queryChildrenAndRedo(PageBean<RecordVo> pageBean) {
        List<RecordVo> parentRecords = pageBean.getResult();
        for (RecordVo parentRecord : parentRecords) {
            String sql = "SELECT R.recordId,R.jobId,R.jobType,R.startTime,R.endTime,R.execType,R.status,R.redoCount,R.command,R.success,T.jobName,D.name AS agentName,D.password,D.ip,T.cronExp,U.userName AS operateUname FROM T_RECORD AS R " +
                    "INNER JOIN T_JOB AS T " +
                    "ON R.jobId = T.jobId " +
                    "LEFT JOIN T_AGENT AS D " +
                    "ON T.agentId = D.agentId " +
                    "LEFT JOIN T_USER AS U " +
                    "ON T.userId = U.userId " +
                    "WHERE R.parentId = ? " +
                    "ORDER BY R.startTime ASC ";

            //单一任务有重跑记录的，查出后并把最后一条重跑记录的执行结果记作整个任务的成功、失败状态
            if (parentRecord.getJobType() == Opencron.JobType.SINGLETON.getCode() && parentRecord.getRedoCount() > 0) {
                List<RecordVo> records = queryDao.sqlQuery(RecordVo.class, sql, parentRecord.getRecordId());
                parentRecord.setSuccess(records.get(records.size() - 1).getSuccess());
                parentRecord.setChildRecord(records);
            }
            //流程任务，先查出父任务的重跑记录，再查出各个子任务，最后查询子任务的重跑记录，并以最后一条记录的执行结果记作整个流程任务的成功、失败状态
            if (parentRecord.getJobType() == Opencron.JobType.FLOW.getCode()) {
                if (parentRecord.getRedoCount() != 0) {
                    List<RecordVo> records = queryDao.sqlQuery(RecordVo.class, sql, parentRecord.getRecordId());
                    //流程任务不能保证子任务也有记录，先给父任务一个成功、失败状态
                    parentRecord.setSuccess(Opencron.ResultStatus.FAILED.getStatus());
                    parentRecord.setChildRecord(records);
                }
                sql = "SELECT R.recordId,R.jobId,R.jobType,R.startTime,R.endTime,R.execType,R.status,R.redoCount,R.command,R.success,R.groupId,T.jobName,T.lastChild,D.name as agentName,D.password,D.ip,T.cronExp,U.userName AS operateUname FROM T_RECORD AS R " +
                        "INNER JOIN T_JOB AS T " +
                        "ON R.jobId = T.jobId " +
                        "LEFT JOIN T_AGENT AS D " +
                        "ON T.agentId = D.agentId " +
                        "LEFT JOIN T_USER AS U " +
                        "ON T.userId = U.userId " +
                        "WHERE R.parentId IS NULL " +
                        "AND R.groupId = ? " +
                        "AND R.flowNum > 0 " +
                        "ORDER BY R.flowNum ASC ";
                List<RecordVo> childJobs = queryDao.sqlQuery(RecordVo.class, sql, parentRecord.getGroupId());
                if (notEmpty(childJobs)) {
                    parentRecord.setChildJob(childJobs);
                    for (RecordVo childJob : parentRecord.getChildJob()) {
                        if (childJob.getRedoCount() > 0) {
                            sql = "SELECT R.recordId,R.jobId,R.jobType,R.startTime,R.endTime,R.execType,R.status,R.redoCount,R.command,R.success,R.parentId,T.jobName,D.name AS agentName,D.password,D.ip,T.cronExp,U.userName AS operateUname FROM T_RECORD AS R " +
                                    "INNER JOIN T_JOB AS T " +
                                    "ON R.jobId = T.jobId " +
                                    "LEFT JOIN T_AGENT AS D " +
                                    "ON T.agentId = D.agentId " +
                                    "LEFT JOIN T_USER AS U " +
                                    "ON T.userId = U.userId " +
                                    "WHERE R.parentId = ? " +
                                    "ORDER BY R.startTime ASC ";
                            List<RecordVo> childRedo = queryDao.sqlQuery(RecordVo.class, sql, childJob.getRecordId());
                            childJob.setChildRecord(childRedo);
                        }

                    }
                    //判断整个流程任务最终执行的成功、失败状态
                    RecordVo lastJob = childJobs.get(childJobs.size() - 1);
                    if (lastJob.getLastChild()) {
                        if (notEmpty(lastJob.getChildRecord())) {
                            parentRecord.setSuccess(lastJob.getChildRecord().get(lastJob.getChildRecord().size() - 1).getSuccess());
                        } else {
                            parentRecord.setSuccess(lastJob.getSuccess());
                        }
                    } else {
                        parentRecord.setSuccess(Opencron.ResultStatus.FAILED.getStatus());
                    }
                }
            }
        }
        pageBean.setResult(parentRecords);
    }

    public RecordVo getDetailById(Long id) {
        return queryDao.sqlUniqueQuery(
                RecordVo.class,
                "SELECT R.recordId,R.jobType,R.jobId,R.startTime,R.endTime,R.execType,R.returnCode,R.message,R.redoCount,R.command,R.success,T.jobName,T.agentId,D.name AS agentName,D.password,D.ip,T.cronExp,T.userId,U.userName AS operateUname FROM T_RECORD AS R " +
                        "LEFT JOIN T_JOB AS T " +
                        "ON R.jobId = T.jobId " +
                        "LEFT JOIN T_AGENT AS D " +
                        "ON R.agentId = D.agentId " +
                        "LEFT JOIN T_USER AS U " +
                        "ON R.userId = U.userId " +
                        "WHERE R.recordId = ?",
                id);
    }

    public Record merge(Record record) {
        record = (Record) queryDao.merge(record);
//        String sql = "UPDATE T_RECORD SET startTime=NOW() WHERE recordId=?";
//        queryDao.createSQLQuery(sql,record.getRecordId()).executeUpdate();
        return record;
    }

    public int updateRecordStartTime(Long recordId){
        String sql = "UPDATE T_RECORD SET startTime=NOW() WHERE recordId=?";
           return queryDao.createSQLQuery(sql,recordId).executeUpdate();
    }

    public Record get(Long recordId) {
        return queryDao.get(Record.class, recordId);
    }

    /**
     * 只查询单一任务的
     *
     * @return
     */
    public List<Record> getReExecuteRecord() {
        String sql = "SELECT R.*,D.ip,D.`name` AS agentName,D.password FROM T_RECORD AS R " +
                "INNER JOIN T_AGENT AS D " +
                "ON R.agentId = D.agentId " +
                "WHERE R.success=0 " +
                "AND R.jobType=0 " +
                "AND R.status IN(1,5) " +
                "AND R.parentId IS NULL " +
                "AND R.redo=1 " +
                "AND R.redoCount<R.runCount ";
        return queryDao.sqlQuery(Record.class, sql);
    }

    public Boolean isRunning(Long id) {
        return queryDao.getCountBySql("SELECT COUNT(1) FROM T_RECORD AS R " +
                "LEFT JOIN T_JOB AS T " +
                "ON R.jobId = T.jobId  " +
                "WHERE (R.jobId = ? OR T.flowId = ?) " +
                "AND R.status IN (0,2,4,7) ", id, id) > 0L;
    }

    public List<ChartVo> getRecord(HttpSession session, String startTime, String endTime) {
        String sql = "SELECT DATE_FORMAT(r.startTime,'%Y-%m-%d') AS date, " +
                "sum(CASE r.success WHEN 0 THEN 1 ELSE 0 END) failure, " +
                "sum(CASE r.success WHEN 1 THEN 1 ELSE 0 END) success, " +
                "sum(CASE r.success WHEN 2 THEN 1 ELSE 0 END) killed, " +
                "sum(CASE r.jobType WHEN 0 THEN 1 ELSE 0 END) singleton, " +
                "sum(CASE r.jobType WHEN 1 THEN 1 ELSE 0 END) flow, " +
                "sum(CASE j.cronType WHEN 0 THEN 1 ELSE 0 END) crontab, " +
                "sum(CASE j.cronType WHEN 1 THEN 1 ELSE 0 END) quartz, " +
                "sum(CASE r.execType WHEN 0 THEN 1 ELSE 0 END) auto, " +
                "sum(CASE r.execType WHEN 1 THEN 1 ELSE 0 END) operator, " +
                "sum(CASE r.redoCount>0 WHEN 1 THEN 1 ELSE 0 END) rerun " +
                "FROM T_RECORD r left join T_JOB j ON r.jobid=j.jobid " +
                "WHERE DATE_FORMAT(r.startTime,'%Y-%m-%d') " +
                "BETWEEN '" + startTime + "' AND '" + endTime + "'";
        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            sql += " AND r.userId = " + user.getUserId() + " AND r.agentId in (" + user.getAgentIds() + ")";
        }
        sql += " GROUP BY DATE_FORMAT(r.startTime,'%Y-%m-%d') ORDER BY DATE_FORMAT(r.startTime,'%Y-%m-%d') ASC";
        return queryDao.sqlQuery(ChartVo.class, sql);
    }

    public ChartVo getAsProgress(HttpSession session) {
        String sql = "SELECT " +
                "sum(CASE R.success WHEN 0 THEN 1 ELSE 0 END) failure, " +
                "sum(CASE R.success WHEN 1 THEN 1 ELSE 0 END) success, " +
                "sum(CASE R.success WHEN 2 THEN 1 ELSE 0 END) killed, " +
                "sum(CASE R.jobType WHEN 0 THEN 1 ELSE 0 END) singleton, " +
                "sum(CASE R.jobType WHEN 1 THEN 1 ELSE 0 END) flow, " +
                "sum(CASE J.cronType WHEN 0 THEN 1 ELSE 0 END) crontab, " +
                "sum(CASE J.cronType WHEN 1 THEN 1 ELSE 0 END) quartz, " +
                "sum(CASE R.execType WHEN 0 THEN 1 ELSE 0 END) auto, " +
                "sum(CASE R.execType WHEN 1 THEN 1 ELSE 0 END) operator, " +
                "sum(CASE R.redoCount>0 WHEN 1 THEN 1 ELSE 0 END) rerun " +
                "FROM T_RECORD R LEFT JOIN T_JOB J ON R.jobid=J.jobid WHERE 1=1 ";

        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            sql += " AND R.userId = " + user.getUserId() + " AND R.agentId in (" + user.getAgentIds() + ")";
        }
        return queryDao.sqlUniqueQuery(ChartVo.class, sql);
    }

    public void flowJobDone(Record record) {
        /**
         *
         * String sql = "UPDATE T_RECORD SET status=?,success=? WHERE actionId=?";
         * queryDao.createSQLQuery(sql, Opencron.RunStatus.DONE.getStatus(),Opencron.ResultStatus.KILLED.getStatus(), record.getActionId()).executeUpdate();
         *
         */
//
    }

    /**
     * 根据记录ID，查询当前轮次对应在所有记录
     * @param recordId
     * @return
     */
    public List<Record> getRunningFlowJob(Long recordId) {
        String sql = "SELECT R.* FROM T_RECORD AS R INNER JOIN (SELECT actionId FROM T_RECORD WHERE recordId=?) AS T " +
                "WHERE R.actionId = T.actionId and R.status in (0,7)";
        return queryDao.sqlQuery(Record.class, sql, recordId);
    }


    /**
     * 获取作业数
     * @param status
     * @param startDate
     * @param endDate
     * @return
     */
    public Long getTask( int status,String startDate,String endDate){
        String sql;
        if (status == 1) {
            sql = "SELECT COUNT(DISTINCT(jobId)) FROM T_RECORD WHERE success=?   AND STATUS IN (?,?,?)  ";
        } else {
            sql = "SELECT COUNT(DISTINCT(jobId)) FROM T_RECORD WHERE success<>?   AND STATUS IN (?,?,?) ";
        }

        if (notEmpty(startDate, endDate)) {
            sql+=" AND insertDate BETWEEN '"+startDate+"' AND  DATE_ADD('"+endDate+"',INTERVAL 1 DAY)";
        }
        return queryDao.getCountBySql(sql, 1,
                Opencron.RunStatus.STOPED.getStatus(),Opencron.RunStatus.DONE.getStatus(),Opencron.RunStatus.RERUNDONE.getStatus());

    }

    public Long getRecords(HttpSession session, int status, Opencron.ExecType execType,String startDate,String endDate) {
        String sql;
        if (status == 1) {
            sql = "SELECT COUNT(1) FROM T_RECORD WHERE success=? AND execType=? AND STATUS IN (?,?,?)  ";
        } else {
            sql = "SELECT COUNT(1) FROM T_RECORD WHERE success<>? AND execType=? AND STATUS IN (?,?,?) ";
        }
        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            sql += " AND userId = " + user.getUserId() + " AND agentid IN (" + user.getAgentIds() + ")";
        }
        if (notEmpty(startDate, endDate)) {
            sql+=" AND insertDate BETWEEN '"+startDate+"' AND  DATE_ADD('"+endDate+"',INTERVAL 1 DAY)";
        }


        return queryDao.getCountBySql(sql, 1, execType.getStatus(),
                Opencron.RunStatus.STOPED.getStatus(),Opencron.RunStatus.DONE.getStatus(),Opencron.RunStatus.RERUNDONE.getStatus());
    }

    @Transactional
    public void deleteRecordBetweenTime(String startTime, String endTime) {
        if (notEmpty(startTime, endTime)) {
            String sql = "DELETE FROM T_RECORD WHERE DATE_FORMAT(startTime,'%Y-%m-%d') BETWEEN ? AND ?";
            queryDao.createSQLQuery(sql, startTime, endTime).executeUpdate();
        }
    }

    public Record getReRunningSubJob(Long recordId) {
        String sql = "SELECT * FROM T_RECORD WHERE parentId = ? ORDER BY redoCount DESC LIMIT 1";
        return queryDao.sqlUniqueQuery(Record.class, sql, recordId);
    }

    /**
     * 从一个状态更新到另一个状态
     * @param recordId
     * @param fromStatus
     * @param toStatus
     * @return
     */
    public int updateRecord(Long recordId, Integer fromStatus, Integer toStatus) {
        String updateSet="UPDATE t_record tr SET tr.`status`=? WHERE tr.`recordId`=? AND tr.`status`=?";
         return queryDao.createSQLQuery(updateSet, toStatus,recordId,fromStatus).executeUpdate();

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateRecordRequiresNew(Long recordId, Integer fromStatus, Integer toStatus){
        return updateRecord(recordId,fromStatus,toStatus);
    }

    /**
     * 根据
     * @param actionId
     * @param jobId
     * @return
     */
    public Record getRecord(Long actionId, Long jobId) {
        String sql="SELECT * FROM `t_record` tr " +
                "WHERE tr.`actionId`=? AND tr.`jobId`=? order by recordId desc LIMIT 1";
        return  queryDao.sqlUniqueQuery(Record.class, sql, actionId,jobId);
    }

    public Record getRecord(Long actionId,JobVo job){
        if(job.getRecordId()==null){
            return getRecord(actionId,job.getJobId());
        }
        return this.get(job.getRecordId());


    }

    /**
     * 插入pending状态的执行状态
     * @param jobVo
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Record insertPendingReocrd(Long actionId,JobVo jobVo,Opencron.RunStatus runStatus) {
        jobVo.setActionId(actionId);
        //先判断是否已经保存过了
        Record record = this.getRecord(actionId, jobVo);
        log.info("actionId:{} jobId:{} record:{}",actionId, jobVo.getJobId(),record);
//        if(record!=null && record.getStatus().equals(Opencron.RunStatus.PENDING.getStatus())){//如果可以查找，则表示已经保存过了
        if(record!=null ){//如果可以查找，则表示已经保存过了
            log.info("job:{} record:{} has exits!",jobVo.getJobName(),record.getRecordId());
            return record;
        }
        record=insertRecord(jobVo,runStatus);

        return record;
    }

    /**
     *
     * @param childJob
     * @return
     */
    public Record insertRecord(JobVo childJob,Opencron.RunStatus status) {
        Record record=null;
        Long actionId = childJob.getActionId();
        try {

            record=new Record(childJob);
            record.setStatus(status.getStatus());//默认待执行
            record.setGroupId(childJob.getGroupId());
            record.setActionId(actionId);
            record.setParentId(childJob.getRecordId());
            record.setJobType(childJob.getJobType());

            JobActionGroup actionGroup = childJob.getActionGroup();

            String parse=record.getCommand();
            if(actionGroup!=null && StringUtils.isNotNullString(actionGroup.getParam())){
                childJob.setParam(actionGroup.getParam());
            }
             parse = Parser.parse(childJob);
            log.info("job:{},command:{}",childJob.getJobName(),parse);
            record.setCommand(parse);

            String code = DigestUtils.md5Hex(record.getStatus() + record.getJobId() + record.getActionId()+"");
            record.setUniqueCode(code);
            record = this.merge(record);
            log.info("by actionId:{} jobId:{} insert into new reocrd:{},cmd:{}",actionId, childJob.getJobId(),record.getRecordId(),record.getCommand());
        } catch (Exception e) {
            e.printStackTrace();
            DBException.business(e,"UK_UNIQUECODE");
            return this.getRecord(actionId, childJob.getJobId());
        }
        return record;
    }

    /**
     * 加载处于Pending状态的执行记录
     * @return
     */
    public List<Record> loadPendingRecord(int offSet,int limit) {
        String sql="SELECT R.*,D.ip,D.`name` AS agentName,D.password FROM T_RECORD AS R  " +
                "INNER JOIN T_AGENT AS D  " +
                "ON R.agentId = D.agentId  " +
                "WHERE R.status=7 " +//AND recordId<?
                "ORDER BY R.`weight` DESC,R.flowNum DESC, R.recordId DESC LIMIT ?";
        return this.queryDao.sqlQuery(Record.class,sql,limit);
    }

    /**
     * 获取运行中的状态
     * @return
     */
    public int runningRecordCount() {

        String sql="SELECT COUNT(1) FROM `t_record` tr " +
                "WHERE tr.`status`in(?,?)";

        Integer[] stats={Opencron.RunStatus.RUNNING.getStatus(),Opencron.RunStatus.RERUNNING.getStatus()};
        Long countBySql = this.queryDao.getCountBySql(sql, stats);

        return  countBySql==null ? 0 : countBySql.intValue();
    }

    /**
     * 查询成功的记录个数
     * @param actionId
     * @param actionId
     * @return
     */
    public int loadSuccessRecordCount(Long jobId,Long actionId) {
        String sql="SELECT COUNT(1)  FROM `t_record` tr " +
                "WHERE tr.`status`in(1,3,6) and tr.success=1 AND tr.actionId=? and tr.jobId=?";
        Long countBySql = this.queryDao.getCountBySql(sql, actionId,jobId);

        return countBySql==null ? 0 : countBySql.intValue();
    }

    /**
     *
     * @param jobId
     * @return
     */
    public List<Record> loadPendingRecordByJobId(Long jobId) {
        String sql="SELECT * FROM `t_record` tr " +
                "WHERE tr.`status`=? AND tr.`jobId`=?";
        return this.queryDao.sqlQuery(Record.class,sql,Opencron.RunStatus.PENDING.getStatus(),jobId);

    }


    /**
     *
     * @param beforeTime
     * @return
     */
    public List<RecordVo> loadUnfinishedRecord(String beforeTime) {
        String sql="SELECT tj.`jobName`,tr.`startTime` " +
                "FROM `t_record` tr,`t_job` tj " +
                "WHERE tr.`status` IN(0,4) " +
                "AND tr.`jobId`=tj.`jobId` " +
                "AND tr.`startTime`<?";
        return this.queryDao.sqlQuery(RecordVo.class,sql,beforeTime);
    }

    /**
     * 加载最新的一个执行记录
     * @param actionId
     * @param jobId
     * @return
     */
    public Record loadLastRecord(Long actionId, Long jobId) {
        String sql="SELECT * FROM `t_record` tr " +
                "WHERE tr.`jobId`=? AND tr.`actionId`=? " +
                "ORDER BY recordId DESC " +
                "LIMIT 1";
        return this.queryDao.sqlUniqueQuery(Record.class,sql,jobId,actionId);
    }

    public Record updateOldRecorAndInsertNewRecord(Record record, JobVo job) {
        record.setStatus(Opencron.RunStatus.RERUNDONE.getStatus());//设置状态为重做
        this.merge(record);//修改当前状态为重做

        job.setRecordId(record.getRecordId());
        Record newRecord = insertRecord(job, Opencron.RunStatus.PENDING);

        Long recordId = newRecord.getRecordId();
        job.setRecordId(recordId);
        log.info("insert record:{} from record:{}",newRecord.getRecordId(),record.getRecordId());
        return newRecord;


    }


    public Record loadLastRecord(Long jobId) {
        String sql="SELECT * FROM `t_record` tr " +
                "WHERE tr.`jobId`=? " +//AND tr.`status`=1 AND tr.`success`!=1
                "ORDER BY recordId DESC " +
                "LIMIT 1";
        return this.queryDao.sqlUniqueQuery(Record.class,sql,jobId);
    }

    public Long loadRootDoneCount(Long actionId) {
        String sql="SELECT COUNT(tr.`recordId`)  " +
                " FROM t_record tr " +
                " WHERE tr.`actionId`=? AND tr.`flowNum`=0 AND tr.`status`=1";
        return this.queryDao.getCountBySql(sql,actionId);
    }

    public List<Record> loadRecord(Long[] recordIds) {
        String sql="SELECT * FROM `t_record` tr " +
                "WHERE tr.`recordId` IN (";
        for(int i=0;i<recordIds.length;i++){
            if(i==recordIds.length-1){
                sql+="?";
            }else{
                sql+="?,";
            }
        }
        sql+=")";
        return  this.queryDao.sqlQuery(Record.class,sql,recordIds);


    }
}
