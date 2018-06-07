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
    static String group_finished_alarm="任务组【%s】已全部成功完成!";
    static String job_timeout_alarm="任务【%s】已超时，请及时处理!";
    static String job_fail_alarm="任务【%s】运行时出错，请及时处理!";
    static String job_success_alarm="任务【%s】成功运行完成!";
    static String job_error_alarm="任务【%s】记录ID:【%s】出错异常情况:%s!";


    public static String getJobErrorAlarm(String ... info){
        return formart(job_error_alarm,info);
    }
    public static String getTimeOutUnfinishedMsg(String jobName,String startDate){
        return formart(timeout_unfinished,jobName,startDate);
    }
    public static String getGroupFinishedMsg(String groupName){
        return formart(group_finished_alarm,groupName);
    }

    public static String getJobTimeoutAlarm(String jobName){
        return formart(job_timeout_alarm,jobName);
    }
    public static String getJobSuccessAlarm(String jobName){
        return formart(job_success_alarm,jobName);
    }

    public static String getJobFailAlarm(String jobName){
        return formart(job_fail_alarm,jobName);
    }

    static String formart(String templet,String ...value){
        return String.format(templet,value);
    }
}
