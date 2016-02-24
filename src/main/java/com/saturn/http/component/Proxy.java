/** ******Proxy.java*****/
/**
 *Copyright
 *
**/
package com.saturn.http.component;

import java.lang.reflect.Method;

/**
 * @describe:
 * 
 *            <pre>
 *代理接口，用来处理类似登陆态校验内容
 *            </pre>
 * 
 * @date :2015年10月14日 下午6:10:31
 * @author : eric
 */
public interface Proxy {

    public void proxy(Method m, SaturnHttpRequest request, SaturnHttpResponse response);
}
