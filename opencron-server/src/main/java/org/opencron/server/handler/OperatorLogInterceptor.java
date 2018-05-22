package org.opencron.server.handler;
/**
 * @Package org.opencron.server.handler
 * @Title: OperatorLogInterceptor
 * @author hapic
 * @date 2018/5/21 16:33
 * @version V1.0
 */

import lombok.extern.slf4j.Slf4j;
import org.opencron.server.domain.User;
import org.opencron.server.job.OpencronTools;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Descriptions: 操作日志记录
 */
@Slf4j
public class OperatorLogInterceptor extends HandlerInterceptorAdapter {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        User user = OpencronTools.getUser(request.getSession());
        request.getRemoteHost();
        String remoteAddr = request.getRemoteAddr();
        log.info("user:{} uri:{} from:{}",user==null?null:user.getUserName(),remoteAddr,requestURI);

        return super.preHandle(request, response, handler);
    }
}
