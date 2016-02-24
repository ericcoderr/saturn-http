/** ******ThriftGetIpHandler.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.util.thrift;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @describe: <pre>
 * </pre>
 * @date :2015年5月27日 下午4:02:13
 * @author : ericcoderr@gmail.com
 */
public class ThriftGetIpHelper {

    private static final Map<String, String> CLIENT_IP = new ConcurrentHashMap<String, String>();

    public static String getIp(String threadId) {
        return CLIENT_IP.get(threadId);
    }

    public static void setIp(String threadId, String ip) {
        CLIENT_IP.put(threadId, ip);
    }

    public static void removeIp(String threadId) {
        CLIENT_IP.remove(threadId);
    }

}
