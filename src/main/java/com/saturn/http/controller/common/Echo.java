/** ******Echo.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.controller.common;

import java.util.Date;
import org.springframework.stereotype.Controller;
import com.saturn.http.component.SaturnHttpRequest;
import com.saturn.http.controller.annotation.ReqMapping;

/**
 * @describe: <pre>
 * </pre>
 * @date :2013年7月19日 下午2:57:19
 * @author : ericcoderr@gmail.com
 */
@Controller
public class Echo {

    @ReqMapping({ "/echo" })
    public Object echo() throws Exception {
        return String.format("当前服务器时间为:%s", new Date());
    }

    @ReqMapping({ "/echoparam" })
    public Object echoParam(String id, int num, boolean isB, long l, SaturnHttpRequest request) throws Exception {
        // IpAuthUtil.ipAuth(request);
        return String.format("当前输入参数为:%s - %d %s - %d", id, num, isB, l);
    }
}
