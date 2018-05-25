package org.opencron.server.domain;
/**
 * @Package org.opencron.server.domain
 * @Title: JobActionGroup
 * @author hapic
 * @date 2018/4/4 13:52
 * @version V1.0
 */

import javax.persistence.*;
import java.util.Date;

/**
 * @Descriptions: 小组任务记录状态
 */
@Entity
@Table(name = "t_job_action_group")
public class JobActionGroup {
    @Id
    @GeneratedValue
    private Long id;

    private Long groupId;//组ID
    private Long actionId;

    private Integer status;//当前小组任务在状态
    private Integer endType;//结束方式

    private Date startTime;//当前小组在开始时间
    private Date endateTime;//当前小组在结束时间

    private Integer alarm=0;//是否已经通知


    public Integer getAlarm() {
        return alarm;
    }

    public void setAlarm(Integer alarm) {
        this.alarm = alarm;
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    public Integer getEndType() {
        return endType;
    }

    public void setEndType(Integer endType) {
        this.endType = endType;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndateTime() {
        return endateTime;
    }

    public void setEndateTime(Date endateTime) {
        this.endateTime = endateTime;
    }
}
