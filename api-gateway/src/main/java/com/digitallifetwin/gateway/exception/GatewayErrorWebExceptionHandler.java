package com.digitallifetwin.gateway.exception;

import com.digitallifetwin.gateway.filter.GatewayHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

/**
 * Controlled JSON errors for Gateway-level failures. Never exposes stack traces or internal URLs.
 */
@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        String correlationId = resolveCorrelationId(exchange);
        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(status);

        log.warn(
                "correlationId={} path={} status={} errorType={}",
                correlationId,
                exchange.getRequest().getPath().value(),
                status.value(),
                ex.getClass().getSimpleName()
        );

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(GatewayHeaders.CORRELATION_ID, correlationId);

        GatewayErrorResponse body = new GatewayErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                correlationId
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (isDownstreamUnavailable(ex)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private boolean isDownstreamUnavailable(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof TimeoutException
                    || current instanceof PrematureCloseException
                    || current.getClass().getName().contains("ConnectException")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("connection refused")
                        || lower.contains("connection reset")
                        || lower.contains("failed to resolve")
                        || lower.contains("prematurely closed")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String resolveMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "No gateway route matches this path";
            case SERVICE_UNAVAILABLE, BAD_GATEWAY, GATEWAY_TIMEOUT ->
                    "The requested service is temporarily unavailable";
            case UNAUTHORIZED -> "Authentication required";
            case TOO_MANY_REQUESTS -> "Too many requests. Please try again later.";
            default -> "An unexpected gateway error occurred";
        };
    }

    private static String resolveCorrelationId(ServerWebExchange exchange) {
        Object attr = exchange.getAttribute(GatewayHeaders.CORRELATION_ID_ATTR);
        if (attr instanceof String s && !s.isBlank()) {
            return s;
        }
        String header = exchange.getRequest().getHeaders().getFirst(GatewayHeaders.CORRELATION_ID);
        if (header != null && !header.isBlank()) {
            return header;
        }
        return UUID.randomUUID().toString();
    }
}
