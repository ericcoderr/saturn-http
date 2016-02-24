package com.saturn.http.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import com.saturn.http.component.SaturnHttpRequest;

/**
 * 本类主要提供获得IP地址的方法
 */
public class HttpUtil {

    private static Set<String> localIPSet;
    private static Set<String> localIPWith127001Set;
    private static String localSampleIP;

    /**
     * 获得inetSocketAddress对应的IP地址
     * 
     * @return
     */
    public static String getIP(InetSocketAddress inetSocketAddress) {
        if (inetSocketAddress != null) {
            InetAddress addr = inetSocketAddress.getAddress();
            if (addr != null) {
                return addr.getHostAddress();
            }
        }
        return "";
    }

    public static Set<String> getLocalIPWith127001() {
        if (localIPWith127001Set == null) {
            Set<String> localIPSetTmp = new LinkedHashSet<String>(3);
            try {
                Enumeration<?> e1 = NetworkInterface.getNetworkInterfaces();
                while (e1.hasMoreElements()) {
                    NetworkInterface ni = (NetworkInterface) e1.nextElement();

                    Enumeration<?> e2 = ni.getInetAddresses();
                    while (e2.hasMoreElements()) {
                        InetAddress ia = (InetAddress) e2.nextElement();
                        if (ia instanceof Inet6Address) {
                            continue;
                        }
                        String ip = ia.getHostAddress();
                        localIPSetTmp.add(ip);
                    }
                }
            } catch (SocketException e) {
                // log.error("", e);//因为logback在初始化时就要用到此方法,所以不能使用
                e.printStackTrace();
            }
            localIPWith127001Set = localIPSetTmp;
        }
        return localIPWith127001Set;
    }

    /**
     * 获得本地IP（除去127.0.0.1之外的IP）
     */
    public static Set<String> getLocalIP() {
        if (localIPSet == null) {
            Set<String> localIPSetTmp = new LinkedHashSet<String>(3);
            localIPSetTmp.addAll(getLocalIPWith127001());
            localIPSetTmp.remove("127.0.0.1");
            localIPSet = localIPSetTmp;
        }
        return localIPSet;
    }

    /**
     * 获得本地特征IP
     * 
     * @return
     */
    public static String getLocalSampleIP() {
        if (localSampleIP == null) {
            Set<String> set = getLocalIP();
            localSampleIP = (set == null || set.size() == 0) ? "N/A" : set.iterator().next();
        }
        return localSampleIP;
    }

    /**
     * 通过domainName获得IP地址
     * 
     * @param domainName
     * @return
     */
    public static Set<String> getIPByDomainName(String domainName) {
        Set<String> domainIPSet = new LinkedHashSet<String>(2);
        try {
            InetAddress[] inets = InetAddress.getAllByName(domainName);
            for (InetAddress inetAddress : inets) {
                domainIPSet.add(inetAddress.getHostAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return domainIPSet;
    }

    public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
    public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_REAL_IP = "X-Real-IP";

    public static String getIP(SaturnHttpRequest request, String proxyHeader) {
        String proxyIp = request.getHeader(proxyHeader);
        if (proxyIp == null) {
            return request.getRemoteIp();
        }
        proxyIp = proxyIp.trim();
        if (proxyIp.isEmpty() || !proxyIp.contains(".")) {
            return request.getRemoteIp();
        }
        return proxyIp;
    }

    /**
     * X-Forwarded-For 多级反向代理，取第一个ip为真实客户端ip
     * @param request
     * @return
     */
    public static String getIPByXForwared(SaturnHttpRequest request) {
        String proxyIp = request.getHeader(X_FORWARDED_FOR);
        if (proxyIp == null) {
            return request.getRemoteIp();
        }
        proxyIp = proxyIp.trim();
        if (proxyIp.isEmpty() || !proxyIp.contains(".")) {
            return request.getRemoteIp();
        }

        int index = proxyIp.indexOf(',');
        if (index > 0) {
            proxyIp = proxyIp.substring(0, index);
        }
        return proxyIp;
    }

    public static String getIP(SaturnHttpRequest request, String... proxyHeaders) {
        for (int i = 0; i < proxyHeaders.length; i++) {
            String proxyIp = request.getHeader(proxyHeaders[i]);
            if (proxyIp == null) {
                continue;
            }

            proxyIp = proxyIp.trim();
            int index = proxyIp.indexOf(',');
            if (index > 0) {
                proxyIp = proxyIp.substring(0, index);
            }
            if (proxyIp.isEmpty() || !proxyIp.contains(".")) {
                continue;
            }
            return proxyIp;
        }
        return request.getRemoteIp();
    }

}
