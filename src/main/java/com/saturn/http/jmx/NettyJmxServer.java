/** ******NettyJmxAgent.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.jmx;

import java.rmi.registry.LocateRegistry;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import com.saturn.http.util.HttpServerConfig;
import com.sun.jdmk.comm.HtmlAdaptorServer;

/**
 * @describe: <pre>
 * </pre>
 * @date :2014年2月20日 上午10:44:54
 * @author : ericcoderr@gmail.com
 */
public class NettyJmxServer {

    private static final String JMX_NAME = "NettyJmx";

    /**
     * 绑定要管理的服务
     */
    public static void bind() throws Exception {
        int jmxPort = HttpServerConfig.HTTP_SERVER_CONFIG.getJmxPort();
        LocateRegistry.createRegistry(jmxPort);
        MBeanServer mbs = MBeanServerFactory.createMBeanServer(JMX_NAME);

        HtmlAdaptorServer adapter = new HtmlAdaptorServer();
        ObjectName adapterName = new ObjectName(JMX_NAME + ":name=" + "htmladapter");
        adapter.setPort(HttpServerConfig.HTTP_SERVER_CONFIG.getJmxHttpPort());
        adapter.start();
        mbs.registerMBean(adapter, adapterName);

        ObjectName objName = new ObjectName(JMX_NAME + ":name=" + "NettyServerMBean");
        mbs.registerMBean(new ObjectMBean(new NettyServerMBean(objName)), objName);

        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:" + jmxPort + "/" + JMX_NAME);
        JMXConnectorServer jmxConnServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        jmxConnServer.start();
    }
}
