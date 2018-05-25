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
    static String group_finished_alarm="任务组【%s】已全部成功完成^_^";


    public static String getTimeOutUnfinishedMsg(String jobName,String startDate){
        return formart(timeout_unfinished,jobName,startDate);
    }
    public static String getGroupFinishedMsg(String groupName){
        return formart(group_finished_alarm,groupName);
    }

    static String formart(String templet,String ...value){
        return String.format(templet,value);
    }
}
