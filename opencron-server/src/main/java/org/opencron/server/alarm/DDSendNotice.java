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
@Component("ddSendNotice")
public class DDSendNotice implements SendNotice {


    private RecordVo record;
    String token="63b9b07ea3e4eaadd49efdf681610958435e6e011303a08e99ce8e754e64d108";
    String url="http://frcsms.eloancn.com/SendSMS/sendMsgByHs?access_token="+token;

    @Override
    public boolean send(String accept, String msg) {
        JSONObject jsonObject= new JSONObject();

        jsonObject.put("content",msg);
        jsonObject.put("sendWay","0");
        jsonObject.put("source","WARNING_FIXME");
        try {
            sendPost(url,jsonObject.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }


        return false;
    }

    private String sendPost(String url,String msg)throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        httppost.addHeader("Content-Type", "application/json; charset=utf-8");

        String result = "";
        StringEntity se = new StringEntity(msg, "utf-8");
        httppost.setEntity(se);
        HttpResponse response = httpclient.execute(httppost);
        if (response.getStatusLine().getStatusCode()== HttpStatus.SC_OK){
            result= EntityUtils.toString(response.getEntity(), "utf-8");
            System.out.println(result);
        }
        return result;
    }


    public static void main(String[] args) {
        DDSendNotice dd= new DDSendNotice();
        dd.send("","test");


    }

}
