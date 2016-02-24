/** ******StatisUtil.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.jmx;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.saturn.util.concurrent.ConcurrentUtil;

/**
 * @describe: <pre>
 * tps 统计util
 * </pre>
 * @date :2014年8月4日 上午11:36:49
 * @author : ericcoderr@gmail.com
 */
public class StatisUtil {

    private static final StatisUtil STATISUTIL = new StatisUtil();

    private StatisUtil() {
        tps();
    }

    // 通道打开次数
    private static AtomicLong channelOpenTimes = new AtomicLong(0);

    // 通道关闭次数
    private static AtomicLong channelCloseTimes = new AtomicLong(0);

    // 通道中断次数
    private static AtomicLong channelInterruptTimes = new AtomicLong(0);

    // 请求地址打开次数
    private static Map<String, AtomicLong> pathOpenTimes = new ConcurrentHashMap<String, AtomicLong>();

    // 业务处理，这里记录成功调用了writeResponse()接口的请求
    private static AtomicLong businessProcessTimes = new AtomicLong(0);

    public static void incrChannelOpen() {
        channelOpenTimes.incrementAndGet();
    }

    public static void incrChannelClose() {
        channelCloseTimes.incrementAndGet();
    }

    public static void incrChannelInterrupt() {
        channelInterruptTimes.incrementAndGet();
    }

    public static void incrBusinessProcessTimes() {
        businessProcessTimes.incrementAndGet();
    }

    public static AtomicLong getChannelOpenTimes() {
        return channelOpenTimes;
    }

    public static AtomicLong getChannelCloseTimes() {
        return channelCloseTimes;
    }

    public static AtomicLong getChannelInterruptTimes() {
        return channelInterruptTimes;
    }

    public static void incrOpenNumsByPath(String path) {
        synchronized (path.intern()) {
            AtomicLong times = pathOpenTimes.get(path);
            if (times == null) {
                times = new AtomicLong(1);
                pathOpenTimes.put(path, times);
            } else {
                times.incrementAndGet();
                pathOpenTimes.put(path, times);
            }
        }
    }

    public static Map<String, AtomicLong> getPathOpenTimes() {
        return pathOpenTimes;
    }

    /**
     * 简单tps 统计，指定时间段内每分钟处理请求完成的次数
     */
    // 最大tps
    private Snapshot maxTps;

    // 保存最近一次的tps
    private Snapshot lastTps;

    // 每次统计时间间隔
    private static final long statisTpsPeriodMilliseconds = 5 * 60 * 1000;

    // 缓存快照数量
    private static final int snapshotSize = (int) (30 * 24 * 3600 * 1000l / statisTpsPeriodMilliseconds);

    // 快照内存缓存记录
    private final Snapshot[] snapshot = new Snapshot[snapshotSize];

    // 当前数组位置
    private static final AtomicInteger index = new AtomicInteger(0);

    private synchronized void tps() {

        ConcurrentUtil.getDaemonExecutor().scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                System.err.println("TPS...");
                AtomicLong _tmp = new AtomicLong(businessProcessTimes.get());
                LocalDateTime now = LocalDateTime.now();
                snapshot[index.get()] = new Snapshot(_tmp, now);
                if (index.get() >= snapshotSize && lastTps != null) {
                    lastTps = snapshot[index.get() - 1];
                    index.set(0);
                } else if (index.get() - 1 < 0 && lastTps == null) { // 第一次进来
                    lastTps = snapshot[0];
                    lastTps.setTps(0);
                } else {
                    lastTps = snapshot[index.get() - 1];
                }
                calcTps(lastTps, snapshot[index.get()]);
                index.incrementAndGet();
            }
        }, getSnapshotDelay(), statisTpsPeriodMilliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * 计算tps
     * 
     * @param snapshot0 数组里面前一个位置
     * @param snapshot1 数组里面下一个位置
     * @return
     */
    private void calcTps(Snapshot lastTps, Snapshot snapshot1) {
        if (maxTps == null) {
            maxTps = lastTps;
        } else if (lastTps.getTps() > maxTps.getTps()) {
            maxTps = lastTps;
        }
        long tps = (snapshot1.getCurrTimes().longValue() - lastTps.getCurrTimes().longValue()) / (statisTpsPeriodMilliseconds / 1000);
        snapshot1.setTps(tps);
    }

    /**
     * 近似系统时间,分钟数整点延迟
     * 
     * @return
     */
    private long getSnapshotDelay() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        Date date = Date.from(now.toInstant(ZoneOffset.from(ZonedDateTime.now())));
        return date.getTime() + statisTpsPeriodMilliseconds - Clock.systemDefaultZone().millis();
    }

    /**
     * 获取最近 size条 tps数据
     * 
     * @param size
     * @return
     */
    private List<Snapshot> getSnapshotList(int size) {
        List<Snapshot> list = new ArrayList<Snapshot>();
        int currentIndex = index.get();

        // 不考虑 刚给数组snapshot[currentIndex] 赋值完成，还没调用 index.incrementAndGet() 的情况
        if (currentIndex <= size - 1) {
            for (int i = currentIndex - 1; i >= 0; i--) {
                list.add(snapshot[i]);
            }
            for (int i = 0; i < size - currentIndex; i++) {
                Snapshot _tmp = snapshot[snapshot.length - 1 - i];
                if (_tmp == null) {
                    continue;
                }

                list.add(snapshot[snapshot.length - 1 - i]);
            }
        } else {
            for (int i = currentIndex - (size - 1) - 1; i < currentIndex; i++) {
                list.add(snapshot[i]);
            }
        }
        return list;
    }

    public String getSnapshotListStr(int size) {
        List<Snapshot> list = getSnapshotList(size);
        StringBuilder sb = new StringBuilder();
        for (Snapshot snapshot : list) {
            sb.append(snapshot.toString()).append("<br>");
        }
        return sb.toString();
    }

    /**
     * 快照类
     */
    public class Snapshot {

        // 当前处理业务总次数
        private AtomicLong currTimes;

        // 当前统计时间
        private LocalDateTime statisTime;

        private long tps;

        public Snapshot(AtomicLong currTimes, LocalDateTime statisTime) {
            this.currTimes = currTimes;
            this.statisTime = statisTime;
        }

        public AtomicLong getCurrTimes() {
            return currTimes;
        }

        public LocalDateTime getStatisTime() {
            return statisTime;
        }

        public long getTps() {
            return tps;
        }

        public void setTps(long tps) {
            this.tps = tps;
        }

        @Override
        public String toString() {
            return "currTimes=" + currTimes + ", statisTime=" + statisTime.toString() + ", tps=" + tps;
        }
    }

    public static StatisUtil getStatisUtil() {
        return STATISUTIL;
    }
}
