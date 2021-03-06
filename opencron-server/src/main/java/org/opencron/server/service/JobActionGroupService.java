package org.opencron.server.service;
/**
 * @Package org.opencron.server.service
 * @Title: JobActionGroupService
 * @author hapic
 * @date 2018/4/4 14:03
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.opencron.common.exception.ParameterException;
import org.opencron.common.exception.UnknownException;
import org.opencron.common.job.Opencron;
import org.opencron.common.utils.CommonUtils;
import org.opencron.server.DBException;
import org.opencron.server.dao.QueryDao;
import org.opencron.server.domain.JobActionGroup;
import org.opencron.server.domain.JobGroup;
import org.opencron.server.domain.Record;
import org.opencron.server.vo.JobActionGroupVo;
import org.opencron.server.vo.JobVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.Date;
import java.util.List;

/**
 * @Descriptions:
 */
@Service
@Transactional
@Slf4j
public class JobActionGroupService {
    @Autowired
    private QueryDao queryDao;

    @Autowired
    private JobGroupService jobGroupService;

    @Autowired
    private RecordService recordService;


    public JobActionGroup loadActionGroupByActionId(Long actionId){
        String sql="SELECT * FROM t_job_action_group tjag " +
                "WHERE tjag.actionId=? limit 1";
        return queryDao.sqlUniqueQuery(JobActionGroup.class,sql,actionId);
    }

    public JobActionGroup merge(JobActionGroup actionGroup){
        return (JobActionGroup)queryDao.merge(actionGroup);
    }

    public void updateActionGroupParam(Long actionId,String param){
        if(org.opencron.common.utils.StringUtils.isNotNullString(param)){
            JobActionGroup jobActionGroup = loadActionGroupByActionId(actionId);
            jobActionGroup.setParam(param);
            this.merge(jobActionGroup);
        }
    }


    /**
     * 加载运行中的分组任务
     * @return
     */
    public List<JobActionGroup> loadRunningGroup(int offSet,int limit){
        Integer status=Opencron.RunStatus.RUNNING.getStatus();

        String sql="SELECT * FROM `t_job_action_group`  tjag " +
                "WHERE tjag.`status`=? and tjag.id<? order by id desc limit "+limit;
        return queryDao.sqlQuery(JobActionGroup.class,sql,status,offSet);
    }

    public void finishActionGroup(Long id, Opencron.RunStatus status, Opencron.ExecType execType) {
        JobActionGroup actionGroup = queryDao.get(JobActionGroup.class, id);
        actionGroup.setEndateTime(new Date());
        actionGroup.setEndType(execType.getStatus());
        actionGroup.setStatus(status.getStatus());
        this.queryDao.merge(actionGroup);
    }

    public void setActionGroupToJob(JobVo job, Long actionId) {
        if(job.getActionGroup()==null){
            JobActionGroup actionGroup = loadActionGroupByActionId(actionId);
            if(actionGroup==null){
                throw new UnknownException("not found "+actionId+" action group!");
            }
            job.setActionGroup(actionGroup);
        }
    }
    /**
     * 更新当前执行组的状态
     * @param job
     * @param actionId
     */
    public Opencron.DBOperate updateActionGroup(JobVo job, Long actionId) {

        JobActionGroup actionGroup = loadActionGroupByActionId(actionId);
        if(actionGroup!=null){
            log.info("aleady exits actionGroup:{}",actionId);
            return Opencron.DBOperate.SELECT;
        }

        try {
            actionGroup= new JobActionGroup();
            actionGroup.setGroupId(job.getGroupId());
            actionGroup.setActionId(actionId);
            actionGroup.setStartTime(new Date());
            actionGroup.setStatus(Opencron.RunStatus.RUNNING.getStatus());
            actionGroup=merge(actionGroup);
            log.info("insert into actionGroup:{}",actionGroup.getId());
        } catch (Exception e) {
            DBException.business(e,"UK_ACTIONID");
            return Opencron.DBOperate.SELECT;
        }
        return Opencron.DBOperate.INSERT;
    }

    public Long acquire(JobVo job) {
        if(job==null){
            throw  new org.opencron.common.exception.DBException("Job not found!");
        }
        if(job.getGroupId()==null){
            log.info("job:{} no groupId",job.getJobName());
            return  CommonUtils.groupId();
        }

        String sql="SELECT tjg.* FROM t_job_group tjg " +
                "LEFT JOIN t_job tj " +
                "ON tj.groupId=tjg.id " +
                "WHERE tj.`jobId`=?";
        JobGroup jobGroup = this.queryDao.sqlUniqueQuery(JobGroup.class, sql, job.getJobId());
        if(jobGroup==null){
            throw  new org.opencron.common.exception.DBException("job group not found!");
        }else if(jobGroup.getActionId()==null){
            if(job.getGroupId()==null){
                throw new ParameterException("groupId not null!");
            }
            Long id = jobGroup.getId();
            Long actionId = CommonUtils.groupId();
            jobGroup =jobGroupService.updateGroupActionId(id, actionId, jobGroup.getVersion());
            log.info("job:{},actionId:{}",id,jobGroup.getActionId());
            return jobGroup.getActionId();
        }
        return  jobGroup.getActionId();

    }

    public Long loadLastActionId(Long groupId) {
        String sql="SELECT ag.actionId FROM `t_job_action_group` ag " +
                "WHERE ag.`groupId`=? ORDER BY id DESC LIMIT 1";
        return this.queryDao.getCountBySql(sql,groupId);
    }

    public List<JobActionGroupVo> loadFinishedGroup(String beginTime, String endTime) {
        String sql="SELECT tjag.*,tjg.`name` groupName FROM `t_job_action_group` tjag,t_job_group tjg " +
                "WHERE tjag.`startTime` BETWEEN ? AND ? " +
                "AND tjag.`status`=1 AND tjag.`alarm`=0 AND tjg.`id`=tjag.`groupId`;";
        return this.queryDao.sqlQuery(JobActionGroupVo.class,sql,beginTime,endTime);
    }

    public int updateActionGroupAlarm(Long id, int fromAlarm, int toAlarm) {
        String sql="UPDATE `t_job_action_group` tjag " +
                "SET tjag.`alarm`=? " +
                "WHERE tjag.`alarm`=? AND tjag.`id`=?";
        return this.queryDao.createSQLQuery(sql,toAlarm,fromAlarm,id).executeUpdate();
    }

    public Record updateActionAndSetParam(Long actionId, JobVo job, String param) {
        this.updateActionGroup(job, actionId);
        this.setActionGroupToJob(job,actionId);
        this.updateActionGroupParam(actionId,param);
       return this.recordService.insertPendingReocrd(actionId, job,Opencron.RunStatus.PENDING);
    }
}
