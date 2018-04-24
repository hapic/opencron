package org.opencron.server;
/**
 * @Package org.opencron.server
 * @Title: ConcurrencyControlTest
 * @author hapic
 * @date 2018/4/8 11:01
 * @version V1.0
 */

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opencron.common.utils.CommonUtils;
import org.opencron.server.controller.JobController;
import org.opencron.server.domain.Job;
import org.opencron.server.service.ConcurrencyControl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Descriptions:
 */
public class ConcurrencyControlTest {



    @Test
    public void testMd5(){
        String code =DigestUtils.md5Hex("111111").toLowerCase();
        System.out.println(code);





    }


    @Test
    public void testDif(){

        Job job1= new Job();
        job1.setJobId(1L);

        Job job2= new Job();
        job2.setJobId(2L);

        Job job3= new Job();
        job3.setJobId(3L);

        Job job4= new Job();
        job4.setJobId(2L);

        Job job5= new Job();
        job5.setJobId(4L);


        List<Job> jobList= new ArrayList<>();
        jobList.add(job1);
        jobList.add(job2);


        List<Job> jobList2= new ArrayList<>();
        jobList2.add(job4);
        jobList2.add(job3);
        jobList2.add(job5);

//        jobList.removeAll(jobList2);
//
//
//        Collection disjunction = CollectionUtils.disjunction(jobList, jobList2);
        JobController controller= new JobController();
        List<Job> jobs = controller.differenceSet(jobList, jobList2);
        System.out.println(jobs);







    }



    public static void main(String[] args) {

        ConcurrencyControl cc=new ConcurrencyControl();

        boolean aContinue = cc.isContinue(19);

        Assert.assertTrue(aContinue);
    }
}
