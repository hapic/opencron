package org.opencron.server;
/**
 * @Package com.eloancn.test.servic
 * @Title: ExecuteServiceTest
 * @author hapic
 * @date 2018/4/3 16:05
 * @version V1.0
 */

import com.eloancn.test.BaseTest;
import com.sun.org.apache.regexp.internal.RE;
import org.junit.Assert;
import org.junit.Test;
import org.opencron.common.graph.KahnTopo;
import org.opencron.common.graph.Node;
import org.opencron.common.utils.CommandUtils;
import org.opencron.common.utils.CommonUtils;
import org.opencron.server.controller.JobController;
import org.opencron.server.domain.Agent;
import org.opencron.server.domain.Job;
import org.opencron.server.domain.JobDependence;
import org.opencron.server.job.ClearJobRecordRunnable;
import org.opencron.server.job.DoPendingRunnable;
import org.opencron.server.service.*;
import org.opencron.server.until.GraphUntils;
import org.opencron.server.vo.JobVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * @Descriptions:
 */
public class ExecuteServiceTest extends BaseTest{


    @Autowired
    private JobDependenceService jobDependenceService;


    @Autowired
    private ExecuteService  executeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private JobActionGroupService jobActionGroupService;


    @Autowired
    private JobController controller;

    @Autowired
    private JobGroupService jobGroupService;


    @Test
    public void testDep3(){
        Long jobId=2L;
        JobVo job = jobService.getJobVo(jobId);

        boolean b = executeService.judgeDependentJobsAllDone(job, 4667394352218858L);
        System.out.println(b);
    }

    @Test
    public void testDep2(){
        Long groupId=3L;
        Job dbJob= new Job();
        dbJob.setGroupId(groupId);
        dbJob.setJobId(50L);

        String[] dependenceid={"49","56"};
        List<JobDependence> jobDependenceList =jobDependenceService.loadDependence(dbJob.getGroupId());

        KahnTopo<Long> graph = GraphUntils.createGraph(dbJob.getJobId(),dependenceid,jobDependenceList);
        System.out.println(graph.getResult());

    }


    @Test
    public void testDep(){
        Long groupId=3L;
        List<JobDependence> jobDependenceList =jobDependenceService.loadDependence(groupId);

        KahnTopo<Long> graph = GraphUntils.createGraph(jobDependenceList);
        Iterable<Node<Long>> result = graph.getResult();
        System.out.println(result);
        System.out.println(jobDependenceList);
    }

    @Test
    public void testAcie(){
        Long jobId=47L;
        JobVo job = jobService.getJobVo(jobId);
        jobActionGroupService.acquire(job);


    }


    @Test
    public void testInsertPending(){
        Long jobId=47L;
        JobVo job = jobService.getJobVo(jobId);
        Long aLong = 7111263295183L;
//        recordService.insertPendingReocrd(aLong,job);
        try {
            Thread.sleep(10000*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


    @Test
    public void testChildJob(){

        Long id=45L;
        List<Long> longs = jobDependenceService.childsAllNodeJob(id);
        System.out.println(longs);
    }


    /**
     * 1 变更依赖关系
     * 2 添加依赖关系
     */
    @Test
    public void updateJobDep(){

        Long jobId=42L;
        Job job = jobService.getJob(jobId);
        String[] dependenceid={"45"};

        List<Job> oldDependentJob = jobDependenceService.dependentJob2(jobId);

        List<Job> newDependentJob=controller.buildDepnedentJobShip(job,dependenceid);

        List<Job> difJob= controller.differenceSet(oldDependentJob,newDependentJob);

        System.out.println(difJob);

        /**
         * 更新依赖关系
         */
        jobService.updateDependence(job,difJob);


    }



    @Test
    public void testClearRecordService(){
        ExecutorService clearRecordService = newSingleThreadExecutor();

//        clearRecordService.execute(new ClearJobRecordRunnable(jobActionGroupService,recordService,jobService,jobLinkService));


        try {
            Thread.sleep(1000*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testDoPending(){
        ExecutorService doPendingService = newSingleThreadExecutor();

//        doPendingService.execute(new DoPendingRunnable(recordService,jobService,agentService,executeService));


        try {
            Thread.sleep(1000*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void checkJobStatus(){
        Long jobId=44L;
        Job job = jobService.getJob(jobId);

        job.setPause(false);
        job.setDeleted(false);
        jobService.merge(job);
    }

    @Test
    public void insetJob(){
        Long jobId=44L;
        Job job = jobService.getJob(jobId);
        job.setJobId(null);
        job.setJobName("Node-d");
//        job.setLinkId(null);
        Job mergeJob = jobService.merge(job);
        System.out.println(mergeJob);



    }

    /**
     * 42-->43
     * 先执行42
     */
    @Test
    public void testTwoJob(){
//        Long jobId=42L;
        Long jobId=48L;
//        Long jobId=44L;
        JobVo job = jobService.getJobVoById(jobId);//找到要执行的任务
        boolean b = executeService.executeJob(job);
        Assert.assertTrue(b);


    }


    @Test
    public void testOne(){
        Long jobId=42L;
        JobVo jobVo = jobService.getJobVoById(jobId);
        if(jobVo==null){
            return;
        }



        boolean b = executeService.executeJob(jobVo,null);


    }


    @Test
    public void testchildsNodeJob(){

        List<JobVo> jobVos =
                jobDependenceService.childsNodeJob(37L);

        for(JobVo vo:jobVos){
            System.out.println(vo);
        }

    }



}
