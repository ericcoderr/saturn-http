/** ******OptionConfig.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http;

import com.saturn.http.util.HttpServerConfig;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOption;

/**
 * @describe: <pre>
 * 默认值
 * WRITE_SPIN_COUNT=16, CONNECT_TIMEOUT_MILLIS=30000, 
 * WRITE_BUFFER_LOW_WATER_MARK=32768,
 *  SO_BACKLOG=3072,
 *   SO_RCVBUF=65536,
 *    ALLOCATOR=UnpooledByteBufAllocator(directByDefault: true),
 *     SO_REUSEADDR=false, 
 *     MESSAGE_SIZE_ESTIMATOR=io.netty.channel.DefaultMessageSizeEstimator@5c8504fd,
 *      AUTO_READ=true, MAX_MESSAGES_PER_READ=16, WRITE_BUFFER_HIGH_WATER_MARK=65536
 * </pre>
 * @date :2013年8月16日 上午9:43:44
 * @author : ericcoderr@gmail.com
 */
public class OptionConfig {

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = HttpServerConfig.HTTP_SERVER_CONFIG.getConnecttimeoutmillis();
    private static final int DEFAULT_SO_RCVBUF = HttpServerConfig.HTTP_SERVER_CONFIG.getReceivebuffersize();
    private static final int DEFAULT_SO_SNDBUF = HttpServerConfig.HTTP_SERVER_CONFIG.getSendBufferSize();
    private static final boolean DEFAULT_CONFIG_REUSEADDRESS = true;
    private static final int DEFAULT_CONFIG_SOLINGER = -1;
    private static final boolean DEFAULT_SO_KEEPALIVE = true;
    private static final boolean DEFAULT_TCP_NODELAY = true;

    public static void setBootstrapOptions(ChannelConfig config) {
        config.setOption(ChannelOption.TCP_NODELAY, DEFAULT_TCP_NODELAY); // 客户端是否组装大包发送，为true则不组装成大包发送，收到东西马上发出（Nagle化在这里的含义是采用Nagle算法把较小的包组装为更大的帧）

        /**
         * 在默认情况下，当调用close方法后，将立即返回；如果这时仍然有未被送出的数据包，那么这些数据包将被丢弃。如果将linger参数设为一个正整数n时（n的值最大是65，535），在调用close方法后，将最多被阻塞n秒。在这n秒内，系统将尽量将未送出的数据包发送出去；如果超过了n秒，如果还有未发送的数据包，这些数据包将全部被丢弃；而close方法会立即返回。
         * 如果将linger设为0， 和关闭SO_LINGER选项的作用是一样的。
         * 如果底层的Socket实现不支持SO_LINGER都会抛出SocketException例外。当给linger参数传递负数值时，setSoLinger还会抛出一个IllegalArgumentException例外。可以通过getSoLinger方法得到延迟关闭的时间，如果返回-1，则表明SO_LINGER是关闭的。例如，下面的代码将延迟关闭的时间设为1分钟：
         */
        config.setOption(ChannelOption.SO_LINGER, DEFAULT_CONFIG_SOLINGER);

        /**
         * 如果端口忙，但TCP状态位于 TIME_WAIT ，可以重用 端口。如果端口忙，而TCP状态位于其他状态，重用端口时依旧得到一个错误信息， 抛出“Address already in use： JVM_Bind”。如果你的服务程序停止后想立即重启，不等60秒，而新套接字依旧 使用同一端口，此时 SO_REUSEADDR 选项非常有用。必须意识到，此时任何非期
         * 望数据到达，都可能导致服务程序反应混乱，不过这只是一种可能，事实上很不可能。 这个参数在Windows平台与Linux平台表现的特点不一样。在Windows平台表现的特点是不正确的， 在Linux平台表现的特点是正确的。 在Windows平台，多个Socket新建立对象可以绑定在同一个端口上，这些新连接是非TIME_WAIT状态的。这样做并没有多大意义。
         * 在Linux平台，只有TCP状态位于 TIME_WAIT ，才可以重用 端口。这才是正确的行为
         */
        config.setOption(ChannelOption.SO_REUSEADDR, DEFAULT_CONFIG_REUSEADDRESS);

        config.setOption(ChannelOption.SO_KEEPALIVE, DEFAULT_SO_KEEPALIVE);

        /**
         * 这个Socket选项在前面已经讨论过。可以通过这个选项来设置读取数据超时。当输入流的read方法被阻塞时，如果设置timeout（timeout的单位是毫秒），那么系统在等待了timeout毫秒后会抛出一个InterruptedIOException例外。在抛出例外后，输入流并未关闭，你可以继续通过read方法读取数据。
         * 如果将timeout设为0，就意味着read将会无限等待下去，直到服务端程序关闭这个Socket.这也是timeout的默认
         */
        // config.setOption(ChannelOption.SO_TIMEOUT, DEFAULT_CONNECT_TIMEOUT_MILLIS);

        config.setOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, DEFAULT_CONNECT_TIMEOUT_MILLIS);

        config.setOption(ChannelOption.SO_RCVBUF, DEFAULT_SO_RCVBUF);// 设置输入缓冲区的大小
        config.setOption(ChannelOption.SO_SNDBUF, DEFAULT_SO_SNDBUF);// 设置输出缓冲区的大小
    }
}
