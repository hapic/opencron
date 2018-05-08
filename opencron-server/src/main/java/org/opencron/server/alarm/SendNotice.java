package org.opencron.server.alarm;
/**
 * @Package org.opencron.server.alarm
 * @Title: SendNotice
 * @author hapic
 * @date 2018/5/8 11:26
 * @version V1.0
 */

/**
 * @Descriptions: 发送消息
 */

public interface SendNotice {

    boolean send(String accept,String msg);

}
