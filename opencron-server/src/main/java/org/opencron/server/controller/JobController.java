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

package org.opencron.server.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.opencron.common.graph.KahnTopo;
import org.opencron.common.graph.Node;
import org.opencron.common.job.Opencron;
import org.opencron.common.utils.CommonUtils;
import org.opencron.common.utils.DigestUtils;
import org.opencron.common.utils.StringUtils;
import org.opencron.server.domain.*;
import org.opencron.server.job.OpencronTools;
import org.opencron.server.service.*;
import org.opencron.server.tag.PageBean;
import org.opencron.server.until.GraphUntils;
import org.opencron.server.vo.JobGroupVo;
import org.opencron.server.vo.JobVo;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.opencron.common.utils.CommonUtils.notEmpty;
import static org.opencron.common.utils.WebUtils.write404;
import static org.opencron.common.utils.WebUtils.writeJson;

@Slf4j
@Controller
@RequestMapping("job")
public class JobController extends BaseController {

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobGroupService jobGroupService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private JobDependenceService jobDependenceService;

    @Autowired
    private JobActionGroupService jobActionGroupService;

    @RequestMapping("view.htm")
    public String view(HttpSession session, HttpServletRequest request, PageBean pageBean, JobVo job, Model model) {

        model.addAttribute("agents", agentService.getOwnerAgents(session));

        if (notEmpty(job.getGroupId())) {
            model.addAttribute("groupId", job.getGroupId());
        }
        if (notEmpty(job.getAgentId())) {
            model.addAttribute("agentId", job.getAgentId());
        }
        if (notEmpty(job.getCronType())) {
            model.addAttribute("cronType", job.getCronType());
        }
        if (notEmpty(job.getJobType())) {
            model.addAttribute("jobType", job.getJobType());
        }
        if (notEmpty(job.getExecType())) {
            model.addAttribute("execType", job.getExecType());
        }
        if (notEmpty(job.getRedo())) {
            model.addAttribute("redo", job.getRedo());
        }
        if (notEmpty(job.getGroupId())) {
            model.addAttribute("groupId", job.getGroupId());
        }
        model.addAttribute("groups", jobGroupService.getAll());

        jobService.getJobVos(session, pageBean, job);
        if (request.getParameter("refresh") != null) {
            return "/job/refresh";
        }
        return "/job/view";
    }

    /**
     * 同一台执行器上不能有重复名字的job
     *
     * @param jobId
     * @param agentId
     * @param name
     */
    @RequestMapping(value = "checkname.do",method= RequestMethod.POST)
    @ResponseBody
    public boolean checkName(Long jobId, Long agentId, String name) {
        return !jobService.existsName(jobId, agentId, name);
    }

    @RequestMapping(value = "checkdel.do",method= RequestMethod.POST)
    @ResponseBody
    public String checkDelete(Long id) {
        return jobService.checkDelete(id);
    }

    @RequestMapping(value = "delete.do",method= RequestMethod.POST)
    @ResponseBody
    public boolean delete(Long id) {
        try {
            //如果有子任务则不能删除

            int childCount = jobDependenceService.childCount(id);
            if(childCount>0){
                log.info("delete:{} fail,child job count:{}",id,childCount);
                return false;
            }
            jobService.delete(id);
            return true;
        } catch (SchedulerException e) {
            e.printStackTrace();
            return false;
        }
    }

    @RequestMapping("add.htm")
    public String addpage(HttpSession session, Model model, Long id,Long groupId) {
        if (notEmpty(id)) {
            Agent agent = agentService.getAgent(id);
            model.addAttribute("agent", agent);
        }
        List<Agent> agents = agentService.getOwnerAgents(session);
        model.addAttribute("agents", agents);

        //加载所有作业组列表
        List<JobGroup> groupList=jobGroupService.getAllOrbyGroupId(groupId);
        model.addAttribute("jobGroupList", groupList);
        return "/job/add";
    }

    @RequestMapping("init.htm")
    public String init(HttpSession session, Model model, Long id,Long groupId) {
       this.addpage(session,model,id,groupId);
        return "/job/initJob";
    }

