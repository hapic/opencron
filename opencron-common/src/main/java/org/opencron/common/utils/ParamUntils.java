package org.opencron.common.utils;
/**
 * @Package org.opencron.common.utils
 * @Title: ParamUntils
 * @author hapic
 * @date 2018/4/17 15:02
 * @version V1.0
 */

/**
 * @Descriptions: 参数替替换
 */
public class ParamUntils {

    String command;
    enum Param{
        DATE,TIME,USERID;
        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public String command(String command,Param param,String value){
        return command.toLowerCase().replaceAll("$"+param.getName().toLowerCase()+"$",value);
    }




}
