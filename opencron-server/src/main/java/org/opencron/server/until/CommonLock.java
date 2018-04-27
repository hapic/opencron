package org.opencron.server.until;
/**
 * @Package org.opencron.server.until
 * @Title: CommonLock
 * @author hapic
 * @date 2018/4/16 20:26
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Descriptions:
 */
@Slf4j
public class CommonLock {
    private volatile   static ConcurrentHashMap<String,Lock> hashMap= new ConcurrentHashMap();

    public static Lock acquireLock(String key){
        log.info("try lock {}",key);
        if(hashMap.containsKey(key)){
            return hashMap.get(key);
        }else{
            Lock lock= new ReentrantLock();
            hashMap.putIfAbsent(key,lock);
            return hashMap.get(key);
        }
    }

    public static void reomve(String key){
        hashMap.remove(key);
    }
}
