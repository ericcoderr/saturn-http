/** ******AresHttpRequestDecoder.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.component;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @describe:
 * 
 *            <pre>
 *            </pre>
 * 
 * @date :2013年7月22日 上午10:21:32
 * @author : ericcoderr@gmail.com
 */
public class SaturnHttpRequestDecoder extends HttpRequestDecoder {

    private static final Logger log = LoggerFactory.getLogger(SaturnHttpRequestDecoder.class.getSimpleName());

    @Override
    protected SaturnHttpRequest createMessage(String[] initialLine) throws Exception {
        SaturnHttpRequest request = null;
        HttpMethod method = HttpMethod.valueOf(initialLine[0]);
        try {
            request = new SaturnHttpRequest(HttpVersion.valueOf(initialLine[2]), method, initialLine[1]);
        } catch (Exception e) {
            String fix = initialLine[1] + " " + initialLine[2];
            int result = 0;
            for (result = fix.length(); result > 0; --result) {
                if (Character.isWhitespace(fix.charAt(result - 1))) {
                    break;
                }
            }
            String version = fix.substring(result);
            for (; result > 0; --result) {
                if (!Character.isWhitespace(fix.charAt(result - 1))) {
                    break;
                }
            }
            String uri = fix.substring(0, result);
            // uri = uri.replaceAll("\t", "%09").replaceAll("\n",
            // "%0D").replaceAll("\r", "%0A").replaceAll(" ", "+");
            log.error("parse httpRequest initialLine fail!\n\tori:{}\n\t      fix:{}\n\t      uri:{}\n\t  version:{}\n\t{}",
                    new Object[] { Arrays.toString(initialLine), fix, uri, version, e.getMessage() });
            request = new SaturnHttpRequest(HttpVersion.valueOf(version), method, uri);
        } finally {
            if (request != null) {
                request.getContent().release();
            }
        }
        return request;
    }
}
