/** ******IpAuthInterceptor.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.util.ipauth;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ResourceBundle;
import com.saturn.http.annotation.IpAuth;
import com.saturn.http.component.SaturnHttpRequest;
import com.saturn.http.exception.IpAuthError;
import com.saturn.util.ResourceBundleUtil;
import com.saturn.util.StringTools;

/**
 * @describe: <pre>
 * ip过滤拦截器
 * </pre>
 * @date :2014年12月30日 下午2:35:56
 * @author : ericcoderr@gmail.com
 */
public class IpAuthUtil {

    public static String[] ipWhiteList() {
        ResourceBundle rb = ResourceBundleUtil.reload("ipAuth");
        if (rb == null) {
            return new String[] {};
        }
        String whiteList = ResourceBundleUtil.getString(rb, "WHITE_LIST");
        if (whiteList == null) {
            return new String[] {};
        }
        return StringTools.splitAndTrimAsArray(whiteList, ",");
    }

    /**
     * <pre>
     * 此方法只有在过滤所有http请求时才使用,IpAuth里面的白名单ip列表可以直接设置也可以通过
     * ipAuth.properties文件读取，优先使用设置的值
     * </pre>
     * 
     * @param request
     * @param m
     */
    public static void ipAuth(SaturnHttpRequest request, Method m) {
        if (m != null) {
            IpAuth ipAuth = m.getDeclaredAnnotation(IpAuth.class);
            if (ipAuth == null) {
                return;
            }
            String[] ips = ipAuth.value();
            if (ips.length == 0) {
                ips = ipWhiteList();
            }
            if (!Arrays.asList(ips).contains(request.getRemoteIp())) {
                throw IpAuthError.INSTANCE;
            }
        }
    }

    public static void ipAuth(SaturnHttpRequest request) {
        String[] whiteList = ipWhiteList();
        if (!Arrays.asList(whiteList).contains(request.getRemoteIp())) {
            throw IpAuthError.INSTANCE;
        }
    }
}
