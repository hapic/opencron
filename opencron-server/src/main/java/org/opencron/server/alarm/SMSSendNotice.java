package org.opencron.server.alarm;
/**
 * @Package org.opencron.server.alarm
 * @Title: DDSendNotice
 * @author hapic
 * @date 2018/5/8 11:28
 * @version V1.0
 */

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.opencron.server.vo.RecordVo;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Descriptions:
 */
//@Component("smsSendNotice")
public class SMSSendNotice implements SendNotice {


    @Override
    public boolean send(String accept, String msg) {

        return false;
    }
}
