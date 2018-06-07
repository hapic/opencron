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


package org.opencron.server.job;

import org.opencron.common.job.Opencron;
import org.opencron.common.utils.CommonUtils;
import org.opencron.common.utils.DateUtils;
import org.opencron.server.alarm.AlarmNoticeFacory;
import org.opencron.server.alarm.AlarmTypes;
import org.opencron.server.alarm.MsgTemplet;
import org.opencron.server.domain.Record;
import org.opencron.server.service.*;
import org.opencron.server.vo.JobActionGroupVo;
import org.opencron.server.vo.JobVo;
import org.opencron.server.vo.RecordVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import static java.util.concurrent.Executors.newFixedThreadPool;


@Component
public class OpencronTask implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(OpencronTask.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private OpencronMonitor opencronMonitor;

    @Autowired
    private ConcurrencyControl concurrencyControl;

    @Autowired
    private JobActionGroupService jobActionGroupService;

    @Override
    public void afterPropertiesSet() throws Exception {
        configService.initDataBase();
        concurrencyControl.initAtomic(configService);
        //检测所有的agent...
        clearCache();
        //通知所有的agent,启动心跳检测...
        opencronMonitor.start();
        schedulerService.initQuartz(executeService);
        schedulerService.initCrontab();

    }

    /**
     * 重新执行任务
     */
//    @Scheduled(cron = "0/5 * * * * ?")
    public void reExecuteJob() {
        logger.info("[opencron] reExecuteIob running...");
        final List<Record> records = recordService.getReExecuteRecord();
        if (CommonUtils.notEmpty(records)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (final Record record : records) {
                        final JobVo jobVo = jobService.getJobVoById(record.getJobId());
                        logger.info("[opencron] reexecutejob:jobName:{},jobId:{},recordId:{}", jobVo.getJobName(), jobVo.getJobId(), record.getRecordId());
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                jobVo.setAgent(agentService.getAgent(jobVo.getAgentId()));
                                executeService.reExecuteJob(record, jobVo, Opencron.JobType.SINGLETON);
                            }
                        });
                        thread.start();
                    }
                }
            }).start();
        }
    }

    /**
     * 任务超时未完成告警
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void unfinishedAlarm() {
            //加载超时(3h)未完成的
        String beforeTime= DateUtils.beforeTime(3*60);
        List<RecordVo> records =recordService.loadUnfinishedRecord(beforeTime);
        if (CommonUtils.notEmpty(records)) {
            for(RecordVo record:records){
                String jobName = record.getJobName();
                Date startTime = record.getStartTime();
                String timeOutUnfinishedMsg =
                        MsgTemplet.getTimeOutUnfinishedMsg(jobName, DateUtils.formatFullDate(startTime));

                AlarmNoticeFacory.sendMsg(null,timeOutUnfinishedMsg, AlarmTypes.DINGDING);
            }
        }
    }

    //完成后的消息通知
//    @Scheduled(cron = "0 0/5 * * * ?")
    public void finishedAlarm() {
        String beginTimeCurDay = DateUtils.getBeginTimeCurDayTime();
        String endTimeCurDay = DateUtils.getEndTimeCurDayTime();

        List<JobActionGroupVo> jobActionGroups =
                jobActionGroupService.loadFinishedGroup(beginTimeCurDay, endTimeCurDay);
        for(JobActionGroupVo jag:jobActionGroups){

            String groupFinishedMsg =
                    MsgTemplet.getGroupFinishedMsg(jag.getGroupName());

            AlarmNoticeFacory.sendMsg(null,groupFinishedMsg, AlarmTypes.DINGDING);

            logger.info("send msg group:{} msg",jag.getGroupName());
            int i=jobActionGroupService.updateActionGroupAlarm(jag.getId(),jag.getAlarm(),1);
            logger.info("update ActionGroupStatus:{},result:{}",jag.getId(),i);
        }
    }

    private void clearCache() {
        OpencronTools.CACHE.remove(OpencronTools.CACHED_AGENT_ID);
        OpencronTools.CACHE.remove(OpencronTools.CACHED_JOB_ID);
    }


}
