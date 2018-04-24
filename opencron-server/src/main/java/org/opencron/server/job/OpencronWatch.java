package org.opencron.server.job;
/**
 * @Package org.opencron.server.job
 * @Title: OpencronWatch
 * @author hapic
 * @date 2018/4/8 15:34
 * @version V1.0
 */

import org.opencron.server.service.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.*;

/**
 * @Descriptions:
 */
@Component
public class OpencronWatch implements InitializingBean {

    //处理pending状态的记录的线程池
    ExecutorService doPendingService;


    //清理执行记录的线程池
    ExecutorService clearRecordService;


    ExecutorService jobExecuteServicePool;


    @Autowired
    private RecordService recordService;
    @Autowired
    private JobService jobService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private ExecuteService executeService;


    @Autowired
    private JobActionGroupService jobActionGroupService;

    @Autowired
    private ConcurrencyControl concurrencyControl;



    @Override
    public void afterPropertiesSet() throws Exception {

        doPendingService= newSingleThreadExecutor();

        clearRecordService= newSingleThreadExecutor();

        jobExecuteServicePool= newFixedThreadPool(50);

        doPendingService.execute(new DoPendingRunnable(recordService,jobService,agentService,executeService,jobExecuteServicePool,concurrencyControl));

        clearRecordService.execute(new ClearJobRecordRunnable(jobActionGroupService,recordService,jobService));

    }
}
