/** ******ThriftDispatcher.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.util.thrift;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import java.lang.reflect.Constructor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.saturn.http.component.AbstractDispatcher;
import com.saturn.http.component.Dispatcher;
import com.saturn.http.component.SaturnHttpRequest;
import com.saturn.http.component.SaturnHttpResponse;
import com.saturn.http.component.SpringUrlHandlerMapping;
import com.saturn.http.util.HttpUtil;
import com.saturn.util.CharsetUtil;
import com.saturn.util.StringTools;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaderValues;

/**
 * @describe:
 * 
 *            <pre>
 * thrift dispatcher
 *            </pre>
 * 
 * @date :2014年7月7日 下午1:48:45
 * @author : ericcoderr@gmail.com
 */
@Sharable
public class ThriftDispatcher extends AbstractDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ThriftDispatcher.class);

    private String serviceInterface;
    private Object serviceImplObject;

    public ThriftDispatcher(String serviceInterface, Object serviceImplObject) {
        this.serviceInterface = serviceInterface;
        this.serviceImplObject = serviceImplObject;
    }

    private static final String THRIFT = "x-thrift";

    private boolean isThrift(String protocol) {
        if (StringTools.isNotEmpty(protocol) && protocol.contains(THRIFT)) {
            return true;
        }
        return false;
    }

    @Override
    public Object forward(SaturnHttpRequest request, SaturnHttpResponse response) throws Exception {
        try {
            ByteBuf buf = request.getContent();
            byte[] array = new byte[buf.readableBytes()];
            buf.readBytes(array);
            LOG.info("req head:{}:req content:{}.", request.header2Str(), new String(array, "utf-8").replaceAll("[^A-Za-z0-9]", "#").replaceAll("#+", "#"));
            if (isThrift(request.getHeader("Accept"))) {
                TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
                Class<?> Processor = Class.forName(serviceInterface + "$Processor");
                Class<?> Iface = Class.forName(serviceInterface + "$Iface");
                Constructor<?> constructor = Processor.getConstructor(Iface);
                TProcessor processor = (TProcessor) constructor.newInstance(serviceImplObject);
                response.headers().add(CONTENT_TYPE, "applicatiRon/x-thrift");
                TProtocol protocol = protocolFactory.getProtocol(new TNettyTransport(request.getContent(), response.content()));

                // TODO 此处依赖于必须在同一个线程里完成操作
                ThriftGetIpHelper.setIp(Thread.currentThread().getId() + "", HttpUtil.getIPByXForwared(request));
                processor.process(protocol, protocol);
                boolean keepAlive = HttpHeaderUtil.isKeepAlive(request);
                response.headers().set(CONTENT_TYPE, "text/plain; charset=" + CharsetUtil.UTF_8);
                response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
                if (keepAlive) {
                    // Add keep alive header as per:
                    // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
                    response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }
                return response;
            } else {
                Dispatcher dispatcher = new Dispatcher();
                return dispatcher.forward(request, response);
            }
        } catch (Throwable e) {
            throw e;
        }
    }

    /**
     * 初始化url映射关系
     */
    @Override
    public void init() {
        try {
            urlHandlerMapping = new SpringUrlHandlerMapping();
            urlHandlerMapping.initUrlMap();
        } catch (Exception e) {
            LOG.error("Init UrlMap failed,", e);
        }
    }
}
