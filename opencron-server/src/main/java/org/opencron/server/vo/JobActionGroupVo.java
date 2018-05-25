package org.opencron.server.vo;
/**
 * @Package org.opencron.server.vo
 * @Title: JobActionGroupVo
 * @author hapic
 * @date 2018/5/25 11:05
 * @version V1.0
 */

import org.opencron.server.domain.JobActionGroup;

/**
 * @Descriptions:
 */
public class JobActionGroupVo extends JobActionGroup {

    private String groupName;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
