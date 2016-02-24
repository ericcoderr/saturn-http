/** ******StatisHandler.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.jmx;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * @describe: <pre>
 * 统计handler
 * 
 * 通道关闭时调用顺序是
 * close()-->channelInactive()
 * 
 * TODO 通道释放时间是根据什么确定的？
 * </pre>
 * @date :2014年12月19日 上午10:27:02
 * @author : ericcoderr@gmail.com
 */
@Sharable
public class StatisHandler extends ChannelHandlerAdapter {

    public static final StatisHandler statisHandler = new StatisHandler();

    private StatisHandler() {
    }

    /**
     * 通道打开
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        StatisUtil.incrChannelOpen();
    }

    /**
     * 通道关闭，释放连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        StatisUtil.incrChannelClose();
    }

}
