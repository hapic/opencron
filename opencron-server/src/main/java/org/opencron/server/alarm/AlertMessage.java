package org.opencron.server.alarm;
/**
 * @Package org.opencron.server.alarm
 * @Title: AlertMessage
 * @author hapic
 * @date 2018/5/29 17:45
 * @version V1.0
 */

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Descriptions:
 */
@Setter
@Getter
@ToString
public class AlertMessage {

    private String uuid;
    private String message;

    public AlertMessage(String message) {
        this.message = message;
    }
}
