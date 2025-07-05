package com.winnguyen1905.shipping.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
@EnableFeignClients(basePackages = "com.winnguyen1905.shipping.core.feign.client")
@Slf4j
public class FeignConfig {

    /**
     * Configure Feign logging level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Add authentication token to requests
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Add common headers for microservice communication
            requestTemplate.header("X-Service-Name", "shipping-service");
            requestTemplate.header("X-Service-Version", "1.0.0");
            
            // Add correlation ID for tracing
            String correlationId = generateCorrelationId();
            requestTemplate.header("X-Correlation-ID", correlationId);
            
            log.debug("Adding correlation ID {} to request: {} {}", 
                     correlationId, requestTemplate.method(), requestTemplate.url());
        };
    }

    /**
     * Custom error decoder for handling external service errors
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            HttpStatus status = HttpStatus.valueOf(response.status());
            
            log.warn("External service call failed: {} with status: {}", methodKey, status);
            
            return switch (status) {
                case BAD_REQUEST -> new IllegalArgumentException("Bad request to external service: " + methodKey);
                case UNAUTHORIZED -> new SecurityException("Unauthorized access to external service: " + methodKey);
                case FORBIDDEN -> new SecurityException("Forbidden access to external service: " + methodKey);
                case NOT_FOUND -> new IllegalArgumentException("Resource not found in external service: " + methodKey);
                case INTERNAL_SERVER_ERROR -> new RuntimeException("External service internal error: " + methodKey);
                case SERVICE_UNAVAILABLE -> new RuntimeException("External service unavailable: " + methodKey);
                default -> new RuntimeException("External service error: " + methodKey + " - " + status);
            };
        };
    }

    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
} 
