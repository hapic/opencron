package org.opencron.common.utils;
/**
 * @Package org.opencron.common.utils
 * @Title: ExecuteJobCache
 * @author hapic
 * @date 2018/5/14 13:45
 * @version V1.0
 */

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Descriptions:
 */
public class ExecuteJobCache {
    private static ConcurrentMap<Long,Long> handlerJobRecord= new ConcurrentHashMap<>();
    private static long gap=5*60*100L;//间歇为5分钟

    public static boolean addRecord(Long jobId){
        Long currentTime = DateUtils.getCurrentTime();
        clearOverdue(currentTime);
        if(handlerJobRecord.containsKey(jobId)){
            return false;
        }else {
            handlerJobRecord.putIfAbsent(jobId,currentTime);
            return true;
        }
    }

    public static boolean checkJobId(Long jobId){
        Long currentTime = DateUtils.getCurrentTime();
        clearOverdue(currentTime);
        if(handlerJobRecord.containsKey(jobId)){
            return false;
        }
        return true;
    }

    private static void clearOverdue(Long currentTime){
        Iterator<Map.Entry<Long, Long>> iterator =
                handlerJobRecord.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Long, Long> next = iterator.next();
            Long value = next.getValue();
            if(value!=null && currentTime-value>gap){//证明已经过期了，则清理掉
                handlerJobRecord.remove(next.getKey(),value);
            }
        }
    }
}
