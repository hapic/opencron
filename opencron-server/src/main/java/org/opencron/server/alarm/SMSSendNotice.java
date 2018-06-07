package org.opencron.server.alarm;
/**
 * @Package org.opencron.server.alarm
 * @Title: DDSendNotice
 * @author hapic
 * @date 2018/5/8 11:28
 * @version V1.0
 */

/**
 * @Descriptions:
 */
//@Component("smsSendNotice")
public class SMSSendNotice implements SendNotice {


    @Override
    public boolean send(String accept, String msg) {

        return false;
    }
}
