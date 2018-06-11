package org.opencron.server.until;
/**
 * @Package com.jobxhub.server.common
 * @Title: Parser
 * @author hitechr
 * @date 2018/6/8 21:02
 * @version V1.0
 */

import org.apache.commons.lang3.StringUtils;
import org.opencron.common.utils.DateUtils;
import org.opencron.server.domain.Job;
import org.opencron.server.vo.JobVo;

import java.util.Date;

/**
 * @Descriptions: 参数替换工具类
 * $date: 当期日期 2017-08-01
 * $time: 当前时间 20180609210423
 * $user: 作业的用户
 * $jobId: 作业的ID
 * $pid: 作业记录对应的pid
 * $name: 作业的名称
 *
 */
public class Parser {

    public  final static String MARK="$";

    /**
     * a.sh 1 2 $date 3 $name 5 $jobId $pid
     * inputParam=2017-3-4 Lily 21 1111111
     * @param job
     * @return
     */
    public static String parse(JobVo job){
        String inputParam = job.getParam();
        //按默认替换
        if(StringUtils.isBlank(inputParam)){//如果没有传递参数，则默认取值
            String cmdValue=parseDefault(job);
            return cmdValue.trim();
        }
        //append
        String command = job.getCommand();
        int f$ = command.indexOf(MARK);
        if(f$<0){// a.sh 1 2 3
            return command.trim()+" "+inputParam.trim();
        }
        return parse2(job);
        // a.sh 1 2 $date 3 $name 5 $jobId $pid
    }

    /**
     * a.sh 1 2 $date 3 $name 5 $jobId $pid
     *  inputParam=2017-3-4 Lily 21 1111111
     * 按传递的参数替换
     * @param job
     */
    private static String parse2(JobVo job) {
        String inputParam = job.getParam();
        String command = job.getCommand();
        String[] params = inputParam.split(" ");
        String[] values = command.split(" ");
        StringBuffer sb= new StringBuffer();
        int length = params.length;
        int $i=0;
        for(String v:values){
            if(v.startsWith(MARK)){
                sb.append(" "+($i<length?params[$i]:v));
                $i++;
            }else{
                sb.append(" "+v);
            }
        }
        for(;$i<params.length;$i++){
            sb.append(" "+params[$i]);
        }
        return replaceSign(sb.toString(),job);
    }

    /**
     * 按默认值替换
     * @param job
     * @return
     */
    private static String parseDefault(JobVo job) {
        String command = job.getCommand();
        return replaceSign(command,job);
    }

    /**
     * 用job中的值替换command中的变量
     * @param command
     * @param job
     * @return
     */
    private static String replaceSign(String command,JobVo job) {
        int f$ = command.indexOf(MARK);
        if(f$<0){
            return command;
        }
        String subCmd = command.substring(0, f$);

        StringBuffer cmd=new StringBuffer(subCmd);

        String[] params = command.substring(f$).split(" ");
        for(String param:params){
            if(param.startsWith(MARK)){
                cmd.append(" "+replace(param,job).trim());
            }else{
                cmd.append(" "+param.trim());
            }
        }
        return cmd.toString();
    }

    /**
     * * $date: 当期日期 2017-08-01
     *  * $time: 当前时间 20180609210423
     *  * $user: 作业的用户
     *  * $jobId: 作业的ID
     *  * $pid: 作业记录对应的pid
     *  * $name: 作业的名称
     *
     * @param param
     * @param job
     * @return
     */
    private static String replace(String param, JobVo job) {
        switch (param){
            case  "$date":case "$date;":
                return DateUtils.formatSimpleDate(new Date());
            case "$time":case "$time;":
                return DateUtils.formatSimpleDate(new Date());
            case "$user":case "$user;":
                return job.getUserId()+"";
            case "$jobId":case "$jobId;":
                return job.getJobId()+"";
            case "$name":case "$name;":
                return job.getJobName();
                default:
                    return "";
        }
    }

    public static void main(String[] args) {
        JobVo job=new JobVo();
        job.setCommand("echo 1 ");
        job.setJobName("test");
        job.setJobId(1212L);
        job.setUserId(2222L);
        job.setParam("2018-3-4");
        String parse = parse(job);
        System.out.println(parse);
    }
}
