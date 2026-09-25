package com.digitallifetwin.gateway.filter;

import com.digitallifetwin.gateway.config.RateLimitProperties;
import com.digitallifetwin.gateway.exception.GatewayErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * In-memory MVP rate limiter for sensitive Auth endpoints.
 * Per Gateway instance only — replace with distributed limiting for multi-instance production.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!isProtectedAuthEndpoint(request)) {
            return chain.filter(exchange);
        }

        String clientKey = clientKey(request);
        int limit = rateLimitProperties.auth().requests();
        long windowMs = rateLimitProperties.auth().windowSeconds() * 1000L;

        WindowCounter counter = counters.compute(clientKey, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart >= windowMs) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > limit) {
            return writeTooManyRequests(exchange);
        }
        return chain.filter(exchange);
    }

    private boolean isProtectedAuthEndpoint(ServerHttpRequest request) {
        if (request.getMethod() != HttpMethod.POST) {
            return false;
        }
        String path = request.getURI().getRawPath();
        return "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/auth/refresh".equals(path);
    }

    private String clientKey(ServerHttpRequest request) {
        String path = request.getURI().getRawPath();
        InetSocketAddress remote = request.getRemoteAddress();
        String host = remote != null && remote.getAddress() != null
                ? remote.getAddress().getHostAddress()
                : "unknown";
        return host + "|" + path;
    }

    private Mono<Void> writeTooManyRequests(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = String.valueOf(
                exchange.getAttributeOrDefault(GatewayHeaders.CORRELATION_ID_ATTR, "unknown"));
        response.getHeaders().set(GatewayHeaders.CORRELATION_ID, correlationId);

        GatewayErrorResponse body = new GatewayErrorResponse(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Too many requests. Please try again later.",
                exchange.getRequest().getPath().value(),
                correlationId
        );
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /** Exposed for deterministic tests. */
    public void reset() {
        counters.clear();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    private record WindowCounter(long windowStart, AtomicInteger count) {
    }
}
