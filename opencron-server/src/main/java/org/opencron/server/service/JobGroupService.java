package org.opencron.server.service;
/**
 * @Package org.opencron.server.service
 * @Title: JobLinkService
 * @author hapic
 * @date 2018/4/3 20:03
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;
import org.opencron.common.exception.DBException;
import org.opencron.common.exception.ParameterException;
import org.opencron.common.utils.CommonUtils;
import org.opencron.server.dao.QueryDao;
import org.opencron.server.domain.Group;
import org.opencron.server.domain.Job;
import org.opencron.server.domain.JobGroup;
import org.opencron.server.tag.PageBean;
import org.opencron.server.vo.JobGroupVo;
import org.opencron.server.vo.JobVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Descriptions:
 */
@Service
@Transactional
@Slf4j
public class JobGroupService {

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private JobService jobService;


    /**
     * 保存或更新记录
     * @param jobGroup
     * @return
     */
    public JobGroup merge(JobGroup jobGroup) {
        JobGroup jobLink =(JobGroup)queryDao.merge(jobGroup);
        return jobLink;
    }

    /**
     * 根据版本更新组的执行ID编号，单独创建一个新的事务
     * @param id
     * @param actionId
     * @param version
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobGroup updateGroupActionId(Long id,Long actionId,int version){
        String updateSql="UPDATE t_job_group tjg " +
                "SET tjg.actionId=?,tjg.version=tjg.version+1 " +
                "WHERE tjg.id=? AND tjg.version=?";
        int i = this.queryDao.createSQLQuery(updateSql, actionId, id, version).executeUpdate();
        log.info("update jobGroup:{} by :{} result:{}",id,actionId,i);
        JobGroup jobGroup =null;
        /*if(i<0){
            jobGroup = this.queryDao.get(JobGroup.class, id);
        }*/
        do{
            jobGroup = this.queryDao.get(JobGroup.class, id);
        }while (jobGroup.getActionId()==null);
        return jobGroup;
    }

    /**
     * 根据名称加载组
     * @param name
     * @return
     */
    public JobGroup loadGroupByName(String name) {
        String sql="SELECT * FROM t_job_group tjg " +
                "WHERE tjg.name=? limit 1";
        return this.queryDao.sqlUniqueQuery(JobGroup.class,sql,name);
    }

    public  PageBean<JobGroupVo> getJobGroupPage(PageBean pageBean) {
        String sql="SELECT tjg.*,tu.userName FROM t_job_group tjg " +
                "LEFT JOIN t_user tu " +
                "ON tu.`userId`=tjg.userId";
        pageBean = queryDao.getPageBySql(pageBean,JobGroupVo.class,sql);
        List<JobGroupVo> groups = pageBean.getResult();
        //查个每个job对应的个数
        if (CommonUtils.notEmpty(groups)) {
            sql = "SELECT COUNT(1) FROM t_job WHERE deleted=0 AND  groupId=?";
            for (JobGroupVo group : groups) {
                Long count = queryDao.getCountBySql(sql,group.getId());
                group.setJobCount(count);
            }
        }
        return pageBean;
    }

    /**
     * 根据主键查询
     * @param id
     * @return
     */
    public JobGroup getById(Long id) {
        return queryDao.get(JobGroup.class,id);
    }

    public List<JobGroup> getAll() {
        return queryDao.getAll(JobGroup.class);
    }

    public List<JobGroup> getAllOrbyGroupId(Long groupId) {
        String sql="SELECT * FROM `t_job_group` tjg " ;
        if(groupId!=null){
            sql+="WHERE tjg.`id`=?";
        }else{
            return getAll();
        }
        return queryDao.sqlQuery(JobGroup.class,sql,groupId);
    }

    /**
     * 清空每个组的执行id
     * @param actionId
     * @return
     */
    public int clearActionId(Long actionId) {
        String sql="UPDATE `t_job_group` tjg " +
                "SET tjg.`actionId`=NULL,tjg.`version`=tjg.`version`+1 " +
                "WHERE tjg.`actionId`=?";
        return queryDao.createSQLQuery(sql,actionId).executeUpdate();
    }
}