    @RequestMapping(value = "initJobList.do",method= RequestMethod.POST)
    public String save(HttpSession session, Job job,String commentList, HttpServletRequest request) throws SchedulerException {

        Long groupId = job.getGroupId();
        Long agentId = job.getAgentId();

        String[] commentArr = commentList.split("#");

        List<Job> jobVos= new ArrayList<>();
        for(String commant:commentArr){
            Job jobVo= new Job();

            jobVo.setGroupId(groupId);
            jobVo.setFlowNum(0);
            jobVo.setAgentId(agentId);
            jobVo.setCronExp(job.getCronExp());

            jobVo.setCronType(Opencron.CronType.QUARTZ.getType());
            jobVo.setExecType(Opencron.ExecType.AUTO.getStatus());
            jobVo.setRedo(0);
            jobVo.setRunAs("root");
            jobVo.setSuccessExit("0");
            jobVo.setTimeout(0);
            jobVo.setWarning(false);

            String jobName = commant.substring(commant.lastIndexOf("/")+1);
            jobVo.setJobName(jobName);
            jobVo.setCommand(commant);
            jobVo.setDeleted(false);
            jobVo.setPause(false);//未暂停
            jobVo.setUserId(OpencronTools.getUserId(session));
            jobVo.setUpdateTime(new Date());
            jobVo.setLastChild(true);//默认未最后一个子节点
            jobVo.setJobType(Opencron.JobType.FLOW.getCode());
            jobVos.add(jobVo);
        }

        jobService.batchIinitJob(jobVos);

        for(Job job2:jobVos){
            log.info("start job:{}",job2.getJobId());
            schedulerService.syncJobTigger(job2.getJobId(), executeService);
        }

        return "redirect:/job/view.htm?csrf=" + OpencronTools.getCSRF(session);
    }


    @RequestMapping(value = "save.do",method= RequestMethod.POST)
    public String save(HttpSession session, Job job,String[] dependenceid, HttpServletRequest request) throws SchedulerException {

        job.setCommand(DigestUtils.passBase64(job.getCommand()));
        job.setDeleted(false);
        job.setPause(false);//未暂停
        job.setUserId(OpencronTools.getUserId(session));
        job.setUpdateTime(new Date());
        job.setLastChild(true);//默认未最后一个子节点
        job.setJobType(Opencron.JobType.FLOW.getCode());
        String groupParam="";
        if(job.getGroupId()!=null && job.getGroupId()>0){
            groupParam="&groupId="+job.getGroupId();
        }
        if(!this.checkName(job.getJobId(),job.getAgentId(),job.getJobName())){
            return "redirect:/job/view.htm?csrf=" + OpencronTools.getCSRF(session)+groupParam;
        }
        if (job.getJobId() != null) {
            /**
             * 如果作业已经存在，则进行修改操作，首先验证当前用户是否有权限修改
             */
            Job job1 = jobService.getJob(job.getJobId());
            if (!jobService.checkJobOwner(session, job1.getUserId())){
                //没有权限修改
                return "redirect:/job/view.htm?csrf=" + OpencronTools.getCSRF(session)+groupParam;
            }
            /**
             * 将数据库中持久化的作业和当前修改的合并,当前修改的属性覆盖持久化的属性...
             */
            BeanUtils.copyProperties(job1, job, "jobName", "cronType", "cronExp", "command", "execType", "comment","runAs","successExit", "redo", "runCount", "jobType", "runModel", "warning", "mobiles", "emailAddress", "timeout");
        }
        /**
         * 首先判断是否有依赖
         */
        if(dependenceid==null || dependenceid.length<1){//如果没有依赖，则是单独的一个任务
            job.setFlowNum(0);
            job = jobService.merge(job);
            log.info("merge job:{}",job);
        }else{

            //加载当前job所在组的依赖关系
            List<JobDependence> jobDependenceList =jobDependenceService.loadDependence(job.getGroupId());

            KahnTopo<Long> graph = GraphUntils.createGraph(jobDependenceList);

            //更新Job的flowNum值
            jobService.batchUpdateJobFlowNum(graph.getResult());
            /**
             * 加载上级依赖
             */
             List<Job> dependentJob= buildDepnedentJobShip(job,dependenceid);

            //保存当前Job，和添加依赖关系
            job= jobService.saveDependentJobs(job,dependentJob);
            log.info("saveDependentJobs jobId:{} dependentJob:{}",job,dependentJob.size());
        }
        //只初始化根任务
        schedulerService.syncJobTigger(job.getJobId(), executeService);

        return "redirect:/job/view.htm?csrf=" + OpencronTools.getCSRF(session)+groupParam;
    }

