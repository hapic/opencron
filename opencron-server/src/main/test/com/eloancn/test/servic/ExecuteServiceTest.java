package com.eloancn.test.servic;
/**
 * @Package com.eloancn.test.servic
 * @Title: ExecuteServiceTest
 * @author hapic
 * @date 2018/4/3 16:05
 * @version V1.0
 */

import com.eloancn.test.BaseTest;
import org.junit.Test;
import org.opencron.server.service.JobDependenceService;
import org.opencron.server.vo.JobVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @Descriptions:
 */
public class ExecuteServiceTest extends BaseTest{


    @Autowired
    private JobDependenceService jobDependenceService;


    @Test
    public void testchildsNodeJob(){

        List<JobVo> jobVos =
                jobDependenceService.childsNodeJob(37L);

        for(JobVo vo:jobVos){
            System.out.println(vo);
        }

    }



}
