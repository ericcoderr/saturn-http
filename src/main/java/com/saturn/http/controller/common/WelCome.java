/** ******WelCome.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.controller.common;

import java.util.Date;
import org.springframework.stereotype.Controller;
import com.saturn.http.controller.annotation.ReqMapping;

/**
 * @describe: <pre>
 * </pre>
 * @date :2014年7月31日 下午5:04:10
 * @author : ericcoderr@gmail.com
 */
@Controller
public class WelCome {

    // @formatter:off
    
    // http://www.kammerl.de/ascii/AsciiSignature.php
    // choose font: 'bright'
    // 还可以的： small
    private static final  String ASCII_BRIGHT ="\r\n"     
            +" ======================================\r\n"
            +"    . ####    . . ##### . . . ###### . . .####.. \r\n"
            +"   .## . . ## . . ## . . ## . .## ......    ## ..... \r\n"
            +"   .###.### . . ##### . .  ###### . . . ####.. \r\n"
            +"   .## . .## . . ## . . ## . .## ..........         ##. \r\n"
            +"   .## . .## . . ## . . ## . .###### ...####.. \r\n";
    // @formatter:on

    @ReqMapping("/welcome")
    public String welcome() {
        StringBuilder sb = new StringBuilder();
        sb.append("Welcome ares\n,");
        sb.append(String.format("当前服务器时间为:%s", new Date()));
        sb.append("\n");
        sb.append("Ares 是一个基于Netty5 的http服务器。\n");
        sb.append(ASCII_BRIGHT);
        return sb.toString();
    }
}