    @RequestMapping("editsingle.do")
    public void editSingleJob(HttpSession session, HttpServletResponse response, Long id) {
        JobVo job = jobService.getJobVoById(id);
        if (job == null) {
            write404(response);
            return;
        }
        if (!jobService.checkJobOwner(session, job.getUserId())) return;
        writeJson(response, JSON.toJSONString(job));
    }

    @RequestMapping("editflow.htm")
    public String editFlowJob(HttpSession session, Model model, Long id,Long groupId) {
        JobVo job = jobService.getJobVoById(id);

        if (job == null) {
            return "/error/404";
        }
        if (!jobService.checkJobOwner(session, job.getUserId()))
            return "redirect:/job/view.htm?csrf=" + OpencronTools.getCSRF(session)+"&groupId="+groupId;
        model.addAttribute("job", job);

        List<JobVo> dependentJob = jobDependenceService.dependentJob(id);
        if(CommonUtils.notEmpty(dependentJob)){
            job.setDependenceJob(dependentJob);
            Map map= new HashMap();
            for(JobVo vo:dependentJob){
                map.put(vo.getJobId(),vo);
            }
            model.addAttribute("dependenceMap", map);
        }

        List<Agent> agents = agentService.getOwnerAgents(session);
        model.addAttribute("agents", agents);


        List<JobVo> jobVos = jobService.loadJobByGroupId(job.getGroupId(),job.getJobId());
        model.addAttribute("jobs", jobVos);

        //查询组名，不可编辑

        return "/job/edit";
    }

    @RequestMapping(value = "loadJobByGroupId.do",method= RequestMethod.POST)
    @ResponseBody
    public List<JobVo> loadJobByGroupId(HttpSession session,Long groupId){

        List<JobVo> listVo= this.jobService.loadJobByGroupId(groupId,null);

        return listVo;
    }

    @RequestMapping(value = "checkCycle.do", method = RequestMethod.POST)
    @ResponseBody
    public boolean checkCycle(Job job,String[] dependenceid) {
        Job dbJob = jobService.getJob(job.getJobId());

        List<JobDependence> jobDependenceList =jobDependenceService.loadDependence(dbJob.getGroupId());

        KahnTopo<Long> graph = GraphUntils.createGraph(dbJob.getJobId(),dependenceid,jobDependenceList);

        return graph.hasCycle();
    }

    @RequestMapping(value = "edit.do",method= RequestMethod.POST)
    public String edit(HttpSession session,Job job,String[] dependenceid) throws SchedulerException {
        Job dbJob = jobService.getJob(job.getJobId());

        String groupParam="";
        if(dbJob.getGroupId()!=null && dbJob.getGroupId()>0){
            groupParam="&groupId="+dbJob.getGroupId();
        }

        if (!jobService.checkJobOwner(session, dbJob.getUserId()))
            return "redirect:/job/view.htm?csrf=" + OpencronTools.getCSRF(session)+groupParam;

        dbJob.setExecType(job.getExecType());
        dbJob.setCronType(job.getCronType());
        dbJob.setCronExp(job.getCronExp());
        dbJob.setCommand(DigestUtils.passBase64(job.getCommand()));
        dbJob.setJobName(job.getJobName());
        dbJob.setRunAs(job.getRunAs());
        dbJob.setSuccessExit(job.getSuccessExit());
        dbJob.setRedo(job.getRedo());
        dbJob.setRunCount(job.getRunCount());
        dbJob.setWarning(job.getWarning());
        dbJob.setTimeout(job.getTimeout());
        if (dbJob.getWarning()) {
            dbJob.setMobiles(job.getMobiles());
            dbJob.setEmailAddress(job.getEmailAddress());
        }
        dbJob.setComment(job.getComment());
        dbJob.setUpdateTime(new Date());

        //获取原有的依赖关系
        List<Job> oldDependentJob = jobDependenceService.dependentJob2(dbJob.getJobId());

        //获取最新的依赖关系，如果没有依赖关系，则根据解除与老依赖关系
        if(dependenceid==null || dependenceid.length<1){//如果没有依赖，则是单独的一个任务
            job.setFlowNum(0);

            //求两个集合的差集
           List<Job> difJob= differenceSet(oldDependentJob,null);

            /**
             * 更新依赖关系
             */
            jobService.updateDependence(dbJob,difJob);
            log.info("merge job:{}",job);


        }else{
            job.setJobType(Opencron.JobType.FLOW.getCode());

            /**
             * 加载上级依赖
             */
            List<Job> newDependentJob= buildDepnedentJobShip(job,dependenceid);


            List<Job> difJob= differenceSet(oldDependentJob,newDependentJob);


            //保存当前Job，和添加依赖关系
            jobService.updateDependence(dbJob,difJob);
            log.info("saveDependentJobs jobId:{} dependentJob:{}",job,newDependentJob.size());

        }

        schedulerService.syncJobTigger(dbJob.getJobId(), executeService);

        return "redirect:/job/view.htm?csrf=" + OpencronTools.getCSRF(session)+groupParam;
    }

