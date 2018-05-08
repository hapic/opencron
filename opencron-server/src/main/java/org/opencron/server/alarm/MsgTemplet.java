package org.opencron.server.alarm;
/**
 * @Package org.opencron.server.alarm
 * @Title: MsgTemplet
 * @author hapic
 * @date 2018/5/8 13:39
 * @version V1.0
 */

/**
 * @Descriptions:
 */
public class MsgTemplet {

    static String timeout_unfinished="【%s】在【%s】开始运行,现已超时,请及时处理!";


    public static String getTimeOutUnfinishedMsg(String jobName,String startDate){
        return formart(jobName,startDate);
    }

    static String formart(String ...value){
        return String.format(timeout_unfinished,value);
    }
}
