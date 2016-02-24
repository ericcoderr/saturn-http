/** ******AresServerInitializer.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import com.saturn.http.component.AbstractDispatcher;
import com.saturn.http.component.SaturnHttpRequestDecoder;
import com.saturn.http.component.SaturnHttpResponseEncoder;
import com.saturn.http.jmx.StatisHandler;

/**
 * @describe: <pre>
 *  HttpRequestDecoder会对HTTP请求生成多个 消息对象
 *      1     HttpRequest / HttpResponse
 *   0 - n   HttpContent
 *    1       LastHttpContent
 *    如果你希望为HTTP消息做为单一消息处理，你可以把HttpObjectAggregator放入管道里。
 *    HttpObjectAggregator会把多个消息转换为一个单一的FullHttpRequest或是FullHttpResponse。
 *    一般建议只有消息体是CHUNK是才需要
 *    ********************************************************************
 *    Handler 执行顺序是按addLast()加入顺序执行的,如果handler中的方法要调用next handler的方法，要显示
 *    调用super.父类的方法或者ctx.fireChannelXXX()方法
 * </pre>
 * @date :2013年7月19日 下午4:51:30
 * @author : ericcoderr@gmail.com
 */
public class SaturnServerInitializer extends ChannelInitializer<SocketChannel> {

    private boolean isAggregator = false;

    // dispathcer 分发器
    private AbstractDispatcher dispatcher;

    // CIDR介绍：http://baike.baidu.com/link?url=CcE1wGj4_Ap0YsOHmwWg_RNg45t7jMt__3_BdQDsjbuhazF0w_lBHZalt7i4rimUep3Fj5Ryd9nzcQ6dQ9X3gq
    // TODO 初始化IpFilter ,CIDR表示前222.80.18.18/25为例，其中“/25”表示其前面地址中的前25位代表网络部分，其余位代表主机部分。
    // private static final RuleBasedIpFilter IPFILTER = new RuleBasedIpFilter(new IpSubnetFilterRule("127.0.0.6", 0, IpFilterRuleType.ACCEPT));

    public SaturnServerInitializer(boolean isAggregator, AbstractDispatcher dispatcher) {
        this.isAggregator = isAggregator;
        this.dispatcher = dispatcher;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // pipeline.addLast("ipAuth", IPFILTER);
        pipeline.addLast("decoder", new SaturnHttpRequestDecoder());

        // 只有请求消息是CHUNK是才需要加HttpObjectAggregator,其他情况不推荐
        if (isAggregator) {//1048576 最大上传1m文件
            pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));  //TODO 配置了文件大下，此处直接会抛异常，没找到拦截地方
        }
        pipeline.addLast("encoder", new SaturnHttpResponseEncoder());

        // 连接空闲时间，跟userEventTriggered()方法结合使用
        pipeline.addLast("idle", new IdleStateHandler(30, 30, 15));

        pipeline.addLast("dispatcher", dispatcher);

        // 统计 handler
        pipeline.addLast("statis", StatisHandler.statisHandler);
        // pipeline.addLast("traffic", new GlobalTrafficShapingHandler((EventExecutor) Executors.newScheduledThreadPool(1)));
    }

    public static void main(String[] args) throws UnknownHostException {
        IpSubnetFilterRule rule = new IpSubnetFilterRule("127.0"
                + ".0.50", 32, IpFilterRuleType.ACCEPT);
        InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 8080);
        System.out.println(rule.matches(address));
    }
}
