package org.opencron.server.job;
/**
 * @Package org.opencron.server.job
 * @Title: DoPendingRunnable
 * @author hapic
 * @date 2018/4/8 15:45
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;
import org.opencron.common.utils.CommonUtils;
import org.opencron.server.domain.Record;
import org.opencron.server.service.*;
import org.opencron.server.vo.JobVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Descriptions:
 */
@Slf4j
public class DoPendingRunnable implements Runnable {

    private RecordService recordService;

    private JobService jobService;

    private AgentService agentService;

    private ExecuteService executeService;

    private ExecutorService jobExecuteServicePool;


    private ConcurrencyControl concurrencyControl;


    public DoPendingRunnable(RecordService recordService, JobService jobService, AgentService agentService, ExecuteService executeService, ExecutorService jobExecuteServicePool,ConcurrencyControl concurrencyControl) {
        this.concurrencyControl=concurrencyControl;
        this.recordService = recordService;
        this.jobService = jobService;
        this.agentService = agentService;
        this.executeService = executeService;
        this.jobExecuteServicePool=jobExecuteServicePool;
    }

    @Override
    public void run() {
        int offSet=Integer.MAX_VALUE;
        int limit=100;

        while (true){

            List<Record> records =  recordService.loadPendingRecord(offSet,limit);

            while(CommonUtils.notEmpty(records)) {
                log.info("all pending job record:{}",records.size());

                CountDownLatch countDownLatch= new CountDownLatch(records.size());
                for (final Record record : records) {
                    judgeMaxRunnint();
                    log.info("do pending job record:{} actionId:{}",record.getRecordId(),record.getActionId());

                    offSet=Math.min(record.getRecordId().intValue(),offSet);
                    log.info("next offset:{}",offSet);

                    final JobVo jobVo = jobService.getJobVoById(record.getJobId());
                    jobVo.setAgent(agentService.getAgent(jobVo.getAgentId()));

                    jobExecuteServicePool.execute(new JobExecuteService(jobVo,record,countDownLatch,executeService));
                }
                try {
                    //最多等待2分钟
                    countDownLatch.await(2, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                
                records =  recordService.loadPendingRecord(offSet,limit);
            }
            offSet=Integer.MAX_VALUE;
            records=null;
            try {
                Thread.sleep(1000*40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void judgeMaxRunnint() {
        while (concurrencyControl.getCurrent()<=0){
            try {
                log.info("runningNum:{},maxRunning:{} will sleep",(concurrencyControl.getMaxRunning()-concurrencyControl.getCurrent()),concurrencyControl.getMaxRunning());

                Thread.sleep(1000*40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            continue;
        }

    }
}
