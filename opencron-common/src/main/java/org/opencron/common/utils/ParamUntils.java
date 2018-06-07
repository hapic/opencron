package org.opencron.common.utils;
/**
 * @Package org.opencron.common.utils
 * @Title: ParamUntils
 * @author hapic
 * @date 2018/4/17 15:02
 * @version V1.0
 */

import java.math.BigInteger;
import java.util.Date;

/**
 * @Descriptions: 参数替替换
 */
public class ParamUntils {

    private static String P_DATE="$date";
    private static String P_TIME="$time";


    public static String command(String command,Date date){
        int index = command.indexOf("$");
        if(index<0){
            return command;
        }

        String cmd = command.substring(0, index).trim();
        StringBuffer sb= new StringBuffer(cmd);
        String param = command.substring(index );
        if(StringUtils.isNullString(param)){
            param=param.trim();
        }
        String[] split = param.split(" ");
        for(int i=0;i<split.length;i++){
            if(split[i].startsWith("$")){
                String value= replace(split[i],date);
                sb.append(" "+value);
            }else if(i==0){
                sb.append(split[i]);
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
            return DateUtils.formatFullDate(date);
        }else if(cmd.equalsIgnoreCase(P_DATE)){
            return DateUtils.formatSimpleDate(date);
        }
        return "";
    }
    private static String replace(String cmd) {
        return replace(cmd,new Date());
    }

    public static String command(String command,String value){
        int index$ = command.indexOf("$");
        String substring=null;
        if(index$>-1){
            substring = command.substring(0,index$);
        }else {
            substring=command;
        }
        StringBuffer sb= new StringBuffer(substring.trim());
        sb.append(" "+value);
        return sb.toString();
    }

    /**
     * 根据一个命令获取参数
     * @param command
     * @return
     */
    public static String param(String command){
        if(command.indexOf(" ")==-1){
            return null;
        }
        StringBuffer sb= new StringBuffer(command);
        return sb.substring(sb.indexOf(" ")).trim();
    }

    public static void main(String[] args) {

        String cmd="date;sleep 10; $date $time";
        String command = ParamUntils.command(cmd,"2018-08-3 2018-08-3");
//        String parma = param("/ab/c/c.sh 18273 232");
        System.out.println(command);


    }




}
