/** ******AresHttpRequest.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.component;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import com.saturn.http.util.HttpServerConfig;
import com.saturn.http.util.HttpUtil;
import com.saturn.util.CollectionUtil;
import com.saturn.util.StringTools;
import com.saturn.util.log.Log;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

/**
 * @describe:
 * 
 *            <pre>
 * HTTP request 处理类
 *            </pre>
 * 
 * @date :2013年7月18日 下午7:19:12
 * @author : ericcoderr@gmail.com
 */
public class SaturnHttpRequest extends DefaultHttpRequest {

    private static final Logger log = Log.getLogger();

    private InetSocketAddress localAddr;
    private InetSocketAddress remoteAddr;
    private String localIP;
    private Map<String, List<String>> parameterMap;
    private QueryStringDecoder queryStringDecoder;
    private HttpPostRequestDecoder postRequestDecoder;
    private Map<String, Cookie> cookies;
    private boolean readingChunks;
    private ByteBuf content = Unpooled.directBuffer(512); // 非堆内存不是jvm gc回收，要自己释放
    // private ByteBuf content = Unpooled.buffer(256);
    private Charset charset = CharsetUtil.UTF_8; // 默认是UTF-8
    private HttpContent httpContent;

    public SaturnHttpRequest(HttpVersion httpVersion, HttpMethod method, String uri) {
        super(httpVersion, method, uri);
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    private QueryStringDecoder getQueryStringDecoder() {
        if (queryStringDecoder == null) {
            queryStringDecoder = new QueryStringDecoder(uri(), getCharset());
        }
        return queryStringDecoder;
    }

    // CHUNK操作
    private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk
                                                                                                               // if
                                                                                                               // size
                                                                                                               // exceed

    private static final String uploadBaseDir = HttpServerConfig.HTTP_SERVER_CONFIG.getUploadBaseDir();

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
                                                         // on exit (in normal
                                                         // exit)
        DiskFileUpload.baseDirectory = null; // system temp directory ,如果文件生成失败，临时文件存放目录
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
                                                        // exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
    }

