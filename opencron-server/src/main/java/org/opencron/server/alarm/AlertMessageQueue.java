package org.opencron.server.alarm;
/**
 * @Package org.opencron.server.alarm
 * @Title: AlertMessageQueue
 * @author hapic
 * @date 2018/5/29 17:46
 * @version V1.0
 */

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Descriptions:
 */
public class AlertMessageQueue  {

    private static LinkedBlockingQueue<AlertMessage> queue = new LinkedBlockingQueue<>();

    public static void put(AlertMessage message){
        queue.add(message);
    }

    public static AlertMessage take(){
        try {
            return queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }



}
