/** ******IpAuthException.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @describe: <pre>
 * </pre>
 * @date :2014年12月30日 下午2:54:47
 * @author : ericcoderr@gmail.com
 */
public class IpAuthError extends AbstractHttpServerError {

    private static final long serialVersionUID = 1L;
    public final static IpAuthError INSTANCE = new IpAuthError();

    private IpAuthError() {
    }

    @Override
    public HttpResponseStatus getStatus() {
        return HttpResponseStatus.FORBIDDEN;
    }

}
