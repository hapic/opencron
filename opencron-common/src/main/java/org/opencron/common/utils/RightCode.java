package org.opencron.common.utils;
/**
 * @Package org.opencron.common.utils
 * @Title: RightCode
 * @author hapic
 * @date 2018/5/29 17:17
 * @version V1.0
 */

import java.util.Arrays;
import java.util.Collections;

/**
 * @Descriptions: 权限码的验证
 */
public class RightCode {


    public enum  AlarmCode{
        FAIL(1),TIMEOUT(2),SUCCESS(4),ERROR(5);
        int code;

        AlarmCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     *
     * @param code 授权码
     * @param num 权限
     * @return
     */
    public static boolean auth(int code,int num){
        int re = code & num;
        return re==num;
    }

    public static int code(Integer[] nums){
        if(nums==null || nums.length<1){
            return 0;
        }
        int code=0;
        for(Integer num:nums){
            code=code|num;
        }
        return code;
    }

    public static void main(String[] args) {
        Integer[] codes={1,4};
        System.out.println(code(codes));

    }
}
