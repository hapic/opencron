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
import org.opencron.common.exception.CycleDependencyException;
import org.opencron.common.exception.DBException;
import org.opencron.common.exception.InvalidException;
import org.opencron.common.exception.ParameterException;
import org.opencron.common.graph.KahnTopo;
import org.opencron.common.graph.Node;
import org.opencron.common.job.Opencron;
import org.opencron.common.utils.CommonUtils;
import org.opencron.server.dao.QueryDao;
import org.opencron.server.domain.*;
import org.opencron.server.job.OpencronCollector;
import org.opencron.server.job.OpencronTools;
import org.opencron.server.tag.PageBean;
import org.opencron.server.until.GraphUntils;
import org.opencron.server.vo.JobVo;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.*;

import static org.opencron.common.job.Opencron.*;
import static org.opencron.common.utils.CommonUtils.contains;
import static org.opencron.common.utils.CommonUtils.notEmpty;

@Service
@Transactional
public class JobService {

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private AgentService agentService;

    @Autowired
    private UserService userService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private OpencronCollector opencronCollector;

    @Autowired
    private JobDependenceService jobDependenceService;

    @Autowired
    private JobActionGroupService jobActionGroupService;

    @Autowired
    private JobGroupService jobGroupService;

    @Autowired
    private RecordService recordService;

    private Logger logger = LoggerFactory.getLogger(JobService.class);

    public Job getJob(Long jobId) {
        return queryDao.get(Job.class, jobId);
    }

    public JobVo getJobVo(Long jobId) {
        String sql="SELECT * FROM `t_job` a WHERE a.`jobId`=?";
        List<JobVo> jobVos = queryDao.sqlQuery(JobVo.class, sql, jobId);
        if(jobVos!=null){
            return jobVos.get(0);
        }
        return null;
    }

    /**
     * 获取将要执行的任务，每次都根节点出发加载
     *
     * @return
     */
    public List<JobVo> getJobVo(ExecType execType, CronType cronType) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.ip,D.password FROM T_JOB AS T " +
                "LEFT JOIN T_AGENT AS D " +
                "ON T.agentId = D.agentId " +
                "WHERE IFNULL(T.flowNum,0)=0 " +
                "AND cronType=? " +
                "AND execType=? " +
                "AND T.deleted=0 " +
                "AND T.pause=0";
        List<JobVo> jobs = queryDao.sqlQuery(JobVo.class, sql, cronType.getType(), execType.getStatus());
        //查询更多的任务
        queryJobMore(jobs);
        return jobs;
    }

    public List<JobVo> getJobVoByAgentId(Agent agent, ExecType execType, CronType cronType) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.ip,D.password FROM T_JOB AS T " +
                "INNER JOIN T_AGENT D " +
                "ON T.agentId = D.agentId " +
                "WHERE IFNULL(T.flowNum,0)=0 " +
                "AND cronType=? " +
                "AND execType = ? " +
                "AND T.deleted=0 " +
                "AND T.pause=0 " +
                "AND D.agentId=? ";

        List<JobVo> jobs = queryDao.sqlQuery(JobVo.class, sql, cronType.getType(), execType.getStatus(), agent.getAgentId());
        queryJobMore(jobs);
        return jobs;
    }

    private void queryJobMore(List<JobVo> jobs) {
        if (CommonUtils.notEmpty(jobs)) {
            for (JobVo job : jobs) {
                job.setAgent(agentService.getAgent(job.getAgentId()));
//                queryChildren(job);//查询子任务
                queryJobUser(job);//查询job的执行者
            }
        }
    }

    public List<Job> getJobsByJobType(HttpSession session, JobType jobType) {
        String sql = "SELECT * FROM T_JOB WHERE deleted=0 AND jobType=?";
        if (JobType.FLOW.equals(jobType)) {
            sql += " AND flowNum=0";
        }
        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            sql += " AND userId = " + user.getUserId() + " AND agentId IN (" + user.getAgentIds() + ")";
        }
        return queryDao.sqlQuery(Job.class, sql, jobType.getCode());
    }

    public List<JobVo> getCrontabJob() {
        logger.info("[opencron] init quartzJob...");
        return getJobVo(Opencron.ExecType.AUTO, Opencron.CronType.CRONTAB);
    }

    public List<Job> getAll() {
        List<Job> jobs = OpencronTools.CACHE.get(OpencronTools.CACHED_JOB_ID, List.class);
        if (CommonUtils.isEmpty(jobs)) {
            flushJob();
        }
        return OpencronTools.CACHE.get(OpencronTools.CACHED_JOB_ID, List.class);
    }

    public  List<Job> getAll(Long groupId) {
        String sql="SELECT * FROM T_JOB WHERE deleted=0";
        if(groupId!=null){
            sql+=" and groupId="+groupId;
        }
        return queryDao.sqlQuery(Job.class, sql);

    }


    private synchronized void flushJob() {
        OpencronTools.CACHE.put(OpencronTools.CACHED_JOB_ID, queryDao.sqlQuery(Job.class, "SELECT * FROM T_JOB WHERE deleted=0"));
    }

    public PageBean<JobVo> getJobVos(HttpSession session, PageBean pageBean, JobVo job) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.ip,D.password,U.userName AS operateUname " +
                "FROM T_JOB AS T " +
                "LEFT JOIN T_AGENT AS D " +
                "ON T.agentId = D.agentId " +
                "LEFT JOIN T_USER AS U " +
                "ON T.userId = U.userId " +
                "WHERE T.deleted=0 ";

        if (job != null) {
            if (notEmpty(job.getGroupId())) {
                sql += " AND T.groupId=" + job.getGroupId();
            }
            if (notEmpty(job.getAgentId())) {
                sql += " AND T.agentId=" + job.getAgentId();
            }
            if (notEmpty(job.getCronType())) {
                sql += " AND T.cronType=" + job.getCronType();
            }
            if (notEmpty(job.getJobType())) {
                sql += " AND T.jobType=" + job.getJobType();
            }
            if (notEmpty(job.getExecType())) {
                sql += " AND T.execType=" + job.getExecType();
            }
            if (notEmpty(job.getRedo())) {
                sql += " AND T.redo=" + job.getRedo();
            }
            if (!OpencronTools.isPermission(session)) {
                User user = OpencronTools.getUser(session);
                sql += " AND T.userId = " + user.getUserId() + " AND T.agentId IN (" + user.getAgentIds() + ")";
            }
        }
        pageBean = queryDao.getPageBySql(pageBean, JobVo.class, sql);
