package com.digitallifetwin.gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Accepts or generates {@code X-Correlation-Id}, forwards it downstream, and echoes it on the response.
 * Client-supplied values are validated; invalid/oversized values are replaced.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final int MAX_LENGTH = 64;
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
        String correlationId = sanitizeOrGenerate(incoming);

        exchange.getAttributes().put(GatewayHeaders.CORRELATION_ID_ATTR, correlationId);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Roles");
                    headers.set(GatewayHeaders.CORRELATION_ID, correlationId);
                })
                .build();

        exchange.getResponse().getHeaders().set(GatewayHeaders.CORRELATION_ID, correlationId);

        return chain.filter(exchange.mutate().request(request).build());
    }

    static String sanitizeOrGenerate(String incoming) {
        if (incoming == null) {
            return UUID.randomUUID().toString();
        }
        String trimmed = incoming.trim();
        if (trimmed.length() > MAX_LENGTH || !VALID.matcher(trimmed).matches()) {
            return UUID.randomUUID().toString();
        }
        return trimmed;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
