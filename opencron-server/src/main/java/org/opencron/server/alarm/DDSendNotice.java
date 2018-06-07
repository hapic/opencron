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

//    https://oapi.dingtalk.com/robot/send?access_token=
    private RecordVo record;
    String url="http://frcsms.eloancn.com/SendSMS/sendMsgByHs?access_token=";
    StringBuffer sbUrl= new StringBuffer(url);

    @Override
    public boolean send(String accept, String msg) {

        JSONObject jsonObject= new JSONObject();

        jsonObject.put("content",msg);
        jsonObject.put("sendWay","0");
        jsonObject.put("source","WARNING_FIXME");
        try {
            sendPost(sbUrl.toString(),jsonObject.toJSONString());
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
    public void setToken(String token) {
        sbUrl.append(token);
    }

    public void changeToken(String token){
        int i = sbUrl.lastIndexOf("=");
        String substring = sbUrl.substring(0, i+1);
        sbUrl=new StringBuffer(substring).append(token);
    }

    public static void main(String[] args) {
        DDSendNotice ddSendNotice= new DDSendNotice();
        ddSendNotice.setToken("cffcc6996a57a834a8bde29b79f4ae77103eb57fa1ce37f2743e1867be03dd87");
        System.out.println(ddSendNotice.sbUrl.toString());
        ddSendNotice.send(null,"test");
        ddSendNotice.changeToken("b4538d0fe797dc828727eb896d96cd6744c31622a110118ae00c8a883d69bbf8");
        ddSendNotice.send(null,"test");
        System.out.println(ddSendNotice.sbUrl.toString());
    }
}