    /**
     * 求两个 集合的差集
     * @param oldDependentJob
     * @param newDependentJob
     * @return
     */
    public List<Job> differenceSet(List<Job> oldDependentJob, List<Job> newDependentJob) {
        if(CommonUtils.isEmpty(oldDependentJob) && CommonUtils.isEmpty(newDependentJob)){
            return null;
        }else if(CommonUtils.isEmpty(oldDependentJob)){
            return newDependentJob;
        }else if(CommonUtils.isEmpty(newDependentJob)){
            return oldDependentJob;
        }

        List<Job> allJob= new ArrayList<>();

        Set<Long> oneSet= new HashSet<>();
        Set<Long> otherSet= new HashSet<>();

        ConcurrentMap<Long,Job> mapJob= new ConcurrentHashMap<>();
        for(Job job:oldDependentJob){
            oneSet.add(job.getJobId());
            mapJob.putIfAbsent(job.getJobId(),job);
        }
        for(Job job:newDependentJob){
            otherSet.add(job.getJobId());
            mapJob.putIfAbsent(job.getJobId(),job);
        }

        Collection disjunction = CollectionUtils.disjunction(oneSet, otherSet);
        Iterator iterator = disjunction.iterator();
        while (iterator.hasNext()){
            Long next = CommonUtils.toLong(iterator.next());
            allJob.add(mapJob.get(next));
        }
        return allJob;
    }

    /**
     * 根据依赖id构建依赖关系
     * @param job
     * @param dependenceid
     * @return
     */
    public List<Job> buildDepnedentJobShip(Job job,String[] dependenceid) {
        int maxFlowNum=0;

        List<Job> dependentJob =new ArrayList<>();

        for(String depParentJobId:dependenceid){
            if(StringUtils.isNotNullString(depParentJobId)){
                Job oneJob = jobService.getJob(Long.parseLong(depParentJobId));
                log.info("load one job id:{} {}",depParentJobId,oneJob);

                if(oneJob!=null){

                    maxFlowNum=Math.max(maxFlowNum,oneJob.getFlowNum());
                    dependentJob.add(oneJob);
                }
            }
        }
        job.setFlowNum(maxFlowNum+1);

        return dependentJob;
    }

    @RequestMapping(value = "editcmd.do",method= RequestMethod.POST)
    @ResponseBody
    public boolean editCmd(HttpSession session,Long jobId, String command) throws SchedulerException {
        command = DigestUtils.passBase64(command);
        Job dbJob = jobService.getJob(jobId);
        if (!jobService.checkJobOwner(session, dbJob.getUserId())) return false;
        dbJob.setCommand(command);
        dbJob.setUpdateTime(new Date());
        jobService.merge(dbJob);
        schedulerService.syncJobTigger(Opencron.JobType.FLOW.getCode().equals(dbJob.getJobType()) ? dbJob.getFlowId() : dbJob.getJobId(), executeService);
        return true;
    }

    @RequestMapping(value = "canrun.do",method= RequestMethod.POST)
    @ResponseBody
    public boolean canRun(Long id) {
        return recordService.isRunning(id);
    }

