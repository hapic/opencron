package org.opencron.server.service;
/**
 * @Package org.opencron.server.service
 * @Title: ConcurrencyControl
 * @author hapic
 * @date 2018/4/4 10:03
 * @version V1.0
 */

import org.opencron.server.domain.Config;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Descriptions: 并发控制器
 */
@Service
public class ConcurrencyControl {
    /**
     * 最大并发数
     */
    private volatile Integer maxRunning=30;

    private volatile AtomicInteger atomicInteger=new AtomicInteger(maxRunning);



    /**
     * 更新最大并发数
     * @param newNum
     * @return
     */
    public int updateConcurrencyNum(Integer newNum){
        int i = atomicInteger.addAndGet(newNum - maxRunning);

        maxRunning=newNum;
        if(i<0){//如果更新后的值小于0则表示为往下降
            while (true){
                int i1 = atomicInteger.incrementAndGet();
                if(i1>maxRunning){
                    atomicInteger.decrementAndGet();
                    return maxRunning;
                }
            }
        }
        return maxRunning;
    }


    public int refresh(Integer newNum){
        atomicInteger.set(newNum);
        return maxRunning;
    }


    /**
     * 是否允许继续执行
     * @return
     */
    public boolean isContinue(int runningCount){

        /**
         * 如果已经达到上限
         */
        if(runningCount>=maxRunning){
            return false;
        }

        long l = atomicInteger.decrementAndGet();
        if(l<0){//如果结果>最后的值，则说明已经达到上限
            atomicInteger.incrementAndGet();
            return false;
        }
        return true;
    }

    public Integer getMaxRunning() {
        return maxRunning;
    }

    public Integer getCurrent(){
        return atomicInteger.get();
    }


    public int out() {
        int i = atomicInteger.incrementAndGet();
        if(i>maxRunning){
            return atomicInteger.decrementAndGet();
        }
        return i;
    }

    public void initAtomic(ConfigService configService) {
        Config sysConfig = configService.getSysConfig();
        this.maxRunning = sysConfig.getMaxRunning();

        atomicInteger=new AtomicInteger(this.maxRunning);
    }
}
