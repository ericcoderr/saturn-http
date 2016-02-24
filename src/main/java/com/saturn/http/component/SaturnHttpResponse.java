/** ******AresHttpResponse.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.component;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.ServerCookieEncoder;
import java.util.HashSet;
import java.util.Set;

/**
 * @describe:
 * 
 *            <pre>
 *            </pre>
 * 
 * @date :2013年7月18日 下午7:45:33
 * @author : ericcoderr@gmail.com
 */
public class SaturnHttpResponse extends DefaultFullHttpResponse {

    public enum ContentType {
        json, plain, xml
    }

    public SaturnHttpResponse(HttpVersion version, HttpResponseStatus status) {
        super(version, status);
    }

    public SaturnHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content) {
        super(version, status, content);
    }

    private Set<Cookie> cookies = new HashSet<Cookie>(2);

    @Override
    public HttpHeaders headers() {
        return super.headers();
    }

    /**
     * 302重定向
     * 
     * @param localtionUrl 重定向的URL
     */
    public void redirect(String localtionUrl) {
        setStatus(HttpResponseStatus.FOUND);
        setHeaderIfEmpty(HttpHeaderNames.LOCATION.toString(), localtionUrl);
    }

    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    public void addCookie(String key, String value) {
        Cookie cookie = new DefaultCookie(key, value);
        addCookie(cookie);
    }

    public void removeCookie(Cookie cookie) {
        this.cookies.remove(cookie);
    }

    public Set<Cookie> getCookies() {
        return cookies;
    }

    public void setCookies() {
        Set<Cookie> cookies = getCookies();
        if (!cookies.isEmpty()) {
            headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.encode(cookies));
        }
    }

    /**
     * 注意不要在这里设置keepAlive相关参数,要设置请使用setKeepAliveTimeout
     */
    public boolean setHeaderIfEmpty(String name, String value) {
        if (headers().get(name) == null) {
            headers().set(name, value);
            return true;
        }
        return false;
    }

    /**
     * 重定向
     * 
     * @param localtionUrl 重定向的URL
     */
    public void redirect(String localtionUrl, HttpResponseStatus status) {
        setStatus(status);
        setHeaderIfEmpty(HttpHeaderNames.LOCATION.toString(), localtionUrl);
    }
}
