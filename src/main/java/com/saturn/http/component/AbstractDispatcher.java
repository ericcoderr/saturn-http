/** ******AbstractDispatcher.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.component;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.saturn.http.exception.ResourceNotFoundError;
import com.saturn.http.jmx.StatisUtil;
import com.saturn.http.util.HttpServerConfig;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

/**
 * @describe:
 * 
 *            <pre>
 * HTTP 请求 分发过程
 * -->channelRegistered -->channelActive--> channelInactive-->  channelUnregistered-->
 * 
 * TODO 如果直接让SimpleChannelInboundHandler<I> 泛型为AresHttpRequest(AresHttpRequest extends DefaultHttpRequest )
 * 在channelRead方法里 matcher.match不能匹配类型,mather是AresHttpRequestMather,
 * 因为如果在pipeline里没有加
 * HttpObjectAggregator,HTTP decoder会把消息分成N个消息，此时，当消息为LastHttpContent，不能被matcher.match匹配到
 * 会出现TailHandler.Discarded inbound message {} that reached at the tail of the pipeline.  
 * Please check your pipeline configuration.
 * 
 * 必须显示调用 ctx.fireChannelXXX()方法把请求传递到下一个handler或者调用super方法传递给下一个handler
 *            </pre>
 * 
 * @date :2013年7月18日 下午7:06:44
 * @author : ericcoderr@gmail.com
 */
