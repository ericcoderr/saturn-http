/** ******AresServerBootStrap.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.saturn.http.component.AbstractDispatcher;
import com.saturn.http.component.Dispatcher;
import com.saturn.http.jmx.NettyJmxServer;
import com.saturn.http.jmx.ShutdownThread;
import com.saturn.http.jmx.StatisUtil;
import com.saturn.http.util.HttpServerConfig;
import com.saturn.http.util.NetUtil;
import com.saturn.util.StringHelper;
import com.saturn.util.concurrent.NamedThreadFactory;

/**
 * @describe:
 * 
 *            <pre>
 *            Server启动入口类
 *            </pre>
 * 
 * @date :2013年7月19日 上午10:59:12
 * @author : ericcoderr@gmail.com
 */
public class SaturnServerBootstrap {

    public static ApplicationContext context = null;

    // @formatter:off
    
    // http://www.kammerl.de/ascii/AsciiSignature.php
    // choose font: 'bright'
    // 还可以的： small
    private static final  String ASCII_BRIGHT ="saturn";

    // @formatter:on

    public static void main(String[] args, Runnable initRunnable, AbstractDispatcher _dispatcher, String... springConfigLocations) throws Exception {

        // 服务端的时候实例化了2个EventLoopGroup，1个EventLoopGroup实际就是一个EventLoop线程组，负责管理EventLoop的申请和释放。
        // EventLoopGroup管理的线程数可以通过构造函数设置，如果没有设置，默认取-Dio.netty.eventLoopThreads，如果该系统参数也没有指定，则为可用的CPU内核数 × 2。
        // bossGroup线程组实际就是Acceptor线程池，负责处理客户端的TCP连接请求，如果系统只有一个服务端端口需要监听，则建议bossGroup线程组线程数设置为1。
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, Executors.newCachedThreadPool(new NamedThreadFactory("New I/O server boss $", Thread.MAX_PRIORITY)));
        EventLoopGroup workerGroup = new NioEventLoopGroup(HttpServerConfig.getWorkerCount(), Executors.newCachedThreadPool(new NamedThreadFactory("New I/O woker", Thread.NORM_PRIORITY)));
        // EventLoopGroup bossGroup = new NioEventLoopGroup();
        // EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            context = new ClassPathXmlApplicationContext(springConfigLocations);
            if (_dispatcher == null) {
                _dispatcher = new Dispatcher();
            }

            // 初始化url MAP
            _dispatcher.init();
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
            _Bootstrap _bootstrap = new SaturnServerBootstrap().new _Bootstrap(_dispatcher);
            if (isArgWhat(args, "stop", "shutdown")) {
                ShutdownThread.getInstance().start();
            }
            _bootstrap.start(b, initRunnable);
        } finally {

            // Shut down all event loops to terminate all threads.
            ShutdownThread.register(bossGroup, workerGroup);
        }
    }

    public static void main(String[] args, Runnable initRunnable, String... springConfigLocations) throws Exception {
        main(args, initRunnable, null, springConfigLocations);
    }

    public static boolean isArgWhat(String[] args, String... what) {
        if (args == null || args.length <= 0) {
            return false;
        }
        String arg = args[0].toLowerCase();
        for (String w : what) {
            if (arg.indexOf(w) >= 0)
                return true;
        }
        return false;
    }

    /**
     * 内部启动类
     */
    class _Bootstrap {

        private AbstractDispatcher _dispatcher;

        _Bootstrap(AbstractDispatcher _dispatcher) {
            this._dispatcher = _dispatcher;
        }

        private void start(ServerBootstrap b, Runnable initRunnable) {
            int port = HttpServerConfig.HTTP_SERVER_CONFIG.getListenPort();
            long span = 0;
            try {

                NetUtil.checkSocketPortBind(port);
                // 添加默认日志管理器
                // b.handler(new LoggingHandler(Log.class, LogLevel.DEBUG));
                _dispatcher.setAggregator(HttpServerConfig.HTTP_SERVER_CONFIG.isAggregator());

                b.childHandler(new SaturnServerInitializer(HttpServerConfig.HTTP_SERVER_CONFIG.isAggregator(), _dispatcher));

                // Start the server.
                Channel ch = b.bind(port).sync().channel();

                // 设置OPTION
                ChannelConfig config = ch.config();
                OptionConfig.setBootstrapOptions(config);

                // 是否使用JMX监控
                if (HttpServerConfig.HTTP_SERVER_CONFIG.isUseJmx()) {
                    initJmx();
                }

                if (initRunnable != null) {
                    initRunnable.run();
                }
                span = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime();
                System.out.printf("%s\n %s\n %s\n", ASCII_BRIGHT, "服务器启动完毕.(port[" + port + "])", "[Span :" + span + "MS]");

                // Wait until the server socket is closed.
                ch.closeFuture().sync();
            } catch (Throwable e) {
                span = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime();
                System.err.printf("%s\n %s\n %s\n %s\n", ASCII_BRIGHT, "服务器启动失败.(port[" + port + "])", "[Span :" + span + "MS]", StringHelper.printThrowable(e).toString());
            }
        }

        public void initJmx() throws Exception {
            System.err.println("JMX server 初始化启动... \r\n");
            new Thread() {

                @Override
                public void run() {
                    try {
                        NettyJmxServer.bind();
                        StatisUtil.getStatisUtil();
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
            }.start();
        }

        public void stop() {
        }

        public boolean isStop() {
            return false;
        }
    }
}
