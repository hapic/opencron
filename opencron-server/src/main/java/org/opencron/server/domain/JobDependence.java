package org.opencron.server.domain;
/**
 * @Package org.opencron.server.domain
 * @Title: DependentJob
 * @author hapic
 * @date 2018/4/2 15:14
 * @version V1.0
 */

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * @Descriptions: job依赖关系表
 */
@Entity
@Table(name = "t_job_dependence")
public class JobDependence implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    private Long jobId;
    private Long dependenceJobId;//前置依赖JobId
    private Long groupId;
    private int status;//状态
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getDependenceJobId() {
        return dependenceJobId;
    }

    public void setDependenceJobId(Long dependenceJobId) {
        this.dependenceJobId = dependenceJobId;
    }


    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }


    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
