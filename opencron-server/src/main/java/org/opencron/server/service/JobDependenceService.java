package org.opencron.server.service;
/**
 * @Package org.opencron.server.service
 * @Title: JobDependenceService
 * @author hapic
 * @date 2018/4/2 15:29
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;
import org.opencron.common.job.Opencron;
import org.opencron.server.dao.QueryDao;
import org.opencron.server.domain.Job;
import org.opencron.server.domain.JobDependence;
import org.opencron.server.domain.Record;
import org.opencron.server.vo.JobVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Descriptions:
 */
@Service
@Transactional
@Slf4j
public class JobDependenceService {

    @Autowired
    private QueryDao queryDao;


    /**
     * 保存或更新记录
     * @param job
     * @return
     */
    public JobDependence merge(JobDependence job) {
        JobDependence saveJob =(JobDependence)queryDao.merge(job);
        return saveJob;
    }

    /**
     * 根据job查询可用的前置依赖job
     * @param jobId
     * @return
     */
    public List<JobVo> dependentJob(Long jobId) {
        String sql="SELECT tj.* FROM t_job_dependence tjd " +
                "LEFT JOIN t_job tj " +
                "ON tjd.`dependenceJobId`=tj.`jobId` " +
                "WHERE tjd.`status`=1 AND tjd.`jobId`=? ";
        return queryDao.sqlQuery(JobVo.class, sql, jobId);
    }
    public List<Job> dependentJob2(Long jobId) {
        String sql="SELECT tj.* FROM t_job_dependence tjd " +
                "LEFT JOIN t_job tj " +
                "ON tjd.`dependenceJobId`=tj.`jobId` " +
                "WHERE tjd.`status`=1 AND tjd.`jobId`=? ";
        return queryDao.sqlQuery(Job.class, sql, jobId);
    }

    /**
     * 根据jobid 和 组id 查询执行的记录状态
     * @param jobId
     * @param groupId
     * @return
     */
    public List<Record> dependentJobRecord(Long jobId, Long groupId) {
        String sql="SELECT tr.* FROM `t_job_dependence` tjd " +
                "LEFT JOIN `t_record` tr " +
                "ON tjd.`dependenceJobId`=tr.`jobId`  " +
                "WHERE tjd.`status`=1 AND tjd.`jobId`=? AND tr.`groupId`=?";
        return queryDao.sqlQuery(Record.class,sql,jobId,groupId);
    }

    /**
     * 加载子节点job
     * @param jobId
     * @return
     */
    public List<JobVo> childsNodeJob(Long jobId) {
        String sql="SELECT tj.* FROM t_job_dependence tjd " +
                "LEFT JOIN t_job tj " +
                "ON tjd.`jobId`=tj.`jobId` " +
                "WHERE tjd.`status`=1 AND tjd.`dependenceJobId`=?";

        return queryDao.sqlQuery(JobVo.class,sql,jobId);
    }

    /**
     * 加载所有的子节点
     * @param jobId
     * @return
     */
    public List<Long> childsAllNodeJob(Long jobId) {
        String sql="SELECT tjd.`jobId` FROM `t_job_dependence` tjd " +
                "WHERE tjd.`status`=1 AND tjd.`dependenceJobId`=?";

        List<BigInteger> jobs = queryDao.createSQLQuery(sql, jobId).list();
        List<Long> jobList= new ArrayList<>();
        for(BigInteger childJobId:jobs){
            //如果还有孩子节点
            if(childCount(childJobId.longValue())>0){
                List<Long> childsJob = childsAllNodeJob(childJobId.longValue());
                if(childsJob!=null){
                    jobList.addAll(childsJob);
                }
            }else{
                jobList.add(childJobId.longValue());
            }
        }
        return jobList;

    }

    /**
     * 查询子节点的个数
     * @param jobId
     * @return
     */
    public int childCount(Long jobId){
        String sql="SELECT COUNT(1) FROM `t_job_dependence` tjd " +
                "WHERE tjd.`status`=1 AND tjd.`dependenceJobId`=?";
        Long countBySql = queryDao.getCountBySql(sql, jobId);
        return countBySql==null ? 0 :countBySql.intValue();
    }

