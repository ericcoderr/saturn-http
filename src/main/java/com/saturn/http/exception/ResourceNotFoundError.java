package com.saturn.http.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ResourceNotFoundError extends AbstractHttpServerError {

    private static final long serialVersionUID = 1L;
    public final static ResourceNotFoundError INSTANCE = new ResourceNotFoundError();

    private ResourceNotFoundError() {
    }

    @Override
    public HttpResponseStatus getStatus() {
        return HttpResponseStatus.NOT_FOUND;
    }
}
