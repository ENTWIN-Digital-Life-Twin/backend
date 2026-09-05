package com.digitallifetwin.gateway.filter;

/**
 * Attribute / header names shared across Gateway filters.
 */
public final class GatewayHeaders {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTR = "gateway.correlationId";

    private GatewayHeaders() {
    }
}
