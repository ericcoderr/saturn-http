/** ******NettyServerMBean.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.jmx;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import com.saturn.http.jmx.annotation.ManagedAttribute;
import com.saturn.http.jmx.annotation.ManagedObject;
import com.saturn.http.jmx.annotation.ManagedOperation;

/**
 * @describe:
 * 
 *            <pre>
 * 因为使用的是htmladapter，所以可以使用html标签
 * 譬如换行符使用:<br>
 * 此类目前只用来监控当前服务器舜时状态
 *            </pre>
 * 
 * @date :2014年2月20日 上午10:37:37
 * @author : ericcoderr@gmail.com
 */
@ManagedObject("Netty server 相关监控信息")
public class NettyServerMBean extends ObjectMBean {

    private final long startupTime;

    public NettyServerMBean(Object managedObject) {
        super(managedObject);
        startupTime = System.currentTimeMillis();
    }

    @ManagedAttribute("the startup time since January 1st, 1970 (in ms)")
    public long getStartupTime() {
        return startupTime;
    }

    @ManagedOperation("shutdown server")
    public void shutdown() {
        if (!ShutdownThread.isSystemExit()) {
            ShutdownThread.getInstance(true).start();
        }
    }

    // @formatter:off
    
    // http://www.kammerl.de/ascii/AsciiSignature.php
    // choose font: 'bright'
    // 还可以的： small
    private static final  String ASCII_BRIGHT ="saturn";
    // @formatter:on
    @ManagedOperation("server status")
    public String serverStatus() {
        StringBuilder serverStatus = new StringBuilder();
        serverStatus.append("欢迎来到Ares;<br>");
        serverStatus.append(String.format("%s<br>", ASCII_BRIGHT));
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        serverStatus.append(String.format("服务器启动时间为 : %tc .<br>", new Date(runtimeMXBean.getStartTime())));
        int hours = (int) (runtimeMXBean.getUptime() / 1000 / 3600);
        serverStatus.append(String.format("服务器启运行时长为 : %-10d %s.<br>", hours <= 0 ? runtimeMXBean.getUptime() / 1000 : hours, hours <= 0 ? "Seconds" : "Hours"));
        serverStatus.append(String.format("进程:%s<br>", runtimeMXBean.getName()));
        serverStatus.append(String.format("虚拟机:%s<br> %s", runtimeMXBean.getVmName(), runtimeMXBean.getVmVersion()));
        serverStatus.append(gcInfo());
        serverStatus.append(loadAverage());
        serverStatus.append(memoryUse());
        serverStatus.append(threadInfo());
        serverStatus.append(tps());
        return serverStatus.toString();
    }

    /**
     * <pre>
     * TODO 加上定时收集信息，保存最近半小时的信息 
     * Returns the system load average for the last minute,If the load average is not available, a negative value is returned.
     * </pre>
     * 
     * @return
     */
    private String loadAverage() {
        StringBuilder loadAverage = new StringBuilder();
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        loadAverage.append("<p>");
        loadAverage.append("Load average<br>");
        loadAverage.append(String.format("Os.name :%-10s<br>", operatingSystemMXBean.getName()));
        loadAverage.append(String.format("Os.arch :%-10s<br>", operatingSystemMXBean.getArch()));
        loadAverage.append(String.format("Cpu nums:%-10d<br>", operatingSystemMXBean.getAvailableProcessors()));
        loadAverage.append(String.format("Currtime load average:%s ,%s<br>", operatingSystemMXBean.getSystemLoadAverage(), "如果为负值,表示此平台无法获取"));
        loadAverage.append("Load average <br>");
        loadAverage.append("</p>");
        return loadAverage.toString();
    }

