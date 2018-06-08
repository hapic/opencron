/**
 * Copyright 2016 benjobs
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.opencron.server.service;

import com.mysql.jdbc.PacketTooBigException;
import com.sun.org.apache.regexp.internal.RE;
import org.opencron.common.exception.InvalidException;
import org.opencron.common.exception.PingException;
import org.opencron.common.exception.UnknownException;
import org.opencron.common.job.Action;
import org.opencron.common.job.Opencron;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.utils.*;
import org.opencron.server.DBException;
import org.opencron.server.alarm.AlarmNoticeFacory;
import org.opencron.server.alarm.AlertMessage;
import org.opencron.server.alarm.AlertMessageQueue;
import org.opencron.server.alarm.MsgTemplet;
import org.opencron.server.domain.*;
import org.opencron.server.job.OpencronCaller;
import org.opencron.server.job.OpencronMonitor;
import org.opencron.server.until.CommonLock;
import org.opencron.server.vo.JobVo;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import static org.opencron.common.job.Opencron.*;

@Service
public class ExecuteService implements Job {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RecordService recordService;

    @Autowired
    private JobService jobService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private OpencronCaller opencronCaller;

    @Autowired
    private AgentService agentService;

    @Autowired
    private UserService userService;

    @Autowired
    private JobDependenceService jobDependenceService;

    @Autowired
    private JobGroupService jobGroupService;

    @Autowired
    private ConcurrencyControl concurrencyControl;

    @Autowired
    private JobActionGroupService jobActionGroupService;


    private Map<Long, Integer> reExecuteThreadMap = new HashMap<Long, Integer>(0);

    private static final String PACKETTOOBIG_ERROR = "在向MySQL数据库插入数据量过多,需要设定max_allowed_packet";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String key = jobExecutionContext.getJobDetail().getKey().getName();
        JobVo jobVo = (JobVo) jobExecutionContext.getJobDetail().getJobDataMap().get(key);
        try {
            ExecuteService executeService = (ExecuteService) jobExecutionContext.getJobDetail().getJobDataMap().get("jobBean");
            boolean success = executeService.executeJob(jobVo,true);
            this.loggerInfo("[opencron] job:{} at {}:{},execute:{}", jobVo, success ? "successful" : "failed");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * 基本方式执行任务，按任务类型区分
     */
    public boolean executeJob(final JobVo job,boolean initChildJob) {
        Long actionId= job.getActionId();

        try {
            if(job.getFlowNum()==0){//如果是根节点则还没有对应的actionID,需要申请个actionId
                //根据Job 获取当前对应的GroupId
                actionId = job.getActionId();
                if(actionId==null){
                    actionId = jobActionGroupService.acquire(job);
                    logger.info("load actionId:{} by job:{}",actionId,job.getJobName());
                    job.setActionId(actionId);
                }


                if(!job.getExecType().equals(Opencron.ExecType.OPERATOR.getStatus())){//如果是手动重跑模式
                    //同一时间防止多个节点同时触发的情况
                    Lock lock = CommonLock.acquireLock("action:"+actionId.toString());
                    lock.lock();
                    logger.info("save root action group by jobId:{}",job.getJobName());
                    this.jobService.initRootPendingJob(job,actionId);
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            logger.error(" error:{}",e.getMessage());
        }


        return executeJob(job,actionId,initChildJob);
    }

    private void setActionGroupToJob(JobVo job, Long actionId) {
        if(job.getActionGroup()==null){
            JobActionGroup actionGroup = this.jobActionGroupService.loadActionGroupByActionId(actionId);
            if(actionGroup==null){
                throw new UnknownException("not found "+actionId+" action group!");
            }
            job.setActionGroup(actionGroup);
        }
    }

    public void handleExecuteJob(JobVo job,boolean isNew) {
        Long actionId=null;

        //保存当前执行的参数
        JobActionGroup actionGroup = this.jobActionGroupService.loadActionGroupByActionId(job.getActionId());
        actionGroup.setParam(job.getParam());
        this.jobActionGroupService.merge(actionGroup);

        job.setActionGroup(actionGroup);
        if(job.getRecordId()!=null){
            Record record = this.recordService.get(job.getRecordId());
            actionId=record.getActionId();
            logger.info("load job:{} old record:{} status:{} actionId:{}",job.getJobName(),record.getRecordId(),record.getStatus(),actionId);
            if(!isNew){
                this.recordService.updateOldRecorAndInsertNewRecord(record, job);//修改老的记录，插入新的记录
            }
            this.executeJob(job,true);
        }
    }

    /**
     * 单一任务执行过程
     */
    private boolean executeSingleJob(JobVo job, Long userId) {

        if (!checkJobPermission(job.getAgentId(), userId)) return false;

        Record record = new Record(job);
        record.setJobType(JobType.SINGLETON.getCode());//单一任务
        try {
            //执行前先保存
            record = recordService.merge(record);
            //执行前先检测一次通信是否正常
            checkPing(job, record);
            Response response = responseToRecord(job, record);
            recordService.merge(record);
            if (!response.isSuccess()) {
                //当前的单一任务只运行一次未设置重跑.
                if (job.getRedo() == 0 || job.getRunCount() == 0) {
                    noticeService.notice(job, null);
                }
                this.loggerInfo("execute failed:jobName:{} at ip:{},port:{},info:{}", job, record.getMessage());
                return false;
            } else {
                this.loggerInfo("execute successful:jobName:{} at ip:{},port:{}", job, null);
            }
        } catch (PacketTooBigException e) {
            noticeService.notice(job, PACKETTOOBIG_ERROR);
            this.loggerError("execute failed:jobName:%s at ip:%s,port:%d,info:%s", job, PACKETTOOBIG_ERROR, e);
        } catch (Exception e) {
            if (job.getRedo() == 0 || job.getRunCount() == 0) {
                noticeService.notice(job, null);
            }
            this.loggerError("execute failed:jobName:%s at ip:%s,port:%d,info:%s", job, e.getMessage(), e);
        }
        return record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus());
    }


    /**
     * 每次执行一个Job
     * @param job
     * @param actionId
     * @return
     */
    public boolean executeJob(JobVo job,Long actionId,boolean initChildJob) {

        setActionGroupToJob(job,actionId);

//        if (!checkJobPermission(job.getAgentId(), job.getUserId())) return false;
        logger.info("start job:{} actionId:{}",job.getJobName(),actionId);

        //验证当前组的任务是否已经做过
        boolean haveDone=checkCurrentJobHaveDone(job,actionId);
        if(haveDone){
            logger.info("current Job:{} haved done or running",job.getJobName());
            return false;
        }
        //判断所有的任务是否都做了
//        if(!job.getExecType().equals(ExecType.OPERATOR.getStatus())){
        logger.info(" job:{} execType:{} ",job.getJobName(),job.getExecType());
        boolean allDone=judgeDependentJobsAllDone(job,actionId);
        if(!allDone){
            logger.info("job:{} dependent job not done!");
            return false;
        }
        if(job.getFlowNum()>0 || job.getLastChild()){
            //清理相关的记录
            this.clearGroupActionId(job.getGroupId(),actionId);
        }


//        }


        //判断Running中的任务数是否达到上限，如果已达到上限则记录当前任务的状态为pending稍后触发
        //查询当前处于Runningd状态的记录
        boolean b = false;
        boolean aContinue=true;
        try {
            int runningCount=this.recordService.runningRecordCount();
            logger.info("actionId:{} currintRunning count is:{}",actionId,runningCount);
            aContinue= concurrencyControl.isContinue(runningCount);
            if(!aContinue){
                Record record = this.recordService.insertPendingReocrd(actionId, job, RunStatus.PENDING);
                if(record.getStatus().equals(RunStatus.QUARTATRI.getStatus())){//如果已经保存过
                    record.setStatus(RunStatus.PENDING.getStatus());
                    logger.info("update record:{} to pending",record.getRecordId());
                    this.recordService.merge(record);
                }
                logger.info("actionId:{} job:{} reach the line {} currentRunnint:{},save Redord:{}",actionId, job.getJobName(),concurrencyControl.getMaxRunning(),concurrencyControl.getCurrent(),record.getRecordId());
                return false;
            }
            //真正的执行Job的
            b = doThisJob(job, actionId,initChildJob);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(aContinue){
                concurrencyControl.out();
            }
        }
        return b;
    }

    private void clearGroupActionId(Long groupId, Long actionId) {

        Long rootJob=this.jobService.loadRootJobByGroupIdCount(groupId);
        Long doneCount=this.recordService.loadRootDoneCount(actionId);
        logger.info("groupId:{} root count:{},actionId:{} done Count:{}",groupId,rootJob,actionId,doneCount);
        if(rootJob-doneCount!=0){
            return;
        }
        this.jobGroupService.clearActionId(actionId);
    }


    /**
     * 验证当前记录是否已经做过
     * @param job
     * @param actionId
     * @return
     */
    private boolean checkCurrentJobHaveDone(JobVo job, Long actionId) {
        Record record = this.recordService.getRecord(actionId, job);
        if(record==null){
            return false;
        } else if(("-"+RunStatus.DONE.getStatus()
                +"-"+RunStatus.RERUNDONE.getStatus()
                +"-"+RunStatus.RUNNING.getStatus()
                +"-"+RunStatus.RERUNNING.getStatus()
                +"-").indexOf(record.getStatus())>-1){
            return true;
        }
        return false;
    }

    /**
     * 根据组ID和Job判断前置任务是否都已经成功完成
     * @param job
     * @return
     */
    public boolean judgeDependentJobsAllDone(JobVo job,Long actionId) {

        //判断是否有前置job
        List<JobVo> dependentJobs = jobDependenceService.dependentJob(job.getJobId());
        boolean allDone=true;//是否都已经完成了
        if(dependentJobs!=null && dependentJobs.size()>0){
            for(JobVo jobVo:dependentJobs){//遍历前置依赖
                Record record = recordService.getRecord(actionId, jobVo);
                if(record==null ||
                        !(record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus())
                                && (record.getStatus().equals(RunStatus.DONE.getStatus())
                                    || record.getStatus().equals(RunStatus.RERUNDONE.getStatus())))){//没有执行成功{
                    logger.info("job:{} depJob record:{} not done!",job.getJobId(),record==null?"null":record.getRecordId()+"");
                    allDone=false;
                    break;
                }
            }
        }

        return allDone;
    }


    /**
     * 流程任务 按流程任务处理方式区分
     */
    private boolean executeFlowJob(JobVo job) {
        if (!checkJobPermission(job.getAgentId(), job.getUserId())) return false;

        final long groupId = System.nanoTime() + Math.abs(new Random().nextInt());//分配一个流程组Id
        final Queue<JobVo> jobQueue = new LinkedBlockingQueue<JobVo>();
        jobQueue.add(job);
        jobQueue.addAll(job.getChildren());//所有孩子任务
        RunModel runModel = RunModel.getRunModel(job.getRunModel());
        switch (runModel) {
            case SEQUENCE://串行
                return executeSequenceJob(groupId, jobQueue);//串行任务
            case SAMETIME://并行
                return executeSameTimeJob(groupId, jobQueue);//并行任务
            default:
                return false;
        }
    }

    /**
     * 串行任务处理方式
     */
    private boolean executeSequenceJob(long groupId, Queue<JobVo> jobQueue) {
        for (JobVo jobVo : jobQueue) {
            if (!doFlowJob(jobVo, groupId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 并行任务处理方式
     */
    private boolean executeSameTimeJob(final long groupId, final Queue<JobVo> jobQueue) {
        final List<Boolean> result = new ArrayList<Boolean>(0);

        final Semaphore semaphore = new Semaphore(jobQueue.size());
        ExecutorService exec = Executors.newCachedThreadPool();

        for (final JobVo jobVo : jobQueue) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        semaphore.acquire();
                        result.add(doFlowJob(jobVo, groupId));
                        semaphore.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            exec.submit(task);
        }
        exec.shutdown();
        while (true) {
            if (exec.isTerminated()) {
                logger.info("[opencron]SameTimeJob,All doned!");
                return !result.contains(false);
            }
        }
    }

    /**
     * 执行这个job的具体任务
     * @param job
     * @param actionId
     * @return
     */
    private boolean doThisJob(JobVo job, Long actionId,boolean initChildJob) {

        Record record=this.recordService.getRecord(actionId,job);
        if(record==null){
            throw new InvalidException("pening record not found");
        }
        if(job.getPause() && job.getExecType().equals(Opencron.ExecType.AUTO.getStatus())){//如果当前任务已经暂停了，则修改这个记录为暂停
            record.setStatus(Opencron.RunStatus.STOPED.getStatus());
            record.setEndTime(new Date());
            recordService.merge(record);
            logger.info("job:{} pause,record:{} pause",job.getJobName(),record.getRecordId());
            return false;
        }
        int i =0;
        if(record.getStatus().equals(RunStatus.QUARTATRI.getStatus())){
            i=this.recordService.updateRecordRequiresNew(record.getRecordId(), RunStatus.QUARTATRI.getStatus(), RunStatus.RUNNING.getStatus());
        }else if(record.getStatus().equals(RunStatus.PENDING.getStatus())){
            i=this.recordService.updateRecordRequiresNew(record.getRecordId(), RunStatus.PENDING.getStatus(), RunStatus.RUNNING.getStatus());
        }else if(record.getStatus().equals(RunStatus.STOPED.getStatus())){
            i=this.recordService.updateRecordRequiresNew(record.getRecordId(), RunStatus.STOPED.getStatus(), RunStatus.RUNNING.getStatus());
        }
        if(i<1){
            logger.info("record:{}  update {} by other thread already!",record.getRecordId(),RunStatus.RUNNING.getStatus());
            return false;
        }

        //获取最新记录
        record =recordService.getRecord(actionId,job);

        boolean success = true;

        try {
            //执行前先保存

            //执行前先检测一次通信是否正常
            checkPing(job, record);

            //更改开始时间
            this.recordService.updateRecordStartTime(record.getRecordId());

            Response result = responseToRecord(job, record);//执行远程端命令

            logger.info(" action:{} job:{} result:{} record:{}",actionId,job.getJobName(),result.isSuccess(),record);
            record.setUniqueCode(null);

            if (!result.isSuccess()) {//如果没有成功
                success = false;
                recordService.merge(record);
                //被kill,直接退出
                if (StatusCode.KILL.getValue().equals(result.getExitCode())) {
                    recordService.flowJobDone(record);
                }else if (record.getSuccess().equals(ResultStatus.FAILED.getStatus())) {//超时
                    AlarmNoticeFacory.putMessage(job.getAlarmCode(), RightCode.AlarmCode.FAIL,job.getJobName());

                } else if(record.getSuccess().equals(ResultStatus.TIMEOUT.getStatus())){//失败
                    AlarmNoticeFacory.putMessage(job.getAlarmCode(), RightCode.AlarmCode.TIMEOUT,job.getJobName());
                }
                return false;
            }else{//如果成功了
                recordService.merge(record);

                AlarmNoticeFacory.putMessage(job.getAlarmCode(), RightCode.AlarmCode.SUCCESS,job.getJobName());

                job.setExecType(record.getExecType());//触发方式
                if(initChildJob){
                    initChildJob(actionId,job);//初始化所有任务的子任务
                }

                return true;
            }

        } catch (PingException e) {
            recordService.flowJobDone(record);//通信失败,流程任务挂起.
        }catch (Exception e){
            if (e instanceof PacketTooBigException) {
                record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at ip:%s,port:%d,info:", job, PACKETTOOBIG_ERROR, e));
            } else {
                record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at ip:%s,port:%d,info:%s", job, e.getMessage(), e));
            }
            if(record.getStatus().equals(RunStatus.DONE.getStatus())){
                record.setStatus(RunStatus.DONE.getStatus());
            }
            record.setSuccess(ResultStatus.FAILED.getStatus());//程序调用失败
            record.setReturnCode(StatusCode.ERROR_EXEC.getValue());
            record.setEndTime(new Date());
            recordService.merge(record);
            success = false;
            return false;
        }finally {
            //流程任务的重跑靠自身维护...
            if (!success) {
                Record red = recordService.get(record.getRecordId());
                if (job.getRedo() == 1 && job.getRunCount()!=null && job.getRunCount() > 0) {
                    int index = 0;
                    boolean flag;
                    do {
                        flag = reExecuteJob(red, job, JobType.FLOW);
                        ++index;
                    } while (!flag && index < job.getRunCount());
                }
            }
        }


        return false;
    }

    /**
     * 初始化所有孩子节点
     * @param actionId
     * @param job
     */
    public void initChildJob(Long actionId,JobVo job) {

//        if (!job.getLastChild()) {//如果不是最后一个节点则初始化后置任务的数据
        JobActionGroup actionGroup = job.getActionGroup();
        List<JobVo> childeJobs=jobDependenceService.childsNodeJob(job.getJobId());
            logger.info("action:{} job:{} childeSize:{}",actionId,job.getJobName(),childeJobs.size());
            if(CommonUtils.isEmpty(childeJobs)){
                logger.info("action:{} job:{} no child",actionId,job.getJobName());
                return;
            }
            for(JobVo childJob:childeJobs){
                try {
                    childJob.setActionGroup(actionGroup);

                    childJob.setExecType(job.getExecType());//设置执行方式
                    Record pendingRecord = recordService.insertPendingReocrd(actionId,childJob, RunStatus.PENDING);//加载下级任务的执行状态

                    //如果是手动触发，则原先已经做过的任务进行重做
                    if(job.getExecType().equals(ExecType.OPERATOR.getStatus())){//如果是手动触发
                        if(pendingRecord.getStatus().equals(RunStatus.DONE.getStatus())
                                || pendingRecord.getStatus().equals(RunStatus.STOPED.getStatus())
                                || pendingRecord.getStatus().equals(RunStatus.RERUNDONE.getStatus())){
                            logger.info("redo job:{},recordId:{}",childJob.getJobName(),pendingRecord.getRecordId());
                            recordService.updateOldRecorAndInsertNewRecord(pendingRecord,childJob);
                        }
                    }
                    logger.info("job:{} child insert  into recordId:{}  ",job.getJobName(),pendingRecord.getRecordId());
                } catch (Exception e) {
                    DBException.business(e,"UK_UNIQUECODE");
                    logger.info("insert job:{} record error:{}",childJob,e.getMessage());
                    continue;
                }
            }
//        }else{
//            logger.info("action:{} job:{} is last child",actionId,job.getJobName());
//        }
    }


    private Record createRecordByStatus(JobVo job,Long actionId,Integer status) {
        Record record=this.recordService.getRecord(actionId,job.getJobId());
        if(record!=null){
            record.setStatus(status);
            return record;
        }
        throw new InvalidException("pening record not found");
//        record=new Record(job);
//        record.setGroupId(job.getGroupId());//组Id
//        record.setActionId(actionId);
//        record.setJobType(job.getJobType());//job类型
//        record.setFlowNum(job.getFlowNum());
//        record.setStatus(status);//当前任务正在处理中
//        return record;
    }

    /**
     * 流程任务（通用）执行过程
     */
    private boolean doFlowJob(JobVo job, long groupId) {
        Record record = new Record(job);
        record.setGroupId(groupId);//组Id
        record.setJobType(JobType.FLOW.getCode());//流程任务
        record.setFlowNum(job.getFlowNum());

        boolean success = true;

        try {
            //执行前先保存
            record = recordService.merge(record);
            //执行前先检测一次通信是否正常
            checkPing(job, record);

            Response result = responseToRecord(job, record);

            if (!result.isSuccess()) {
                recordService.merge(record);
                //被kill,直接退出
                if (StatusCode.KILL.getValue().equals(result.getExitCode())) {
                    recordService.flowJobDone(record);
                } else {
                    success = false;
                }
                return false;
            } else {
                //当前任务是流程任务的最后一个任务,则整个任务运行完毕
                if (job.getLastChild()) {
                    recordService.merge(record);
                    recordService.flowJobDone(record);
                } else {
                    record.setStatus(RunStatus.RUNNING.getStatus());
                    recordService.merge(record);
                }
                return true;
            }
        } catch (PingException e) {
            recordService.flowJobDone(record);//通信失败,流程任务挂起.
            return false;
        } catch (Exception e) {
            if (e instanceof PacketTooBigException) {
                record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at ip:%s,port:%d,info:", job, PACKETTOOBIG_ERROR, e));
            } else {
                record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at ip:%s,port:%d,info:%s", job, e.getMessage(), e));
            }
            record.setSuccess(ResultStatus.FAILED.getStatus());//程序调用失败
            record.setReturnCode(StatusCode.ERROR_EXEC.getValue());
            record.setEndTime(new Date());
            recordService.merge(record);
            success = false;
            return false;
        } finally {
            //流程任务的重跑靠自身维护...
            if (!success) {
                Record red = recordService.get(record.getRecordId());
                if (job.getRedo() == 1 && job.getRunCount() > 0) {
                    int index = 0;
                    boolean flag;
                    do {
                        flag = reExecuteJob(red, job, JobType.FLOW);
                        ++index;
                    } while (!flag && index < job.getRunCount());

                    //重跑到截止次数还是失败,则发送通知,记录最终运行结果
                    if (!flag) {
                        noticeService.notice(job, null);
                        recordService.flowJobDone(record);
                    }
                } else {
                    noticeService.notice(job, null);
                    recordService.flowJobDone(record);
                }
            }
        }

    }

    /**
     * 多执行器同时 现场执行过程
     */
    public void batchExecuteJob(final Long userId, String command, String agentIds) {
        String[] arrayIds = agentIds.split(";");
        final Semaphore semaphore = new Semaphore(arrayIds.length);
        ExecutorService exec = Executors.newCachedThreadPool();
        for (String agentId : arrayIds) {
            Agent agent = agentService.getAgent(Long.parseLong(agentId));
            final JobVo jobVo = new JobVo(userId, command, agent);
            jobVo.setRunAs("root");
            jobVo.setSuccessExit("0");

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        semaphore.acquire();
                        executeSingleJob(jobVo, userId);
                        semaphore.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            exec.submit(task);
        }
        exec.shutdown();
        while (true) {
            if (exec.isTerminated()) {
                logger.info("[opencron]batchExecuteJob doned!");
                break;
            }
        }
    }

    /**
     * 失败任务的重执行过程
     */
    public boolean reExecuteJob(final Record parentRecord, JobVo job, JobType jobType) {

        /*if (parentRecord.getRedoCount().equals(reExecuteThreadMap.get(parentRecord.getRecordId()))) {
            return false;
        } else {
            reExecuteThreadMap.put(parentRecord.getRecordId(), parentRecord.getRedoCount());
        }*/

        parentRecord.setStatus(RunStatus.RERUNDONE.getStatus());
        Record record = new Record(job);

        try {

            /**
             * 当前重新执行的新纪录
             */
            job.setExecType(ExecType.RERUN.getStatus());
            record.setActionId(parentRecord.getActionId());
            record.setParentId(parentRecord.getRecordId());
            record.setGroupId(parentRecord.getGroupId());
            record.setJobType(jobType.getCode());
            parentRecord.setRedoCount(parentRecord.getRedoCount() + 1);//运行次数
            record.setRedoCount(parentRecord.getRedoCount());
            record.setStatus(RunStatus.RUNNING.getStatus());//当前记录为运行中

            JobActionGroup actionGroup = job.getActionGroup();

            if(StringUtils.isNullString(actionGroup.getParam())){
                Date startTime = actionGroup.getStartTime();
                if(record.getCommand().indexOf("$")>-1){
                    record.setCommand(ParamUntils.command(record.getCommand(),startTime));//替换具体的变量值
                }
            }else{//替换待参数的部分
                record.setCommand(ParamUntils.replaceCmd(record.getCommand(),actionGroup.getParam()));
            }

            record = recordService.merge(record);

            //执行前先检测一次通信是否正常
            checkPing(job, record);

            Response result = responseToRecord(job, record);

            //当前重跑任务成功,则父记录执行完毕
            if (result.isSuccess()) {
                parentRecord.setStatus(RunStatus.RERUNDONE.getStatus());

                //发送成功通知
                AlarmNoticeFacory.putMessage(job.getAlarmCode(), RightCode.AlarmCode.SUCCESS,job.getJobName());

                initChildJob(parentRecord.getActionId(),job);

                //重跑的某一个子任务被Kill,则整个重跑计划结束
            } else if (StatusCode.KILL.getValue().equals(result.getExitCode())) {
                parentRecord.setStatus(RunStatus.RERUNDONE.getStatus());
            } else if (record.getSuccess().equals(ResultStatus.FAILED.getStatus())) {//超时
                AlarmNoticeFacory.putMessage(job.getAlarmCode(), RightCode.AlarmCode.FAIL,job.getJobName());

            } else if(record.getSuccess().equals(ResultStatus.TIMEOUT.getStatus())){//失败
                AlarmNoticeFacory.putMessage(job.getAlarmCode(), RightCode.AlarmCode.TIMEOUT,job.getJobName());
            }
            this.loggerInfo("execute successful:jobName:{} at ip:{},port:{}", job, null);
        } catch (Exception e) {
            if (e instanceof PacketTooBigException) {
                noticeService.notice(job, PACKETTOOBIG_ERROR);
                errorExec(record, this.loggerError("execute failed:jobName:%s at ip:%s,port:%d,info:%s", job, PACKETTOOBIG_ERROR, e));
            }
            noticeService.notice(job, e.getMessage());
            errorExec(record, this.loggerError("execute failed:jobName:%s at ip:%s,port:%d,info:%s", job, e.getMessage(), e));

        } finally {
            try {
                recordService.merge(record);
                recordService.merge(parentRecord);
            } catch (Exception e) {
                if (e instanceof PacketTooBigException) {
                    record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at ip:%s,port:%d,info:" + PACKETTOOBIG_ERROR, job, e.getMessage(), e));
                } else {
                    record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at ip:%s,port:%d,info:%s", job, e.getMessage(), e));
                }
            }

        }
        return record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus());
    }

    /**
     * 终止任务过程
     */
    public boolean killJob(List<Record> records) {

        final Queue<Record> recordQueue = new LinkedBlockingQueue<Record>();
            recordQueue.addAll(records);

        final List<Boolean> result = new ArrayList<Boolean>(0);

        final Semaphore semaphore = new Semaphore(recordQueue.size());
        ExecutorService exec = Executors.newCachedThreadPool();

        for (final Record cord : recordQueue) {
            cord.setUniqueCode(null);
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    JobVo job = null;
                    try {
                        semaphore.acquire();
                        //临时的改成停止中...
                        cord.setStatus(RunStatus.STOPPING.getStatus());//停止中
                        cord.setSuccess(ResultStatus.KILLED.getStatus());//被杀.
                        recordService.merge(cord);
                        job = jobService.getJobVoById(cord.getJobId());
                        //向远程机器发送kill指令
                        opencronCaller.call(Request.request(job.getIp(), job.getPort(), Action.KILL, job.getPassword()).putParam("pid", cord.getPid()), job.getAgent());
                        cord.setStatus(RunStatus.STOPED.getStatus());
                        cord.setEndTime(new Date());
                        recordService.merge(cord);
                        loggerInfo("killed successful :jobName:{} at ip:{},port:{},pid:{}", job, cord.getPid());
                    } catch (Exception e) {
                        if (e instanceof PacketTooBigException) {
                            noticeService.notice(job, PACKETTOOBIG_ERROR);
                            loggerError("killed error:jobName:%s at ip:%s,port:%d,pid:%s", job, cord.getPid() + " failed info: " + PACKETTOOBIG_ERROR, e);
                        }
                        noticeService.notice(job, null);
                        loggerError("killed error:jobName:%s at ip:%s,port:%d,pid:%s", job, cord.getPid() + " failed info: " + e.getMessage(), e);

                        logger.error("[opencron] job rumModel with SAMETIME error:{}", e.getMessage());

                        result.add(false);
                    }
                    semaphore.release();
                }
            };
            exec.submit(task);
        }
        exec.shutdown();
        while (true) {
            if (exec.isTerminated()) {
                logger.info("[opencron] SAMETIMEjob done!");
                return !result.contains(false);
            }
        }
    }

    /**
     * 向执行器发送请求，并封装响应结果
     */
    public Response responseToRecord(final JobVo job, final Record record) throws Exception {
        Response response = opencronCaller.call(Request.request(job.getIp(), job.getPort(), Action.EXECUTE, job.getPassword())
                .putParam("command", record.getCommand())
                .putParam("pid", record.getPid())
                .putParam("timeout", job.getTimeout() + "")
                .putParam("runAs",job.getRunAs())
                .putParam("successExit",job.getSuccessExit()), job.getAgent());

        logger.info("[opencron]:execute response:{}", response.toString());
        record.setReturnCode(response.getExitCode());
        record.setMessage(response.getMessage());

        record.setSuccess(response.isSuccess() ? ResultStatus.SUCCESSFUL.getStatus() : ResultStatus.FAILED.getStatus());
        if (StatusCode.KILL.getValue().equals(response.getExitCode())) {
            record.setStatus(RunStatus.STOPED.getStatus());
            record.setSuccess(ResultStatus.KILLED.getStatus());//被kill任务失败
        } else if (StatusCode.TIME_OUT.getValue().equals(response.getExitCode())) {
            record.setStatus(RunStatus.STOPED.getStatus());
            record.setSuccess(ResultStatus.TIMEOUT.getStatus());//超时...
        } else {
            record.setStatus(RunStatus.DONE.getStatus());
        }

        record.setStartTime(new Date(response.getStartTime()));
        record.setEndTime(new Date(response.getEndTime()));
        return response;
    }

    /**
     * 调用失败后的处理
     */
    private void errorExec(Record record, String errorInfo) {
        record.setSuccess(ResultStatus.FAILED.getStatus());//程序调用失败
        record.setStatus(RunStatus.RERUNDONE.getStatus());//已完成
        record.setReturnCode(StatusCode.ERROR_EXEC.getValue());
        record.setEndTime(new Date());
        record.setMessage(errorInfo);
        recordService.merge(record);
    }


    /**
     * 任务执行前 检测通信
     */
    public void checkPing(JobVo job, Record record) throws PingException {
        boolean ping = ping(job.getAgent());
        if ( ! ping ) {
            record.setStatus(RunStatus.DONE.getStatus());//已完成
            record.setReturnCode(StatusCode.ERROR_PING.getValue());

            String format = "can't to communicate with agent:%s(%s:%d),execute job:%s failed";
            String content = String.format(format, job.getAgentName(), job.getIp(), job.getPort(), job.getJobName());

            record.setMessage(content);
            record.setSuccess(ResultStatus.FAILED.getStatus());
            record.setEndTime(new Date());
            recordService.merge(record);
            throw new PingException(content);
        }
    }

    public boolean ping(Agent agent) {
        try {
            Response response = opencronCaller.call(Request.request(agent.getIp(), agent.getPort(), Action.PING, agent.getPassword()).putParam("serverPort", OpencronMonitor.port + ""), agent);
            return response!=null && response.isSuccess();
        } catch (Exception e) {
            logger.error("[opencron]ping failed,host:{},port:{}", agent.getIp(), agent.getPort());
            return false;
        }
    }

    public String guid(Agent agent) {
        try {
            Response response = opencronCaller.call(Request.request(agent.getIp(), agent.getPort(), Action.GUID,agent.getPassword()), agent);
            return response.getMessage();
        } catch (Exception e) {
            logger.error("[opencron]getguid failed,host:{},port:{}", agent.getIp(), agent.getPort());
            return null;
        }
    }

    public String path(Agent agent) {
        try {
            Response response = opencronCaller.call(Request.request(agent.getIp(), agent.getPort(), Action.PATH,null), agent);
            return response.getMessage();
        } catch (Exception e) {
            logger.error("[opencron]ping failed,host:{},port:{}", agent.getIp(), agent.getPort());
            return null;
        }
    }

    /**
     * 修改密码
     */
    public boolean password(Agent agent, final String newPassword) {
        boolean ping = false;
        try {
            Response response = opencronCaller.call(Request.request(agent.getIp(), agent.getPort(), Action.PASSWORD, agent.getPassword())
                    .putParam("newPassword", newPassword), agent);
            ping = response.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ping;
    }

    /**
     * 监测执行器运行状态
     */
    public Response monitor(Agent agent) throws Exception {
        return opencronCaller.call(
                Request.request(agent.getIp(), agent.getPort(), Action.MONITOR, agent.getPassword())
                        .setParams(ParamsMap.instance().fill("connType", ConnType.getByType(agent.getProxy()).getName())), agent);
    }

    /**
     * 校验任务执行权限
     */
    public boolean checkJobPermission(Long jobAgentId, Long userId) {
        if (userId == null) return false;
        User user = userService.getUserById(userId);
        //超级管理员拥有所有执行器的权限
        if (user != null && user.getRoleId() == 999) return true;
        String agentIds = userService.getUserById(userId).getAgentIds();
        agentIds = "," + agentIds + ",";
        String thisAgentId = "," + jobAgentId + ",";
        return agentIds.contains(thisAgentId);
    }

    private void loggerInfo(String str, JobVo job, String message) {
        if (message != null) {
            logger.info(str, job.getJobName(), job.getIp(), job.getPort(), message);
        } else {
            logger.info(str, job.getJobName(), job.getIp(), job.getPort());
        }
    }

    private String loggerError(String str, JobVo job, String message, Exception e) {
        String errorInfo = String.format(str, job.getJobName(), job.getIp(), job.getPort(), message);
        logger.error(errorInfo, e);
        return errorInfo;
    }


}
