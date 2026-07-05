package org.example.apigateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2) // phải override trước DefaultErrorWebExceptionHandler của Spring
@RequiredArgsConstructor
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", "Dịch vụ liên quan đang gặp sự cố, vui lòng thử lại sau");
        body.put("path", exchange.getRequest().getPath().value());
        body.put("timestamp", LocalDateTime.now().toString());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = "{\"status\":500,\"error\":\"Internal Server Error\"}".getBytes();
        }

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ConnectException || ex instanceof TimeoutException) {
            return HttpStatus.SERVICE_UNAVAILABLE; // 503, không phải 500
        }
        if (ex instanceof WebClientResponseException webEx) {
            return HttpStatus.resolve(webEx.getStatusCode().value()) != null
                    ? HttpStatus.valueOf(webEx.getStatusCode().value())
                    : HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
