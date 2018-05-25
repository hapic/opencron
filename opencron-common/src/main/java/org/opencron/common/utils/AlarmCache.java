package org.opencron.common.utils;
/**
 * @Package org.opencron.common.utils
 * @Title: AlarmCache
 * @author hapic
 * @date 2018/5/25 10:30
 * @version V1.0
 */

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Descriptions: 告警通知的缓存
 */
public class AlarmCache {

    private static ConcurrentMap<Long,Long> alarmCache= new ConcurrentHashMap<>();

    /**
     * 判断是否为空
     * @return
     */
    public static boolean isEmpty(){
        return alarmCache.isEmpty();
    }

    public static boolean containsKey(Long key){
        return alarmCache.containsKey(key);
    }

    public static void push(Long key,Long value){
        alarmCache.put(key,value);
    }

    public static Long get(Long key){
        return alarmCache.get(key);
    }
    public static void clear(){
        alarmCache.clear();
    }




}
