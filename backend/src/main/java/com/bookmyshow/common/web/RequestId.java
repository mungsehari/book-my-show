package com.bookmyshow.common.web;

/** Names used to pass the request ID through headers and logs. */
public final class RequestId {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private RequestId() {}
}
