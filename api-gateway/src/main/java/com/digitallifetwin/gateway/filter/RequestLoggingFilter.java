package com.digitallifetwin.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Lightweight structured access logging. Never logs Authorization, JWT, or request bodies.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getRawPath();
        String correlationId = String.valueOf(
                exchange.getAttributeOrDefault(GatewayHeaders.CORRELATION_ID_ATTR, "unknown"));

        return chain.filter(exchange)
                .doOnSuccess(v -> logCompletion(correlationId, method, path, exchange, start))
                .doOnError(ex -> logCompletion(correlationId, method, path, exchange, start));
    }

    private void logCompletion(
            String correlationId,
            String method,
            String path,
            ServerWebExchange exchange,
            long start
    ) {
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        int statusCode = status != null ? status.value() : 0;
        long durationMs = System.currentTimeMillis() - start;
        log.info(
                "correlationId={} method={} path={} status={} durationMs={}",
                correlationId,
                method,
                path,
                statusCode,
                durationMs
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
