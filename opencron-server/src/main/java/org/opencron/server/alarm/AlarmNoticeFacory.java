package org.opencron.server.alarm;
/**
 * @Package org.opencron.server.alarm
 * @Title: AlarmNoticeFacory
 * @author hapic
 * @date 2018/5/8 13:59
 * @version V1.0
 */

import java.util.Map;

/**
 * @Descriptions:
 */
public class AlarmNoticeFacory {


    static Map<Object,SendNotice> sendNoticeMap;

    public static SendNotice getInstantce(AlarmTypes alarmType ){
        return sendNoticeMap.get(alarmType.name());
    }

    public void setSendNoticeMap(Map<Object, SendNotice> sendNoticeMap) {
        AlarmNoticeFacory.sendNoticeMap = sendNoticeMap;
    }


    public static void sendMsg(String accept, String msg, AlarmTypes ... alarmTypes) {
        for(AlarmTypes at:alarmTypes){
            SendNotice instantce = AlarmNoticeFacory.getInstantce(at);
            instantce.send(accept,msg);
        }
    }
}