    public Opencron.DBOperate  updateOrInsert(Long jobId, Long depJobId,Long groupId) {
        int updateCount=this.updateDepenceStatus(jobId,depJobId, Opencron.DependenceStatus.DELETGE);
        if(updateCount>0){//如果有更新记录，则说明有
            return Opencron.DBOperate.UPDATE;
        }
        log.info("save dependenc job:{} depJobId:{},groupId:{}",jobId,depJobId,groupId);
        JobDependence dependence= new JobDependence();
        dependence.setDependenceJobId(depJobId);
        dependence.setJobId(jobId);
        dependence.setStatus(Opencron.DependenceStatus.NORMAL.getValue());
        dependence.setGroupId(groupId);
        dependence.setUpdateTime(new Date());
        this.merge(dependence);//保存依赖关系
        return Opencron.DBOperate.INSERT;
    }

    /**
     * 更新依赖关系的状态
     * @param jobId
     * @param depJobId
     * @param status
     * @return
     */
    private int updateDepenceStatus(Long jobId, Long depJobId, Opencron.DependenceStatus status) {
        String update="UPDATE `t_job_dependence` tjd " +
                "SET tjd.`status`=? " +
                "WHERE tjd.`jobId`=? AND tjd.`dependenceJobId`=? and tjd.`status`=1";

        return this.queryDao.createSQLQuery(update,status.getValue(),jobId,depJobId).executeUpdate();
    }


    /**
     * 加载当前组的依赖关系
     * @param groupId
     * @return
     */
    public List<JobDependence> loadDependence(Long groupId) {

        String sql="SELECT * FROM `t_job_dependence` tjd " +
                "WHERE tjd.groupId=? AND tjd.`status`=?";

        return this.queryDao.sqlQuery(JobDependence.class,sql,groupId,Opencron.DependenceStatus.NORMAL.getValue());
    }

    /**
     * 删除孩子节点
     * @param jobId
     */
    public int deleteChild(Long jobId) {
        String sql="UPDATE `t_job_dependence` tjd " +
                "SET tjd.`status`=2 " +
                "WHERE tjd.`dependenceJobId`=?";
        return this.queryDao.createSQLQuery(sql,Opencron.DependenceStatus.DELETGE.getValue(),jobId).executeUpdate();

    }

    /**
     * 断开依赖job的关系
     * @param jobId
     * @return
     */
    public int breakParentShip(Long jobId) {
        String sql="UPDATE `t_job_dependence` tjd " +
                "SET tjd.`status`=? " +
                "WHERE tjd.`jobId`=?";
        return this.queryDao.createSQLQuery(sql,Opencron.DependenceStatus.DELETGE.getValue(),jobId).executeUpdate();
    }

    public List<JobVo> loadJobByGroupId(Long groupId,Long actionId) {
        StringBuffer sb=new StringBuffer("SELECT tj.`jobId`,tj.`jobName`,tjd.dependenceJobId " );
        if(actionId!=null){
            sb.append(",tr.`status`,tr.`success` " );
        }

        sb.append("FROM t_job tj   " )
                .append(
        "LEFT JOIN (SELECT jd.`dependenceJobId`,jd.`jobId` FROM t_job_dependence jd WHERE jd.`status`=1 AND jd.`groupId`=?) tjd  " +
        "ON tj.`jobId`=tjd.`jobId`  " );
        if(actionId!=null){
            sb.append( "LEFT JOIN (SELECT tr.`recordId`,tr.`actionId`,tr.`jobId`,tr.`status`,tr.`success`  FROM `t_record` tr " +
                    "WHERE  " +
                    "tr.`recordId` IN (SELECT MAX(B.recordId) AS rid  FROM `t_record` B " +
                    "WHERE B.`actionId`="+actionId+" GROUP BY B.`jobId`  )  " +
                    "AND tr.actionId="+actionId+" )  tr ON tr.`jobId`=tj.`jobId`  ");
        }
        sb.append(" WHERE tj.`groupId`=? AND tj.`deleted`=0  ");

        return this.queryDao.sqlQuery(JobVo.class,sb.toString(),groupId,groupId);
    }
}
