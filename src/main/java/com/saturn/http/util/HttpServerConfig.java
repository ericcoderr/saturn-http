/** ******HttpServerConfig.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.util;

import java.util.ResourceBundle;
import com.saturn.util.ResourceBundleUtil;
import com.saturn.util.StringTools;

/**
 * @describe:
 * 
 *            <pre>
 *            </pre>
 * 
 * @date :2013年7月18日 下午8:43:50
 * @author : ericcoderr@gmail.com
 */
public class HttpServerConfig {

    public static final HttpServerConfig HTTP_SERVER_CONFIG = new HttpServerConfig();

    private HttpServerConfig() {
        initConfig();
    }

    public static final int CORE_PROCESSOR_NUM = Runtime.getRuntime().availableProcessors();
    private static int workerCount; // WorkerCount 不应该超过cpu个数太多，不然线程间的调度开销也会很大
    private boolean isUseJmx = false; // 是否开启Jmx使用

    // 是否使用Aggregator
    private boolean isAggregator = false;

    public static final String DEFAULT_METHOD = "process"; // 默认方法

    public static int getWorkerCount() {
        if (workerCount < 0) {
            throw new IllegalArgumentException("Usge : worker count is not < 0");
        } else if (workerCount == 0) {
            return CORE_PROCESSOR_NUM * 2; // cpu 个数 * 2 ,如果cpu密集型,建议cpu个数+1,如果是类似数据库操作，时间够长，应该尽量大
        } else
            return workerCount;
    }

    public static String getDefaultMethod() {
        return "foward";
    }

    public boolean isUseJmx() {
        return isUseJmx;
    }

    public boolean isAggregator() {
        return isAggregator;
    }

    private int listen_port;
    private int connectTimeoutMillis;
    private int receiveBufferSize;
    private int sendBufferSize;

    private int jmxHttpPort;
    private int jmxPort;

    private String uploadBaseDir = "";

    public void initConfig() {
        ResourceBundle rb = ResourceBundleUtil.reload("server");
        if (rb == null) {
            throw new RuntimeException("server.properties file in classpath is a must.");
        }
        String listen_portStr = ResourceBundleUtil.getString(rb, "listen_port");
        if (StringTools.isEmpty(listen_portStr)) {
            throw new RuntimeException("Please set port.[listen_port]");
        }
        listen_port = Integer.parseInt(listen_portStr);
        String connectTimeoutMillisStr = ResourceBundleUtil.getString(rb, "connectTimeoutMillis");
        connectTimeoutMillis = StringTools.isEmpty(connectTimeoutMillisStr) ? 5000 : Integer.parseInt(connectTimeoutMillisStr);

        String receiveBufferSizeStr = ResourceBundleUtil.getString(rb, "receiveBufferSize");
        receiveBufferSize = StringTools.isEmpty(receiveBufferSizeStr) ? 8192 : Integer.parseInt(receiveBufferSizeStr);

        String sendBufferSizeStr = ResourceBundleUtil.getString(rb, "sendBufferSize");
        sendBufferSize = StringTools.isEmpty(sendBufferSizeStr) ? 8192 : Integer.parseInt(rb.getString("sendBufferSize"));

        Object isAggregator = ResourceBundleUtil.getObject(rb, "isAggregator");
        if (isAggregator != null && Boolean.valueOf(isAggregator.toString())) {
            this.isAggregator = true;
        }

        uploadBaseDir = ResourceBundleUtil.getString(rb, "uploadBaseDir");
        uploadBaseDir = StringTools.isEmpty(uploadBaseDir) ? "" : uploadBaseDir;

        // 设置jmx参数
        Object isUseJmx = ResourceBundleUtil.getObject(rb, "isUseJmx");
        this.isUseJmx = (isUseJmx == null) ? false : Boolean.valueOf(isUseJmx.toString());
        if (this.isUseJmx) {
            String jmxHttpPortStr = ResourceBundleUtil.getString(rb, "jmxHttpPort");
            jmxHttpPort = StringTools.isEmpty(jmxHttpPortStr) ? 9090 : Integer.parseInt(jmxHttpPortStr);
            String jmxPortStr = ResourceBundleUtil.getString(rb, "jmxPort");
            jmxPort = StringTools.isEmpty(jmxPortStr) ? 1191 : Integer.parseInt(jmxPortStr);
        }

    }

    public int getListenPort() {
        return listen_port;
    }

    public int getJmxHttpPort() {
        return jmxHttpPort;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    public int getConnecttimeoutmillis() {
        return connectTimeoutMillis;
    }

    public int getReceivebuffersize() {
        return receiveBufferSize;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void reset() {
        ResourceBundle rb = ResourceBundleUtil.reload("server");
        Object isUseJmx = ResourceBundleUtil.getObject(rb, "isUseJmx");
        this.isUseJmx = (isUseJmx == null) ? false : Boolean.valueOf(isUseJmx.toString());
    }

    /**
     * 通过server.properties 配置文件获取代理类的实现类
     * 
     * @return
     */
    public String getProxyImplName() {
        ResourceBundle rb = ResourceBundleUtil.reload("server");
        String proxyName = ResourceBundleUtil.getString(rb, "proxyName");
        return proxyName;
    }

    /**
     * 上传文件根目录
     * 
     * @return
     */
    public String getUploadBaseDir() {
        return uploadBaseDir;
    }
}
