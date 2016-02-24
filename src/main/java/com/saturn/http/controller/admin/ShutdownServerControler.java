/** ******ShutdownServerControler.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.controller.admin;

import org.springframework.stereotype.Controller;
import com.saturn.http.component.SaturnHttpRequest;
import com.saturn.http.component.SaturnHttpResponse;
import com.saturn.http.controller.annotation.ReqMapping;
import com.saturn.http.jmx.ShutdownThread;

/**
 * @describe: <pre>
 * 关闭server,此类需要权限验证
 * </pre>
 * @date :2014年2月19日 上午11:22:34
 * @author : ericcoderr@gmail.com
 */
@Controller
public class ShutdownServerControler {

    @ReqMapping("/admin/shutdown")
    public Object shutdown(SaturnHttpRequest request, SaturnHttpResponse response) {
        ShutdownThread.getInstance().start();
        return 0;
    }
}
