package org.opencron.common.utils;
/**
 * @Package org.opencron.common.utils
 * @Title: ParamUntils
 * @author hapic
 * @date 2018/4/17 15:02
 * @version V1.0
 */

import java.util.Date;

/**
 * @Descriptions: 参数替替换
 */
public class ParamUntils {

    private static String P_DATE="$date";
    private static String P_TIME="$time";


    public static String command(String command){
        String[] split = command.split(" ");
        StringBuffer sb= new StringBuffer(split[0]);
        for(String cmd:split){
            if(cmd.startsWith("$")){
               String value= replace(cmd);
                sb.append(" "+value);
            }
        }
        return sb.toString();
    }

    /**
     * 返回年月日时分秒
     * @param cmd
     * @return
     */
    private static String replace(String cmd) {
        if(cmd.equalsIgnoreCase(P_TIME)){
            return DateUtils.formatFullDate2(new Date());
        }else if(cmd.equalsIgnoreCase(P_DATE)){
            return DateUtils.formatDayDate(new Date());
        }
        return "";
    }

    public static String command(String command,String...values){
        String[] split = command.split(" ");
        StringBuffer sb= new StringBuffer(split[0]);
        for(String value:values){
            sb.append(" "+value);
        }
        return sb.toString();
    }

    /**
     * 根据一个命令获取参数
     * @param command
     * @return
     */
    public static String param(String command){
        if(command.indexOf(" ")==-1){
            return command;
        }
        StringBuffer sb= new StringBuffer(command);
        return sb.substring(sb.indexOf(" ")).trim();
    }

    public static void main(String[] args) {
        String parma = param("/ab/c/c.sh 18273 232");
        System.out.println(parma);
    }




}