    public HttpPostRequestDecoder getHttpPostRequestDecoder() {
        // if GET Method: should not try to create a HttpPostRequestDecoder
        if (postRequestDecoder == null) {
            try {
                HttpMethod method = method();
                if (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT))
                    postRequestDecoder = new HttpPostRequestDecoder(factory, this);
            } catch (ErrorDataDecoderException e1) {
                log.error("HttpPostRequestDecoder create error,request:{}", this, e1);
                return null;
            }

            readingChunks = HttpHeaderUtil.isTransferEncodingChunked(this);
            if (readingChunks) {
                // Chunk version
                readingChunks = true;
            }
        }
        return postRequestDecoder;
    }

    // DECODE POST的数据
    private void decodePost() throws UnsupportedEncodingException {
        if (getHttpPostRequestDecoder() != null) {
            // New chunk is received
            if (this.getContent() != null) {
                try {
                    // TODO DefaultHttpContent, postRequestDecoder.offer不能获取最后一对键值 ????
                    // httpContent = new DefaultHttpContent(this.getContent());
                    httpContent = new DefaultLastHttpContent(this.getContent());
                    // 把请求数据写入
                    postRequestDecoder.offer(httpContent);
                    try {
                        this.parameterMap = new HashMap<String, List<String>>();
                        /**
                         * <pre>
                         * TODO 发现当取到最后一对键值时，hasNext还是可以调用，导致 bodyListHttpDataRank >= bodyListHttpData.size()
                         *    HttpPostStandardRequestDecoder
                         *    public boolean hasNext() {
                         *         checkDestroyed();
                         * 
                         *         if (currentStatus == MultiPartStatus.EPILOGUE) {
                         *             // OK except if end of list
                         *             if (bodyListHttpDataRank >= bodyListHttpData.size()) {
                         *                 throw new EndOfDataDecoderException();
                         *             }
                         *         }
                         *         return !bodyListHttpData.isEmpty() && bodyListHttpDataRank < bodyListHttpData.size();
                         *     }
                         * </pre>
                         */

                        // while (postRequestDecoder.hasNext()) {
                        // InterfaceHttpData data = postRequestDecoder.next();
                        // if (data != null) {
                        // try {
                        // // new value
                        // writeHttpPostData(data);
                        // } finally {
                        // data.release();
                        // }
                        // }
                        // }

                        List<InterfaceHttpData> list = postRequestDecoder.getBodyHttpDatas();
                        if (list != null) {
                            for (InterfaceHttpData data : list) {
                                writeHttpPostData(data);
                            }
                        }

                    } catch (EndOfDataDecoderException e1) {
                        // end
                        log.error("PostRequestDecoder decode failed.", e1);
                    }
                } catch (ErrorDataDecoderException e1) {
                    log.error("PostRequestDecoder decode failed.", e1);
                    return;
                }
                // example of reading only if at the end
                if (httpContent instanceof LastHttpContent) {
                    readingChunks = false;
                    // request = null; // TODO
                    // destroy the decoder to release all resources
                    postRequestDecoder.destroy();
                    postRequestDecoder = null;
                }
            }
        }
    }

    private void writeHttpPostData(InterfaceHttpData data) {
        if (data.getHttpDataType() == HttpDataType.Attribute) {
            Attribute attribute = (Attribute) data;
            String value;
            String key;
            try {
                key = attribute.getName();
                value = attribute.getValue();
                List<String> ori = parameterMap.get(key);
                if (ori == null) {
                    ori = new ArrayList<String>(1);
                    parameterMap.put(key, ori);
                }
                ori.add(value);
            } catch (IOException e1) {
                // Error while reading data from File, only print name and error
                log.error("", e1);
                return;
            }
        } else {
            if (data.getHttpDataType() == HttpDataType.FileUpload) {
                FileUpload fileUpload = (FileUpload) data;
                if (fileUpload.isCompleted()) {
                    // fileUpload.isInMemory();// tells if the file is in Memory
                    // or on File
                    // fileUpload.renameTo(dest); // enable to move into another
                    // File dest
                    createFile(createDirectory(), fileUpload);
                    postRequestDecoder.removeHttpDataFromClean(fileUpload);
                    // the File of to delete file
                }
            }
        }
    }

    /**
     * 上传文件保存写硬盘操作，如果业务有特殊操作，可以覆写这个类 directory 一定是/结尾
     * 
     * @throws IOException
     */
    protected void createFile(String directory, FileUpload fileUpload) {
        if (!directory.endsWith("/")) {
            directory = directory + "/";
        }
        File fileDirectory = new File(directory);
        if (!fileDirectory.isDirectory()) {
            fileDirectory.mkdirs();
        }
        File dest = new File(fileDirectory.getPath() + createFileName(fileUpload.getFilename()));

        try {
            fileUpload.renameTo(dest);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private String createFileName(String name) {
        String filePath = "/" + System.currentTimeMillis() + name.substring(name.indexOf("."), name.length());
        return filePath;
    }

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");

    private String createDirectory() {
        String date = LocalDate.now().format(DF);
        return uploadBaseDir + "/image/" + date;
    }

    @Override
    public void setDecoderResult(DecoderResult decoderResult) {
        super.setDecoderResult(decoderResult);
    }

    public Locale getLocale() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 添加REQUEST参数，必须是key-value的形式，keyValue的个数必须是偶数
     * 
     * @param keyValue
     */
    public void addParameters(Object... keyValue) {
        if (keyValue.length % 2 != 0) {
            throw new IllegalArgumentException("keyvalue.length is invalid:" + keyValue.length);
        }
        Map<String, List<String>> params = getParameterMap();
        for (int i = 0; i < keyValue.length; i++) {
            params.put(keyValue[i++].toString(), CollectionUtil.buildList(keyValue[i].toString()));
        }
    }

    /**
     * 查询的key不存在返回null
     * 
     * @param key
     * @return
     */
    public String getParameter(String key) {
        if (StringTools.isEmpty(key)) {
            throw new IllegalArgumentException("key isEmpty:[" + key + "]");
        }
        List<String> values = getParameterMap().get(key);
        if (values != null && values.size() != 0) {
            return values.get(0);
        }
        return null;
    }

    public int getParameterInteger(String key, int defaultValue) {
        if (StringTools.isEmpty(key)) {
            throw new IllegalArgumentException("key isEmpty:[" + key + "]");
        }
        List<String> values = getParameterMap().get(key);
        if (values != null && values.size() != 0 && StringTools.isNumberStr(values.get(0))) {
            return Integer.parseInt(values.get(0));
        }
        return defaultValue;
    }

    public String getParameter(String key, String defaultValue) {
        if (StringTools.isEmpty(key)) {
            throw new IllegalArgumentException("key isEmpty:[" + key + "]");
        }
        List<String> values = getParameterMap().get(key);
        if (values != null && values.size() != 0) {
            return values.get(0);
        }
        return defaultValue;
    }

    public Map<String, List<String>> getParameterMap() {
        if (parameterMap == null) {
            try {
                if (this.method() == HttpMethod.GET) {
                    parameterMap = getQueryStringDecoder().parameters();
                } else if (this.method() == HttpMethod.POST || this.method().equals(HttpMethod.PUT)) {
                    decodePost();
                }
            } catch (Exception e) {
                log.error("Decode fail,req:{},{}:{}", new Object[] { this, e.getClass(), e.getMessage() });
                parameterMap = Collections.emptyMap();
            }
        }
        return parameterMap;
    }

    public InetSocketAddress getLocalAddr() {
        return localAddr;
    }

    public String getLocalName() {
        return localAddr.getHostName();
    }

    public int getLocalPort() {
        return localAddr.getPort();
    }

    public String getRemoteHost() {
        return remoteAddr.getHostName();
    }

    public String getRemoteIp() {
        String remoteIP = "";
        try {
            remoteIP = HttpUtil.getIP((InetSocketAddress) getRemoteAddr());
        } catch (Exception e) {
            log.error("", e);
        }
        return remoteIP;
    }

    public InetSocketAddress getRemoteAddr() {
        return remoteAddr;
    }

    public int getRemotePort() {
        return remoteAddr.getPort();
    }

    public Cookie getCookie(String cookieName) {
        if (StringTools.isEmpty(cookieName)) {
            throw new IllegalArgumentException("cookieName isEmpty:[" + cookieName + "]");
        }
        return getCookies().get(cookieName);
    }

    public String getCookieValue(String cookieName) {
        if (StringTools.isEmpty(cookieName)) {
            throw new IllegalArgumentException("cookieName isEmpty:[" + cookieName + "]");
        }
        Cookie cookie = getCookie(cookieName);
        return cookie == null ? null : cookie.value();
    }

    public Map<String, Cookie> getCookies() {
        if (cookies == null) {
            String cookieStr = headers().getAndConvert(HttpHeaderNames.COOKIE);
            cookies = new HashMap<String, Cookie>();
            if (StringTools.isEmpty(cookieStr)) {
                return cookies;
            }
            Set<Cookie> set = ServerCookieDecoder.decode(cookieStr);
            for (Cookie cookie : set) {
                cookies.put(cookie.name(), cookie);
            }
        }
        return cookies;
    }

    @Override
    public HttpHeaders headers() {
        return super.headers();
    }

    public String getHeader(String headName) {
        if (StringTools.isEmpty(headName)) {
            throw new IllegalArgumentException("headName isEmpty:[" + headName + "]");
        }
        HttpHeaders headers = headers();
        return headers == null ? null : headers.getAndConvert(headName);
    }

    private String requestURL;

    public String getRequestURL() {
        if (requestURL == null) {
            String host = getHeader(HttpHeaderNames.HOST.toString());
            String port = getLocalPort() == 80 ? "" : ":" + getLocalPort();
            requestURL = "http://" + (StringTools.isEmpty(host) ? getLocalIP() + port : host) + uri();
        }
        return requestURL;
    }

    public String getLocalIP() {
        if (localIP == null) {
            try {
                localIP = HttpUtil.getIP((InetSocketAddress) getLocalAddr());
            } catch (Exception e) {
                log.error("", e);
                localIP = "";
            }
        }
        return localIP;
    }

    public String getPath() {
        return getQueryStringDecoder().path();
    }

    public void setLocalAddr(InetSocketAddress localAddr) {
        this.localAddr = localAddr;
    }

    public void setRemoteAddr(InetSocketAddress remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public ByteBuf getContent() {
        content.readerIndex(0);
        return content;
    }

    /**
     * 输出request头信息
     * 
     * @return
     */
    public String header2Str() {
        StringBuilder headerStr = new StringBuilder();
        Iterator<Entry<String, String>> it = headers().iteratorConverted();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            headerStr.append("name:").append(entry.getKey());
            headerStr.append(",value:").append(entry.getValue()).append(";");
        }
        return headerStr.toString();
    }

    @Override
    public String toString() {
        return Integer.toHexString(hashCode()) + this.getRemoteAddr() + "/" + this.method() + " " + this.uri();
    }
}
