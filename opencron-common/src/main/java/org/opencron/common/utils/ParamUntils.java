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


    public static String command(String command,Date date){
        String[] split = command.split(" ");
        StringBuffer sb= new StringBuffer();
        for(String cmd:split){
            if(cmd.startsWith("$")){
               String value= replace(cmd,date);
                sb.append(" "+value);
            }else{
                sb.append(" "+cmd);
            }
        }
        return sb.toString();
    }

    public static String command(String command){
        return command(command,new Date());
    }

    /**
     * 返回年月日时分秒
     * @param cmd
     * @return
     */
    private static String replace(String cmd,Date date) {
        if(cmd.equalsIgnoreCase(P_TIME)){
            return DateUtils.formatFullDate2(date);
        }else if(cmd.equalsIgnoreCase(P_DATE)){
            return DateUtils.formatSimpleDate(date);
        }
        return "";
    }
    private static String replace(String cmd) {
        return replace(cmd,new Date());
    }

    public static String command(String command,String...values){
        int index$ = command.indexOf("$");
        String substring=null;
        if(index$>-1){
            substring = command.substring(0,index$);
        }else {
            substring=command;
        }
        StringBuffer sb= new StringBuffer(substring);
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

        String cmd="date;sleep 30s;";
        String command = ParamUntils.command(cmd,"2018-03-12");
//        String parma = param("/ab/c/c.sh 18273 232");
        System.out.println(command);
    }




}
