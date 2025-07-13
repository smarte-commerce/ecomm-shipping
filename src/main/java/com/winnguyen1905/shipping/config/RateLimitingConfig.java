package com.winnguyen1905.shipping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class RateLimitingConfig {

    @Value("${shipping.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${shipping.rate-limit.burst-capacity:10}")
    private int burstCapacity;

    @Value("${shipping.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    // Simple in-memory rate limiter for demonstration
    // In production, you would use Redis-based bucket4j or similar
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter();
    }

    public class RateLimiter {
        
        public boolean isAllowed(String clientId) {
            if (!rateLimitEnabled) {
                return true;
            }

            String key = "rate_limit:" + clientId;
            RateLimitInfo info = rateLimitMap.computeIfAbsent(key, k -> new RateLimitInfo());
            
            long now = System.currentTimeMillis();
            long windowStart = now - TimeUnit.MINUTES.toMillis(1);
            
            // Clean up old entries
            info.timestamps.removeIf(timestamp -> timestamp < windowStart);
            
            // Check if limit exceeded
            if (info.timestamps.size() >= requestsPerMinute) {
                log.warn("Rate limit exceeded for client: {}", clientId);
                return false;
            }
            
            // Allow request and record timestamp
            info.timestamps.add(now);
            return true;
        }
        
        public RateLimitStatus getRateLimitStatus(String clientId) {
            if (!rateLimitEnabled) {
                return new RateLimitStatus(requestsPerMinute, requestsPerMinute, 0);
            }

            String key = "rate_limit:" + clientId;
            RateLimitInfo info = rateLimitMap.get(key);
            
            if (info == null) {
                return new RateLimitStatus(requestsPerMinute, requestsPerMinute, 0);
            }
            
            long now = System.currentTimeMillis();
            long windowStart = now - TimeUnit.MINUTES.toMillis(1);
            
            // Clean up old entries
            info.timestamps.removeIf(timestamp -> timestamp < windowStart);
            
            int used = info.timestamps.size();
            int remaining = Math.max(0, requestsPerMinute - used);
            long resetTime = info.timestamps.isEmpty() ? 0 : 
                info.timestamps.stream().mapToLong(Long::longValue).min().orElse(0) + TimeUnit.MINUTES.toMillis(1);
            
            return new RateLimitStatus(requestsPerMinute, remaining, resetTime);
        }
    }

    private static class RateLimitInfo {
        private final java.util.List<Long> timestamps = new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    public record RateLimitStatus(
        int limit,
        int remaining,
        long resetTimeMillis
    ) {}
} 
