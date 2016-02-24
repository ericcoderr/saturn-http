package com.saturn.http.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

public abstract class AbstractHttpServerError extends Error {

    private static final long serialVersionUID = 1L;
    protected HttpResponseStatus status;

    public HttpResponseStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return this.getMessage();
    }
}
