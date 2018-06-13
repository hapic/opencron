package org.opencron.server.alarm;
/**
 * @Package org.opencron.server.alarm
 * @Title: AlarmNoticeFacory
 * @author hapic
 * @date 2018/5/8 13:59
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;
import org.opencron.common.utils.RightCode;

import java.util.Map;

/**
 * @Descriptions:
 */
@Slf4j
public class AlarmNoticeFacory {


    static Map<Object,SendNotice> sendNoticeMap;

    public static SendNotice getInstantce(AlarmTypes alarmType ){
        return sendNoticeMap.get(alarmType.name());
    }

    public void setSendNoticeMap(Map<Object, SendNotice> sendNoticeMap) {
        AlarmNoticeFacory.sendNoticeMap = sendNoticeMap;
    }


    public static void sendMsg(String accept, String msg, AlarmTypes ... alarmTypes) {
        if(alarmTypes==null || alarmTypes.length<1){
            return;
        }
        for(AlarmTypes at:alarmTypes){
            SendNotice instantce = AlarmNoticeFacory.getInstantce(at);
            if(instantce!=null){
                log.info("send message:"+msg);
                instantce.send(accept,msg);
            }
        }
    }


    public static void putMessage(Integer code, RightCode.AlarmCode alarmCode,String ... info) {
        if(!RightCode.auth(code,alarmCode.getCode())){
            return;
        }
        AlertMessage alertMessage=null;
        switch (alarmCode){
            case FAIL:
                String jobFailAlarm = MsgTemplet.getJobFailAlarm(info[0]);
                alertMessage= new AlertMessage(jobFailAlarm);
                break;
            case SUCCESS:

                String jobSuccessAlarm = MsgTemplet.getJobSuccessAlarm(info[0]);
                alertMessage= new AlertMessage(jobSuccessAlarm);
                break;
            case TIMEOUT:
                String jobTimeoutAlarm = MsgTemplet.getJobTimeoutAlarm(info[0]);
                alertMessage= new AlertMessage(jobTimeoutAlarm);
                break;
            case ERROR:
                String jobErrorAlarm = MsgTemplet.getJobErrorAlarm(info);
                alertMessage= new AlertMessage(jobErrorAlarm);
        }
        log.info("put message:"+alertMessage);
        AlertMessageQueue.put(alertMessage);

    }
}
