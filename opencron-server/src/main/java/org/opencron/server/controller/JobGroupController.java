package org.opencron.server.controller;
/**
 * @Package org.opencron.server.controller
 * @Title: JobGroupController
 * @author hapic
 * @date 2018/4/10 13:56
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;
import org.opencron.common.utils.StringUtils;
import org.opencron.server.domain.Group;
import org.opencron.server.domain.Job;
import org.opencron.server.domain.JobGroup;
import org.opencron.server.job.OpencronTools;
import org.opencron.server.service.JobGroupService;
import org.opencron.server.tag.PageBean;
import org.opencron.server.vo.JobVo;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

/**
 * @Descriptions:
 */
@Slf4j
@Controller
@RequestMapping("jobGroup")
public class JobGroupController extends BaseController {


    @Autowired
    private JobGroupService jobGroupService;


    @RequestMapping("view.htm")
    public String view(PageBean pageBean) {
        jobGroupService.getJobGroupPage(pageBean);
        return "/jobGroup/view";
    }

    @RequestMapping("edit/{groupId}.htm")
    public String edit(@PathVariable("groupId")Long groupId, Model model) {
        JobGroup group = jobGroupService.getById(groupId);
        model.addAttribute("group",group);
        return "/jobGroup/edit";
    }

    @RequestMapping("add.htm")
    public String add(Model model) {
        return "/jobGroup/add";
    }

    @RequestMapping(value = "save.do",method= RequestMethod.POST)
    public String save(HttpSession session,JobGroup group,Model model) throws SchedulerException {
        group.setCreateTime(new Date());
        group.setUserId(OpencronTools.getUserId(session));
       this.jobGroupService.merge(group);
        return "redirect:/jobGroup/view.htm?csrf=" + OpencronTools.getCSRF(session);
    }

    @RequestMapping(value = "checkname.do",method= RequestMethod.POST)
    @ResponseBody
    public boolean checkname(String name){
        if(StringUtils.isNullString(name)){
            return true;
        }
        JobGroup jobGroup = this.jobGroupService.loadGroupByName(name);
        if(jobGroup==null){
            return true;
        }
        return false;
    }
}