//        List<JobVo> parentJobs = pageBean.getResult();
//
//       /* for (JobVo parentJob : parentJobs) {
//            queryChildren(parentJob);
//        }*/
////        pageBean.setResult(parentJobs);
        return pageBean;
    }

    private List<JobVo> queryChildren(JobVo job) {
        if (job.getJobType().equals(JobType.FLOW.getCode())) {
            String sql = "SELECT T.*,D.name AS agentName,D.port,D.ip,D.password,U.userName AS operateUname FROM T_JOB AS T " +
                    "LEFT JOIN T_AGENT AS D " +
                    "ON T.agentId = D.agentId " +
                    "LEFT JOIN T_USER AS U " +
                    "ON T.userId = U.userId " +
                    "WHERE T.deleted=0 " +
                    "AND T.flowId = ? " +
                    "AND T.flowNum>0 " +
                    "ORDER BY T.flowNum ASC";//按
            List<JobVo> childJobs = queryDao.sqlQuery(JobVo.class, sql, job.getFlowId());
            if (CommonUtils.notEmpty(childJobs)) {
                for (JobVo jobVo : childJobs) {
                    jobVo.setAgent(agentService.getAgent(jobVo.getAgentId()));
                }
            }
            job.setChildren(childJobs);
            return childJobs;
        }
        return Collections.emptyList();
    }


    public Job merge(Job job) {
        Job saveJob = (Job) queryDao.merge(job);
        flushJob();
        return saveJob;
    }

    public JobVo getJobVoById(Long id) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.ip,D.password,U.username AS operateUname ,tjg.`name` groupName" +
                " FROM T_JOB AS T " +
                "LEFT JOIN T_AGENT AS D " +
                "ON T.agentId = D.agentId " +
                "LEFT JOIN T_USER AS U " +
                "ON T.userId = U.userId " +
                "LEFT JOIN t_job_group tjg " +
                "ON t.`groupId`=tjg.`id` "+
                "WHERE T.jobId =?";
        JobVo job = queryDao.sqlUniqueQuery(JobVo.class, sql, id);
        if (job == null) {
            return null;
        }
        queryJobMore(Arrays.asList(job));
        return job;
    }

    private void queryJobUser(JobVo job) {
        if (job != null && job.getUserId() != null) {
            User user = userService.getUserById(job.getUserId());
            job.setUser(user);
        }
    }

    public List<JobVo> getJobByAgentId(Long agentId,Long groupId) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.ip,D.password,U.userName AS operateUname FROM T_JOB AS T " +
                "LEFT JOIN T_USER AS U " +
                "ON T.userId = U.userId " +
                "LEFT JOIN T_AGENT D " +
                "ON T.agentId = D.agentId " +
                "WHERE T.agentId =?";
                if(groupId!=null){
                    sql+=" and t.groupId="+groupId;
                }
        return queryDao.sqlQuery(JobVo.class, sql, agentId);
    }

    public boolean existsName(Long jobId, Long agentId, String name) {
        String sql = "SELECT COUNT(1) FROM T_JOB WHERE agentId=? AND deleted=0 AND jobName=? ";
        if (notEmpty(jobId)) {
            sql += " AND jobId != " + jobId + " AND flowId != " + jobId;
        }
        return (queryDao.getCountBySql(sql, agentId, name)) > 0L;
    }

    public String checkDelete(Long id) {
        Job job = getJob(id);
        if (job == null) {
            return "error";
        }

        //该任务是否正在执行中
        String sql = "SELECT COUNT(1) FROM T_RECORD WHERE jobId = ? AND `status`=?";
        Long count = queryDao.getCountBySql(sql, id, RunStatus.RUNNING.getStatus());
        if (count > 0) {
            return "false";
        }

        //流程任务则检查任务流是否在运行中...
       /* if (job.getJobType() == JobType.FLOW.getCode()) {
            sql = "SELECT COUNT(1) FROM T_RECORD AS R INNER JOIN (" +
                    " SELECT J.jobId FROM T_JOB AS J INNER JOIN T_JOB AS F" +
                    " ON J.flowId = F.flowId" +
                    " WHERE f.jobId = ?" +
                    " ) AS J" +
                    " on R.jobId = J.jobId" +
                    " and R.status=?";
            count = queryDao.getCountBySql(sql, id, RunStatus.RUNNING.getStatus());
            if (count > 0) {
                return "false";
            }
        }*/

        int childCount = this.jobDependenceService.childCount(job.getJobId());
        if(childCount>0){
            return "child";
        }

        return "true";
    }


    @Transactional(rollbackFor = Exception.class)
    public void delete(Long jobId) throws SchedulerException {
        Job job = getJob(jobId);
        if (job != null) {
            //单一任务,直接执行删除
            String sql = "UPDATE T_JOB SET deleted=1 WHERE ";
            sql += " jobId=" + jobId;

            /*if (job.getJobType().equals(JobType.SINGLETON.getCode())) {

            }
            if (job.getJobType().equals(JobType.FLOW.getCode())) {
                if (job.getFlowNum() == 0) {
                    //顶层流程任务,则删除一组
                    sql += " groupId=" + job.getGroupId();
                } else {
                    //其中一个子流程任务,则删除单个
                    sql += " jobId=" + jobId;
                }
            }*/
            queryDao.createSQLQuery(sql).executeUpdate();

            int parentJobCount=this.jobDependenceService.breakParentShip(jobId);
            logger.info("delete job:{} child job count:{}",jobId,parentJobCount);

            refreshJobFowNum(job);


            schedulerService.syncJobTigger(jobId, null);
            flushJob();
        }
    }


    public void saveFlowJob(Job job, List<Job> children) throws SchedulerException {
        job.setLastChild(false);
        job.setUpdateTime(new Date());
        job.setFlowNum(0);//顶层sort是0
        /**
         * 保存最顶层的父级任务
         */
        if (job.getJobId() != null) {
            merge(job);
            /**
             * 当前作业已有的子作业
             */
            JobVo jobVo = new JobVo();
            jobVo.setJobType(JobType.FLOW.getCode());
            jobVo.setFlowId(job.getFlowId());

            /**
             * 取差集..
             */
            List<JobVo> hasChildren = queryChildren(jobVo);
            //数据库里已经存在的子集合..
            top:
            for (JobVo hasChild : hasChildren) {
                //当前页面提交过来的子集合...
                for (Job child : children) {
                    if (child.getJobId() != null && child.getJobId().equals(hasChild.getJobId())) {
                        continue top;
                    }
                }
                /**
                 * 已有的子作业被删除的,则做删除操作...
                 */
                delete(hasChild.getJobId());
            }
        } else {
            Job job1 = merge(job);
            job1.setFlowId(job1.getJobId());//flowId
            merge(job1);
            job.setJobId(job1.getJobId());
        }

        for (int i = 0; i < children.size(); i++) {
            Job child = children.get(i);
            /**
             * 子作业的流程编号都为顶层父任务的jobId
             */
            child.setFlowId(job.getJobId());
            child.setUserId(job.getUserId());
            child.setUpdateTime(new Date());
            child.setJobType(JobType.FLOW.getCode());
            child.setFlowNum(i + 1);
            child.setLastChild(child.getFlowNum() == children.size());
            child.setWarning(job.getWarning());
            child.setMobiles(job.getMobiles());
            child.setEmailAddress(job.getEmailAddress());
            merge(child);
        }
    }

    public boolean checkJobOwner(HttpSession session, Long userId) {
        return OpencronTools.isPermission(session) || userId.equals(OpencronTools.getUserId(session));
    }

    public boolean pauseJob(Job jobBean) {

        Job job = this.getJob(jobBean.getJobId());

        if (jobBean.getPause()==null) return false;

        if ( job.getPause()!=null && jobBean.getPause().equals(job.getPause())) {
            return false;
        }

        //必须是自动执行.
        if ( !job.getExecType().equals(ExecType.AUTO.getStatus()) ) {
            return false;
        }

        //如果有待执行的记录也标记为暂停
       /* List<Record> listRecord=this.recordService.loadPendingRecordByJobId(job.getJobId());

        for(Record record:listRecord){
            int i = this.recordService.updateRecord(record.getRecordId(), RunStatus.PENDING.getStatus(), RunStatus.STOPED.getStatus());
            logger.info("stop job:{} record:{} result:{}",job.getJobId(),record.getRecordId(),i);
        }*/

        CronType cronType = CronType.getByType(job.getCronType());

        switch (cronType) {
            case QUARTZ:
                try {
                    if (jobBean.getPause()) {
                        //暂停任务
                        schedulerService.pause(jobBean.getJobId());
                    }else {
                        //恢复任务
                        schedulerService.resume(jobBean.getJobId());
                    }
                    job.setPause(jobBean.getPause());
                    merge(job);
                    return true;
                }catch (SchedulerException e) {
                    logger.error("[opencron] pauseQuartzJob error:{}",e.getLocalizedMessage());
                    return false;
                }
            case CRONTAB:
                try {
                    if (jobBean.getPause()) {
                        opencronCollector.removeTask(jobBean.getJobId());
                    }else {
                        JobVo jobVo = getJobVoById(job.getJobId());
                        opencronCollector.addTask(jobVo);
                    }
                    job.setPause(jobBean.getPause());
                    merge(job);
                    return true;
                }catch (Exception e) {
                    logger.error("[opencron] pauseCrontabJob error:{}",e.getLocalizedMessage());
                    return false;
                }
        }
        return true;
    }

    /**
     * 保存当前Job，和添加依赖关系
     * @param jobVo
     * @param dependentJobs
     */
    public Job saveDependentJobs(Job jobVo, List<Job> dependentJobs) {

        //保存当前job
        Job job = this.merge(jobVo);
        jobVo.setJobId(job.getJobId());
        logger.info("job id:{}",job.getJobId());


        //保存依赖关系，并修改前置job的类型
        if(dependentJobs!=null && dependentJobs.size()>0){
            /**
             * 保存依赖关系
             */
            for(Job dependentJob:dependentJobs){
                logger.info("dependentJob Id:{}",dependentJob.getJobId());
                JobDependence dependence= new JobDependence();
                dependence.setDependenceJobId(dependentJob.getJobId());
                dependence.setJobId(job.getJobId());
                dependence.setGroupId(job.getGroupId());
                dependence.setStatus(DependenceStatus.NORMAL.getValue());
                dependence.setUpdateTime(new Date());
                jobDependenceService.merge(dependence);//保存依赖关系

                //修改依赖job的类型
                /**
                 * 如果已经是最后子节点 或者 不是流式任务，则修改
                 */
                dependentJob.setLastChild(false);//前置任务不是最后一个子节点
                /*if( dependentJob.getLastChild() ==null
                        || dependentJob.getLastChild()
                        || !dependentJob.getJobType().equals(JobType.FLOW.getCode())){
                    dependentJob.setJobType(JobType.FLOW.getCode());

                }*/
                this.merge(dependentJob);
            }
        }
        return job;
    }

    private int updateJobFlowNum(Long jobId, int level) {
        String sql="UPDATE `t_job` tj " +
                "SET tj.`flowNum`=? ,tj.`lastChild`=IF(flowNum=0,FALSE,TRUE) " +
                "WHERE tj.`jobId`=?";
        return queryDao.createSQLQuery(sql,level,jobId).executeUpdate();
    }


    public int loadJobCountByGroupId(Long groupId) {
        String sql="SELECT count(1) FROM `t_job` tj  " +
                "WHERE tj.`deleted`=0 AND tj.`pause`=0 AND tj.`groupId`=?";
        Long countBySql = this.queryDao.getCountBySql(sql, groupId);
        return countBySql==null ? 0 : countBySql.intValue();
    }

    /**
     * 更新依赖关系
     * @param job
     * @param dependentJob
     */
    public void updateDependence(Job job, List<Job> dependentJob) {
        Long jobId = job.getJobId();

        //修改依赖关系
        if(CommonUtils.notEmpty(dependentJob)){//如果
            for(Job depJob:dependentJob){
                Long depJobId = depJob.getJobId();
                DBOperate dbOperate = jobDependenceService.updateOrInsert(jobId, depJobId,job.getGroupId());
                logger.info("jobId:{},depJobId:{} operate:{}",jobId,dependentJob,dbOperate);
            }
            refreshJobFowNum(job);
        }

        this.merge(job);



    }

    /**
     * 刷新job的flowNum
     * @param job
     */
    public void refreshJobFowNum(Job job){
        List<JobDependence> jobDependenceList =jobDependenceService.loadDependence(job.getGroupId());

        KahnTopo<Long> graph = GraphUntils.createGraph(jobDependenceList);
        if(graph.hasCycle()){//如果有环形依赖，则事务回滚
            throw new CycleDependencyException("更新失败,有任务间循环依赖!");
        }
        logger.info("update flowNum by Job:{}",job.getJobId());
        Iterable<Node<Long>> result = graph.getResult();
        this.batchUpdateJobFlowNum(result);
    }






    /**
     * 根据组id查询所有可用的job
     * @param groupId
     * @return
     */
    public List<JobVo> loadJobByGroupId(Long groupId,Long notJobId) {

        String sql="SELECT * FROM t_job tj " +
                "WHERE tj.`deleted`=0 AND tj.groupId=? "+
                " and tj.jobId!=?";
        if(notJobId==null){
            notJobId=0L;
        }
        return this.queryDao.sqlQuery(JobVo.class,sql,groupId,notJobId);
    }



    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initRootPendingJob(JobVo job, Long actionId) {
        if(job.getGroupId()==null){
            logger.info("inset action:{} no group job:{}",actionId,job.getJobName());
            this.recordService.insertPendingReocrd(actionId,job);
            return;
        }
        //初始化Task记录
        DBOperate dbOperate = this.jobActionGroupService.updateActionGroup(job, actionId);
        if(dbOperate==DBOperate.INSERT){//只有成功插入才可以初始化这些pending的Job
            logger.info("current job:{} actionId:{} will insert into pending record ! ",job.getJobName(),actionId);

            if(job.getGroupId()!=null){
                List<JobVo> rootJobList=this.loadRootJobByGroupId(job.getGroupId());
                for(JobVo rootJob:rootJobList){
                    logger.info("insert job:{},actionId:{} status:pending ",rootJob.getJobName(),actionId);
                    this.recordService.insertPendingReocrd(actionId,rootJob);
                }
            }else{
                logger.info("action:{} job:{} no groupId ",actionId,job.getJobName());
            }

        }else{
            logger.info("current job:{} actionId:{} insert fail ! ",job.getJobName(),actionId);
        }
    }

    /**
     *  根据组加载起始任务的节点
     * @param groupId
     * @return
     */
    public List<JobVo> loadRootJobByGroupId(Long groupId) {
        String sql="SELECT * FROM `t_job` tj " +
                "WHERE tj.`deleted`=0 and  tj.`pause`=0 AND tj.`groupId`=? and tj.flowNum=0";
        return this.queryDao.sqlQuery(JobVo.class,sql,groupId);
    }


    /**
     * 批量更新job的flowNum值
     * @param result
     */
    public void batchUpdateJobFlowNum(Iterable<Node<Long>> result) {
        //更新flowNum值
        Iterator<Node<Long>> iterator = result.iterator();
        while (iterator.hasNext()){
            Node<Long> next = iterator.next();
            Long jobId = next.getVal();
            int level = next.getLevel();
            int i=this.updateJobFlowNum(jobId,level);
            logger.info("update job:{},jobFlowNum:{} ,result:{}",jobId,level,i);
        }
    }


    public List<Job> loadUsedJobByGroupId(Long groupId) {
        String sql="SELECT * FROM `t_job` tj " +
                "WHERE tj.`deleted`=0 AND tj.`groupId`=?";
        return this.queryDao.sqlQuery(Job.class,sql,groupId);
    }

    public void batchIinitJob(List<Job> jobVos) {
        for(Job vo:jobVos){
            Job merge = this.merge(vo);
            vo.setJobId(merge.getJobId());
            logger.info("insert into job:{} name:{} success!",vo.getJobId(),vo.getJobName());
        }
    }
}
