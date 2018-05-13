package org.opencron.server.job;
/**
 * @Package org.opencron.server.job
 * @Title: JobExecuteService
 * @author hapic
 * @date 2018/4/17 13:31
 * @version V1.0
 */

import org.opencron.server.domain.Record;
import org.opencron.server.service.ExecuteService;
import org.opencron.server.vo.JobVo;

import java.util.concurrent.CountDownLatch;

/**
 * @Descriptions:
 */
public class JobExecuteService implements Runnable {

    private ExecuteService executeService;

    private JobVo jobVo;
    private Record record;


    public JobExecuteService(JobVo jobVo, Record record, ExecuteService executeService) {
        this.jobVo=jobVo;
        this.record=record;
        this.executeService=executeService;
    }


    @Override
    public void run() {
        executeService.executeJob(jobVo, record.getActionId(),true);

    }
}