    /**
     * 服务器线程状态信息
     * 
     * @return
     */
    private String threadInfo() {
        StringBuilder thread = new StringBuilder();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        thread.append("<p>");
        thread.append("Thread info<br>");
        thread.append(String.format(
                "Live threads(daemon+ non daemon ):%-10d,live daemon threads:%-10d,live non daemon threads:%-10d, dead locked threads:%-10d,started threads:%-10d,peak threads:%-10d <br>",
                threadMXBean.getThreadCount(), threadMXBean.getDaemonThreadCount(), threadMXBean.getThreadCount() - threadMXBean.getDaemonThreadCount(),
                threadMXBean.findDeadlockedThreads() == null ? 0 : threadMXBean.findDeadlockedThreads(), threadMXBean.getTotalStartedThreadCount(), threadMXBean.getPeakThreadCount()));
        thread.append("Thread info<br>");
        thread.append("</p>");
        return thread.toString();
    }

    /**
     * gc 信息
     * 
     * @return
     */
    private String gcInfo() {
        StringBuilder gcInfo = new StringBuilder();
        gcInfo.append("<p>");
        gcInfo.append("gc info <br>");
        List<GarbageCollectorMXBean> list = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean garbage : list) {
            gcInfo.append(String.format("name:%-10s,collectionTime(毫秒):%-10d,collectionCounts:%-10d<br>", garbage.getName(), garbage.getCollectionTime(), garbage.getCollectionCount()));
        }
        gcInfo.append("gc info <br>");
        gcInfo.append("</p>");
        return gcInfo.toString();
    }

    private String memoryUse() {
        StringBuilder memory = new StringBuilder();
        MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = mx.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = mx.getNonHeapMemoryUsage();
        memory.append("<p>");
        memory.append("Memory <br>");
        memory.append(String.format("Memory ,Max:%-10s;Init:%-10s;Used:%-10s<br>", SystemUtil.byte2MB(Runtime.getRuntime().maxMemory()), SystemUtil.byte2MB(Runtime.getRuntime().totalMemory()),
                SystemUtil.byte2MB(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));
        memory.append(String.format("Heap usage,init:%-10s;Used:%-10s;Max:%-10s<br>", SystemUtil.byte2MB(heapUsage.getInit()), SystemUtil.byte2MB(heapUsage.getUsed()),
                SystemUtil.byte2MB(heapUsage.getMax())));
        memory.append(String.format("NonHeap usage,init:%-10s;Used:%-10s;Max:%-10s,%s<br>", SystemUtil.byte2MB(nonHeapUsage.getInit()), SystemUtil.byte2MB(nonHeapUsage.getUsed()),
                SystemUtil.byte2MB(nonHeapUsage.getMax()), "如果max为负值,表示没有定义最大内存大小"));
        memory.append(String.format("Wait recycle Object nums:%-10d <br>", mx.getObjectPendingFinalizationCount()));
        memory.append("Memory  <br>");
        memory.append("</p>");
        return memory.toString();
    }

    /**
     * @return
     */
    public String tps() {
        StringBuilder tps = new StringBuilder();
        tps.append("<p>");
        tps.append("Tps <br>");
        tps.append(String.format("Channel open times: %-10s<br>", StatisUtil.getChannelOpenTimes()));
        tps.append(String.format("Channel close times: %-10s<br>", StatisUtil.getChannelCloseTimes()));
        tps.append(String.format("%-10s<br>", "服务器请求接口统计============="));
        tps.append(String.format(
                "%s&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%s<br>",
                "地址", "请求次数"));
        Map<String, AtomicLong> pathOpenTimes = StatisUtil.getPathOpenTimes();
        for (Entry<String, AtomicLong> entry : pathOpenTimes.entrySet()) {
            tps.append(String.format(
                    "%s&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;%-10d<br>",
                    entry.getKey(), entry.getValue().get()));
        }
        tps.append(String.format("%-10s<br>", "服务器请求接口统计============="));
        tps.append(String.format("%-10s<br>", "服务器最近tps数据============="));
        tps.append(StatisUtil.getStatisUtil().getSnapshotListStr(10));
        tps.append("Tps <br>");
        tps.append("</p>");
        return tps.toString();
    }
}
