package org.opencron.common.exception;
/**
 * @Package org.opencron.common.exception
 * @Title: CycleDependencyException
 * @author hapic
 * @date 2018/4/12 10:57
 * @version V1.0
 */

/**
 * @Descriptions: 循环依赖异常
 */
public class CycleDependencyException extends BasicException {

    public CycleDependencyException(String msg) {
        super(msg);
    }
}
