/** ******JmxUtil.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.jmx;


/**
 * @describe: <pre>
 * 一些监控工具类
 * </pre>
 * @date :2014年8月1日 上午10:48:24
 * @author : ericcoderr@gmail.com
 */
public class SystemUtil {

    private static final double KBYTE = 1024;
    private static final double MB = KBYTE * 1024;
    private static final double GB = MB * 1024;

    /**
     * 将byte 转换为mb
     * 
     * @return
     */
    public  static String byte2MB(long bytes) {
        return String.format("%f MB.", bytes / MB);
    }

    /**
     * 将byte 转换为GB
     * 
     * @return
     */
    public static String byte2GB(long bytes) {
        return String.format("%f MB.", bytes / GB);
    }
}