@Sharable
public abstract class AbstractDispatcher extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDispatcher.class);

    // 此处必须是static，所有AbstractDispatcher 都只使用同一份
    protected static UrlHandlerMapping urlHandlerMapping;

    private boolean isAggregator;

    public boolean isAggregator() {
        return isAggregator;
    }

    public void setAggregator(boolean isAggregator) {
        this.isAggregator = isAggregator;
    }

    /**
     * Default process HTTP request method.
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    protected abstract Object forward(SaturnHttpRequest request, SaturnHttpResponse response) throws Exception;

    /**
     * SERVER 启动初始化过程，譬如URL映射类
     */
    public abstract void init();

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().flush();
    }

    public static final AttributeKey<SaturnHttpRequest> DISPATCHER = AttributeKey.valueOf(SaturnHttpRequest.class, "Dispatcher");

    // TODO 因为handler定义为@Sharable ,可以共享使用，Dispatcher定义为单个实例，所以使用成员变量会有并发问题
    // private AresHttpRequest request;

    /**
     * A Channel is inactive now, which means it is closed
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SaturnHttpRequest request = ctx.attr(DISPATCHER).get();
        if (request != null && request.getHttpPostRequestDecoder() != null) {
            request.getHttpPostRequestDecoder().cleanFiles();
        }
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    /**
     * 不使用 Aggregator(管道处理) TODO 每一个handler的ChannelHandlerContext的对象都是自己特有的
     * 
     * @param ctx
     * @param msg
     * @throws Exception
     */
    private void channelReadWithoutAggregator(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (!msg.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        //
        SaturnHttpRequest request = ctx.attr(DISPATCHER).get();
        if (msg instanceof HttpRequest) {

            // TODO 因为decode里面释放掉了ByteBuf， ctx.attr(DISPATCHER)会在多次解析时获取AresHttpRequest，
            // 同一个AresHttpRequest.content已被释放，不能再次使用
            // request = (AresHttpRequest) msg;
            // if (is100ContinueExpected(request)) {
            // send100Continue(ctx);
            // }
            // Channel ch = ctx.channel();
            // request.setLocalAddr((InetSocketAddress) ch.localAddress());
            // request.setRemoteAddr((InetSocketAddress) ch.remoteAddress());
            HttpRequest req = (HttpRequest) msg;
            request = buildAresHttpRequest(req, ctx);
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            if (!msg.decoderResult().isSuccess()) {
                sendError(ctx, BAD_REQUEST);
                return;
            }
            request.getContent().writeBytes(httpContent.content());
            if (msg instanceof LastHttpContent) {
                LastHttpContent trailer = (LastHttpContent) msg;
                if (!trailer.trailingHeaders().isEmpty()) {
                    // TODO
                }

                if (!trailer.decoderResult().isSuccess()) {
                    sendError(ctx, BAD_REQUEST);
                    return;
                }
                request.getContent().writeBytes(trailer.content());
                SaturnHttpResponse response = new SaturnHttpResponse(HTTP_1_1, OK);
                Object _response = forward(request, response);
                writeResponse(ctx, request, _response);
            }
        }
        // 会随之当前请求ChannelHandlerContext或Channel的回收一并回收
        ctx.attr(DISPATCHER).set(request);
    }

    /**
     * 添加 Aggregator(管道处理)
     * 
     * @param ctx
     * @param msg
     * @throws Exception
     */
    private void channelReadWithAggregator(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (!msg.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        // 此处要重构REQUEST 对象到AresHttpRequest
        FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
        SaturnHttpRequest request = buildAresHttpRequest(fullHttpRequest, ctx);
        request.getContent().writeBytes(fullHttpRequest.content());

        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        SaturnHttpResponse response = new SaturnHttpResponse(HTTP_1_1, OK);
        Object _response = forward(request, response);
        writeResponse(ctx, request, _response);
    }

    private SaturnHttpRequest buildAresHttpRequest(HttpRequest request, ChannelHandlerContext ctx) {
        SaturnHttpRequest _request = new SaturnHttpRequest(request.protocolVersion(), request.method(), request.uri());
        if (HttpHeaderUtil.is100ContinueExpected(request)) {
            send100Continue(ctx);
        }
        _request.headers().add(request.headers());
        _request.setDecoderResult(request.decoderResult());
        Channel ch = ctx.channel();
        _request.setLocalAddr((InetSocketAddress) ch.localAddress());
        _request.setRemoteAddr((InetSocketAddress) ch.remoteAddress());
        return _request;
    }

    // 单独开业务线程池处理,默认:cpu数*5
    protected void messageReceived(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (isAggregator()) {
            channelReadWithAggregator(ctx, msg);
        } else {
            channelReadWithoutAggregator(ctx, msg);
        }
    }

    private static String resetByPeerFilterStr = "reset by";
    private static String resetByPeerFilterStr1 = "强迫关闭";
    private static String connectionTimedOut = "Connection timed out";

    /**
     * 这个不处理业务异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Throwable ex = null;
        if (cause != null) {
            if (cause instanceof NoSuchMethodException || cause instanceof SecurityException) {
                ex = ResourceNotFoundError.INSTANCE;
            } else if (cause instanceof ResourceNotFoundError) {
                ex = ResourceNotFoundError.INSTANCE;
            } else if (cause instanceof IOException && cause.getMessage() != null) {
                String message = cause.getMessage();
                if (message.contains(resetByPeerFilterStr) || message.contains(resetByPeerFilterStr1)) {
                    LOG.error("resetByPeer {}", new Object[] { message });
                } else if (message.contains(connectionTimedOut)) {
                    LOG.error("connectionTimeout {}", new Object[] { message });
                } else {
                    LOG.error("IOException {}", new Object[] { message });
                }
            } else {
                LOG.error("Server error", new Object[] { cause });
            }
        }
        if (ex != null) {
            cause = ex;
        }
        ctx.channel().close();
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        SaturnHttpResponse response = new SaturnHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        ctx.channel().writeAndFlush(new SaturnHttpResponse(HTTP_1_1, CONTINUE));
    }

    private void writeResponse(ChannelHandlerContext ctx, SaturnHttpRequest request, Object _response) throws Exception {
        ChannelFuture future = null;
        try {
            // 如果相应内容为空，关闭连接
            if (_response == null || request == null) {
                ctx.channel().close();
                return;
            }
            future = ctx.channel().writeAndFlush(_response);

            if (HttpServerConfig.HTTP_SERVER_CONFIG.isUseJmx()) {
                StatisUtil.incrBusinessProcessTimes();
            }
        } finally {
            // 非keepalive,服务器发送完数据立即关闭连接
            if (!HttpHeaderUtil.isKeepAlive(request)) {
                if (future != null) {
                    future.addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.channel().close();
                }
            }

            // 释放非堆内存directBuffer()
            request.getContent().release();
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                ctx.channel().close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                ctx.channel().close();
            } else if (event.state() == IdleState.ALL_IDLE) {
                ctx.channel().close();
            }
        }
    }
}
