package org.opencron.server.job;
/**
 * @Package org.opencron.server.job
 * @Title: ClearJobRecordRunnable
 * @author hapic
 * @date 2018/4/8 15:53
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;
import org.opencron.common.job.Opencron;
import org.opencron.common.utils.CommonUtils;
import org.opencron.server.domain.Job;
import org.opencron.server.domain.JobActionGroup;
import org.opencron.server.domain.Record;
import org.opencron.server.service.JobActionGroupService;
import org.opencron.server.service.JobGroupService;
import org.opencron.server.service.JobService;
import org.opencron.server.service.RecordService;
import org.opencron.server.until.CommonLock;

import java.util.List;

/**
 * @Descriptions:
 */
@Slf4j
public class ClearJobRecordRunnable implements Runnable {



    private JobActionGroupService jobActionGroupService;


    private RecordService recordService;

    private JobService jobService;

    public ClearJobRecordRunnable(JobActionGroupService jobActionGroupService, RecordService recordService, JobService jobService) {
        this.jobActionGroupService = jobActionGroupService;
        this.recordService = recordService;
        this.jobService = jobService;
    }

    @Override
    public void run() {
        int offSet=Integer.MAX_VALUE;
        int limit=1000;

        while (true){
            List<JobActionGroup> jobActionGroups = jobActionGroupService.loadRunningGroup(offSet,limit);

            while(CommonUtils.notEmpty(jobActionGroups)) {
                for(JobActionGroup actionGroup:jobActionGroups){
                    //遍历每一个，查看每个组的任务是否都已经做完了
                    Long groupId = actionGroup.getGroupId();
                    Long actionId = actionGroup.getActionId();

                    offSet=Math.min(actionGroup.getId().intValue(),offSet);


                    List<Job> jobList=this.jobService.loadUsedJobByGroupId(groupId);

                    boolean allDone=true;
                    for(Job job:jobList){
                        Long jobId = job.getJobId();
                        int i = this.recordService.loadSuccessRecordCount(jobId, actionId);
                        log.info(" groupId:{} jobId:{} actionId:{} success count:{}",groupId,jobId,actionId,i);
                        if(i<1){
                            allDone=false;
                            break;
                        }
                    }
                    if(allDone){
                        log.info(" groupId:{} actionId:{} all done",groupId,actionId);
                        //完成组的状态
                        jobActionGroupService.finishActionGroup(actionGroup.getId(),Opencron.RunStatus.DONE,Opencron.ExecType.AUTO);
                        CommonLock.reomve(actionId+"");
                        log.info("remove lock {} ",actionId);
                    }


                }
                jobActionGroups = jobActionGroupService.loadRunningGroup(offSet,limit);
            }
            jobActionGroups=null;
            offSet=Integer.MAX_VALUE;
            sleep();
        }
    }


    public void sleep(){
        try {
            Thread.sleep(1000*40);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
