/** ******Dispatcher.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.component;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.saturn.http.component.UrlHandlerMapping.UrlMapping;
import com.saturn.http.exception.AbstractHttpServerError;
import com.saturn.http.exception.ResourceNotFoundError;
import com.saturn.http.exception.RtnError;
import com.saturn.http.jmx.StatisUtil;
import com.saturn.http.util.HttpServerConfig;
import com.saturn.util.StringHelper;
import com.saturn.util.StringTools;
import com.saturn.util.http.JsonObjectUtil;
import com.saturn.util.http.RtnConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

/**
 * @describe:
 * 
 *            <pre>
 *            </pre>
 * 
 * @date :2013年7月19日 下午8:38:26
 * @author : ericcoderr@gmail.com
 */
// @Sharable 标识这类的实例之间可以在 channel 里面共享
@Sharable
public class Dispatcher extends AbstractDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(Dispatcher.class);

    private static final String FAVICON_ICO = "/favicon.ico";

    // public static final Dispatcher INSTANCE = new Dispatcher();

    // private Dispatcher() {
    // }

    /*
     * (non-Javadoc)
     * @see com.ares.http.component.AbstractDispatcher#forward(com.ares.http.component.AresHttpRequest, com.ares.http.component.AresHttpResponse)
     */
    @Override
    public Object forward(SaturnHttpRequest request, SaturnHttpResponse response) {
        Object returnResp = null;
        try {

            // 此处可以配置ip拦截
            String path = request.getPath();

            // 是否开启请求统计
            if (HttpServerConfig.HTTP_SERVER_CONFIG.isUseJmx()) {
                StatisUtil.incrOpenNumsByPath(path);
            }
            if (isFavicon(path)) {
                // 不处理，返回null 直接关闭连接
                return null;
            }
            UrlMapping urlMapping = urlHandlerMapping.getUrlMapping(path);
            if (urlMapping == null) {
                throw ResourceNotFoundError.INSTANCE;
            }
            Method m = urlMapping.getMethod();

            // TODO 代理接口
            String proxyName = HttpServerConfig.HTTP_SERVER_CONFIG.getProxyImplName();
            if (StringTools.isNotEmpty(proxyName)) {
                Proxy p = (Proxy) (Class.forName(proxyName).newInstance());
                p.proxy(m, request, response);
            }

            // TODO 自定义task处理需等待资源的服务(数据库操作，耗时运算，第三方网络操作等) ，IO线程只处理io,用户业务线程处理完后
            // 怎么提交到io线程里
            // 获取方法的所有参数,用request赋值
            returnResp = m.invoke(urlMapping.getControler(), mapRequestParam(request, response, urlMapping.getMethodParamNames(false)));
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }

            // TODO 处理异常
            if (e instanceof RtnError) {
                returnResp = ((RtnError) e).getJson();
            } else if (e instanceof AbstractHttpServerError) {
                HttpResponseStatus status = ((AbstractHttpServerError) e).getStatus();
                response.setStatus(status);
                returnResp = JsonObjectUtil.getRtnAndDataJsonObject(status.code(), JsonObjectUtil.buildMap("msg", e.getMessage()));
            } else {
                returnResp = JsonObjectUtil.getRtnAndRtnMsgJson(RtnConstants.INTERNAL_SERVER_ERROR, StringHelper.printThrowableSimple(e));
            }
        } finally {
            if (returnResp == null) {
                return null;
            }
            // 此处获取的是directBuffer，所以用完要主动释放
            // http://netty.io/wiki/reference-counted-objects.html
            ByteBuf buf = null;
            try {
                buf = Unpooled.copiedBuffer(returnResp.toString(), CharsetUtil.UTF_8);
                response.content().writeBytes(buf);
            } finally {
                if (buf != null) {
                    buf.release();
                }
            }
            boolean keepAlive = HttpHeaderUtil.isKeepAlive(request);

            // TODO 改为根据反射确定返回类型，或启动时注入返回类型
            HttpHeaders httpHeaders = response.headers();
            if (httpHeaders.get(CONTENT_TYPE) == null) {
                httpHeaders.set(CONTENT_TYPE, "text/plain; charset=" + CharsetUtil.UTF_8);
            }

            if (keepAlive) {
                // Add 'Content-Length' header only for a keep-alive connection.如果不是keep-alive，服务器返回数据后要主动关闭连接
                httpHeaders.setInt(CONTENT_LENGTH, response.content().readableBytes());

                // Add keep alive header as per:
                // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
                httpHeaders.set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            response.setCookies();
        }
        return response;
    }

    // TODO 外部设置
    private boolean isSpring = true;

    /**
     * 初始化controler
     */
    @Override
    public void init() {
        if (isSpring) {
            urlHandlerMapping = new SpringUrlHandlerMapping();
        } else {
            urlHandlerMapping = new UrlHandlerMapping();

            // TODO 此处有个bug,如果扫描的类包含内部类，会报错
            urlHandlerMapping.setPkgNames("com.ares.http.controler");
        }
        try {
            urlHandlerMapping.initUrlMap();
        } catch (Exception e) {
            LOG.error("Init UrlMap failed,", e);
        }
    }

    /**
     * <pre>
     * 用request 对象给方法中的参数赋值
     * </pre>
     */
    public Object[] mapRequestParam(SaturnHttpRequest request, SaturnHttpResponse response, Map<String, Class<?>> methodParamNames) {
        Object[] methodValues = new Object[methodParamNames.size()];
        int i = 0;
        for (Map.Entry<String, Class<?>> entry : methodParamNames.entrySet()) {
            if (entry.getValue() == SaturnHttpRequest.class || entry.getValue().getSuperclass() ==SaturnHttpRequest.class ) {
                methodValues[i] = request;
                i++;
                continue;
            } else if (entry.getValue() == SaturnHttpResponse.class) {
                methodValues[i] = response;
                i++;
                continue;
            }

            String value = request.getParameter(entry.getKey());
            if (value == null) {
                throw new IllegalArgumentException(entry.getKey() + " is must.");
            }

            Class<?> clazz = entry.getValue();
            if (clazz == byte.class || clazz == Byte.class) {

                // byte 必须是在byte取值范围内
                methodValues[i] = Byte.valueOf(value);
            } else if (clazz == short.class || clazz == Short.class) {
                methodValues[i] = Short.valueOf(value);
            } else if (clazz == int.class || clazz == Integer.class) {
                methodValues[i] = Integer.valueOf(value);
            } else if (clazz == long.class || clazz == Long.class) {
                methodValues[i] = Long.valueOf(value);
            } else if (clazz == float.class || clazz == Float.class) {
                methodValues[i] = Float.valueOf(value);
            } else if (clazz == double.class || clazz == Double.class) {
                methodValues[i] = Double.valueOf(value);
            } else if (clazz == boolean.class || clazz == Boolean.class) {
                methodValues[i] = Boolean.valueOf(value);
            } else if (clazz == char.class || clazz == Character.class) {

                // String 的length必须是1，如果大于1 ，其它的直接忽略
                methodValues[i] = value.charAt(0);
            } else {
                methodValues[i] = value;
            }
            i++;
        }
        return methodValues;
    }

    /**
     * 判断请求是不是favicon.ico
     * 
     * @param path
     * @return
     */
    private boolean isFavicon(String path) {
        if (FAVICON_ICO.equalsIgnoreCase(path)) {
            return true;
        }
        return false;
    }
}
