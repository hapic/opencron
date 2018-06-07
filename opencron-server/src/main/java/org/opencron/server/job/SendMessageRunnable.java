package org.opencron.server.job;
/**
 * @Package org.opencron.server.job
 * @Title: SendMessageRunnable
 * @author hapic
 * @date 2018/5/29 20:06
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;
import org.opencron.server.alarm.AlarmNoticeFacory;
import org.opencron.server.alarm.AlarmTypes;
import org.opencron.server.alarm.AlertMessage;
import org.opencron.server.alarm.AlertMessageQueue;

/**
 * @Descriptions: 发送消息的线程
 */
@Slf4j
public class SendMessageRunnable implements Runnable {


    @Override
    public void run() {

        while (true){
            try {
                AlertMessage peek = AlertMessageQueue.take();//如若为空则一直处于堵塞状态
                if(peek!=null){
                    AlarmNoticeFacory.sendMsg(null,peek.getMessage(), AlarmTypes.DINGDING);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("error:{}",e);
                try {
                    Thread.sleep(1000*40);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }


    }
}