    @RequestMapping(value = "execute.do",method= RequestMethod.POST)
    @ResponseBody
    public boolean remoteExecute(HttpSession session, Long id,String inputParam) {
        JobVo job = jobService.getJobVoById(id);//找到要执行的任务
        if (!jobService.checkJobOwner(session, job.getUserId())) return false;
        //手动执行
        Long userId = OpencronTools.getUserId(session);
        job.setUserId(userId);
        job.setExecType(Opencron.ExecType.OPERATOR.getStatus());
        job.setAgent(agentService.getAgent(job.getAgentId()));
        Record record=null;
        String param=null;
        if(inputParam.startsWith("$")){
            String newParam = inputParam.replace("$", "");
            String[] cmds = newParam.split(";");
            Long recordId=0L;
            for(String cmd:cmds){
                if(cmd.startsWith("r=")){
                    String s = cmd.replace("r=", "");
                    recordId=Long.parseLong(s);
                }else if(cmd.startsWith("p=")){
                    String s = cmd.replace("p=", "");
                    param=s;
                }
            }
            record=this.recordService.get(recordId);

        }else if(inputParam.startsWith("#")){//如果以#号开头，则使用最新的一个记录
            String newParam = inputParam.replace("#", "");
            if(newParam.startsWith("p=")){
                String s = newParam.replace("p=", "");
                param=s;
            }
            record= this.recordService.loadLastRecord(id);
        }
        job.setParam(param);
        if(record!=null ){//执行成功或失败的记录
            job.setRecordId(record.getRecordId());
            job.setActionId(record.getActionId());
        }else{//执行起始节点任务
            Long actionId= jobActionGroupService.acquire(job);
            job.setActionId(actionId);
            this.jobActionGroupService.updateActionGroup(job, actionId);
            record = this.recordService.insertPendingReocrd(actionId, job);
            job.setRecordId(record.getRecordId());
        }

        try {
            this.executeService.executeJob(job,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @RequestMapping("goexec.htm")
    public String goExec(HttpSession session, Model model) {
        model.addAttribute("agents", agentService.getOwnerAgents(session));
        return "/job/exec";
    }

    @RequestMapping(value = "batchexec.do",method= RequestMethod.POST)
    @ResponseBody
    public boolean batchExec(HttpSession session, String command, String agentIds) {
        if (notEmpty(agentIds) && notEmpty(command)) {
            command = DigestUtils.passBase64(command);
            Long userId = OpencronTools.getUserId(session);
            try {
                this.executeService.batchExecuteJob(userId, command, agentIds);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @RequestMapping(value = "pause.do", method = RequestMethod.POST)
    @ResponseBody
    public boolean pause(Job jobBean) {
        return jobService.pauseJob(jobBean);
    }

    @RequestMapping("detail/{id}.htm")
    public String showDetail(HttpSession session, Model model,@PathVariable("id") Long id) {
        JobVo jobVo = jobService.getJobVoById(id);
        if (jobVo == null) {
            return "/error/404";
        }
        if (!jobService.checkJobOwner(session, jobVo.getUserId())) {
            return "redirect:/job/view.htm?csrf=" + OpencronTools.getCSRF(session);
        }
        model.addAttribute("job", jobVo);
        return "/job/detail";
    }


    /**
     * 根据组加载所可用的job
     * @return
     */
    @RequestMapping(value = "depJobByGroupId.do", method = RequestMethod.POST)
    @ResponseBody
    public List<JobVo> depJobByGroupId(Long groupId){
        Long actionId=jobActionGroupService.loadLastActionId(groupId);
        List<JobVo> listJob=this.jobDependenceService.loadJobByGroupId(groupId,actionId);
        if(listJob!=null &&listJob.size()>0 && actionId!=null){
            listJob.get(0).setActionId(actionId);
        }
        return listJob;
    }


   /* @RequestMapping("depJob.do")
    public String depJob(HttpSession session, Model model,@PathVariable("gid") Long gid) {

        List<JobVo> listJob=this.jobDependenceService.loadJobByGroupId(gid);
        model.addAttribute("depJobs",listJob);

        return "/jobGroup/depJob";
    }*/





}
