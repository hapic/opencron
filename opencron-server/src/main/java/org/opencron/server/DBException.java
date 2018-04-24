package org.opencron.server;
/**
 * @Package org.opencron.server
 * @Title: DBException
 * @author hapic
 * @date 2018/4/11 16:00
 * @version V1.0
 */

import org.apache.commons.lang.StringUtils;

/**
 * @Descriptions:
 */
public class DBException {

    public static void business(Exception e,String message) {
        Throwable cause = e.getCause();
        if(cause instanceof org.hibernate.exception.ConstraintViolationException) {
            String errMsg = ((org.hibernate.exception.ConstraintViolationException)cause).getSQLException().getMessage();
            if(StringUtils.isNotBlank(errMsg) && errMsg.indexOf(message)!=-1) {
                throw new org.opencron.common.exception.DBException(e.getMessage());
            }
        }

    }
}
