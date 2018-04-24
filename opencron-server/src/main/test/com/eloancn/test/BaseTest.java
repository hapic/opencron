package com.eloancn.test;
/**
 * @Package com.eloancn.test
 * @Title: BaseTest
 * @author hapic
 * @date 2018/4/3 16:05
 * @version V1.0
 */

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @Descriptions:
 */
@ContextConfiguration(locations = { "classpath:app-place.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class BaseTest extends AbstractJUnit4SpringContextTests {
}
