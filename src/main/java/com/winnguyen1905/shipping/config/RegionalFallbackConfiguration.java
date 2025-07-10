package com.winnguyen1905.shipping.config;

import com.winnguyen1905.shipping.secure.RegionPartition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration for regional fallback mechanisms in Shipping Service.
 * Provides intelligent fallback when regional services are unavailable,
 * including cross-region data replication and manual circuit breaker patterns.
 */
@Slf4j
@Configuration
@EnableAsync
public class RegionalFallbackConfiguration {

    /**
     * Regional fallback service that manages cross-region shipping data access
     */
    @Bean
    public RegionalFallbackService regionalFallbackService(
            RedisTemplate<String, Object> redisTemplate) {
        return new RegionalFallbackService(redisTemplate);
    }

    /**
     * Regional health monitor that tracks the health of regional shipping services
     */
    @Bean
    public RegionalHealthMonitor regionalHealthMonitor() {
        return new RegionalHealthMonitor();
    }

    /**
     * Simple circuit breaker for regional services
     */
    @Service
    @Slf4j
    public static class SimpleCircuitBreaker {
        
        public enum State { CLOSED, OPEN, HALF_OPEN }
        
        private volatile State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private volatile LocalDateTime lastFailureTime;
        
        private final int failureThreshold = 5;
        private final int successThreshold = 3;
        private final Duration timeout = Duration.ofSeconds(60);
        
        public <T> T execute(java.util.function.Supplier<T> operation) throws Exception {
            if (state == State.OPEN) {
                if (isTimeoutExpired()) {
                    state = State.HALF_OPEN;
                    log.debug("Shipping circuit breaker transitioning to HALF_OPEN state");
                } else {
                    throw new RuntimeException("Shipping circuit breaker is OPEN");
                }
            }
            
            try {
                T result = operation.get();
                onSuccess();
                return result;
            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }
        
        private void onSuccess() {
            failureCount.set(0);
            if (state == State.HALF_OPEN) {
                int successes = successCount.incrementAndGet();
                if (successes >= successThreshold) {
                    state = State.CLOSED;
                    successCount.set(0);
                    log.info("Shipping circuit breaker recovered to CLOSED state");
                }
            }
        }
        
        private void onFailure() {
            lastFailureTime = LocalDateTime.now();
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                state = State.OPEN;
                successCount.set(0);
                log.warn("Shipping circuit breaker opened due to {} failures", failures);
            }
        }
        
        private boolean isTimeoutExpired() {
            return lastFailureTime != null && 
                   Duration.between(lastFailureTime, LocalDateTime.now()).compareTo(timeout) > 0;
        }
        
        public State getState() { return state; }
        public boolean isAvailable() { return state != State.OPEN; }
    }

    /**
     * Service that handles fallback logic when regional shipping services are unavailable
     */
    @Service
    @Slf4j
    public static class RegionalFallbackService {
        
        private final RedisTemplate<String, Object> redisTemplate;
        private final Map<RegionPartition, RegionPartition> fallbackMapping;
        private final Map<String, Object> crossRegionCache = new ConcurrentHashMap<>();
        private final Map<RegionPartition, SimpleCircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
        
        public RegionalFallbackService(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
            this.fallbackMapping = initializeFallbackMapping();
            initializeCircuitBreakers();
        }

        /**
         * Execute shipping operation with regional fallback
         */
        public <T> T executeWithFallback(RegionPartition primaryRegion, 
                                        String operationName,
                                        java.util.function.Supplier<T> operation) {
            
            SimpleCircuitBreaker primaryCircuitBreaker = circuitBreakers.get(primaryRegion);
            
            // Try primary region first
            try {
                return primaryCircuitBreaker.execute(() -> {
                    log.debug("Executing shipping {} in primary region: {}", operationName, primaryRegion);
                    setCurrentRegion(primaryRegion);
                    return operation.get();
                });
            } catch (Exception e) {
                log.warn("Primary region {} failed for shipping operation {}: {}", 
                        primaryRegion, operationName, e.getMessage());
                
                // Try fallback region
                return executeWithFallbackRegion(primaryRegion, operationName, operation);
            }
        }

        /**
         * Execute shipping operation in fallback region
         */
        private <T> T executeWithFallbackRegion(RegionPartition primaryRegion,
                                               String operationName,
                                               java.util.function.Supplier<T> operation) {
            
            RegionPartition fallbackRegion = getFallbackRegion(primaryRegion);
            
            if (fallbackRegion == null) {
                throw new RuntimeException("No fallback region available for shipping service in " + primaryRegion);
            }
            
            SimpleCircuitBreaker fallbackCircuitBreaker = circuitBreakers.get(fallbackRegion);
            
            try {
                log.info("Executing shipping {} in fallback region: {} (primary {} unavailable)", 
                        operationName, fallbackRegion, primaryRegion);
                
                return fallbackCircuitBreaker.execute(() -> {
                    setCurrentRegion(fallbackRegion);
                    
                    // Check cross-region cache first
                    String cacheKey = generateCrossRegionCacheKey(primaryRegion, operationName);
                    T cachedResult = getCachedResult(cacheKey);
                    if (cachedResult != null) {
                        log.debug("Returning cached shipping result for {} from fallback region", operationName);
                        return cachedResult;
                    }
                    
                    // Execute in fallback region
                    T result = operation.get();
                    
                    // Cache result for cross-region access
                    cacheResultForCrossRegion(cacheKey, result);
                    
                    // Async replication back to primary when it recovers
                    scheduleReplicationToPrimary(primaryRegion, operationName, result);
                    
                    return result;
                });
                
            } catch (Exception fallbackException) {
                log.error("Fallback region {} also failed for shipping operation {}: {}", 
                         fallbackRegion, operationName, fallbackException.getMessage());
                
                // Try cached data as last resort
                String cacheKey = generateCrossRegionCacheKey(primaryRegion, operationName);
                T cachedResult = getCachedResult(cacheKey);
                if (cachedResult != null) {
                    log.warn("Returning stale cached shipping data for {} due to regional failures", operationName);
                    return cachedResult;
                }
                
                throw new RuntimeException("All regional shipping services unavailable for " + operationName, 
                                         fallbackException);
            }
        }

        /**
         * Get fallback region for a primary region
         */
        public RegionPartition getFallbackRegion(RegionPartition primaryRegion) {
            return fallbackMapping.get(primaryRegion);
        }

        /**
         * Check if a region is currently available for shipping operations
         */
        public boolean isRegionAvailable(RegionPartition region) {
            SimpleCircuitBreaker circuitBreaker = circuitBreakers.get(region);
            return circuitBreaker != null && circuitBreaker.isAvailable();
        }

        /**
         * Get list of available regions in fallback order
         */
        public List<RegionPartition> getAvailableRegionsInOrder(RegionPartition preferredRegion) {
            List<RegionPartition> regions = new ArrayList<>();
            
            if (isRegionAvailable(preferredRegion)) {
                regions.add(preferredRegion);
            }
            
            RegionPartition fallback = getFallbackRegion(preferredRegion);
            while (fallback != null && !regions.contains(fallback)) {
                if (isRegionAvailable(fallback)) {
                    regions.add(fallback);
                }
                fallback = getFallbackRegion(fallback);
            }
            
            return regions;
        }

        /**
         * Schedule async replication to primary region when it recovers
         */
        @Async
        public void scheduleReplicationToPrimary(RegionPartition primaryRegion, 
                                               String operationName, 
                                               Object data) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(5000); // Wait 5 seconds before attempting replication
                    if (isRegionAvailable(primaryRegion)) {
                        log.info("Replicating shipping data back to recovered primary region: {}", primaryRegion);
                        // Implementation would replicate data back to primary region
                    }
                } catch (Exception e) {
                    log.warn("Failed to replicate shipping data to primary region {}: {}", 
                            primaryRegion, e.getMessage());
                }
            });
        }

        /**
         * Initialize circuit breakers for each region
         */
        private void initializeCircuitBreakers() {
            for (RegionPartition region : RegionPartition.values()) {
                circuitBreakers.put(region, new SimpleCircuitBreaker());
            }
        }

        /**
         * Initialize fallback mapping: US -> EU -> ASIA -> US
         */
        private Map<RegionPartition, RegionPartition> initializeFallbackMapping() {
            Map<RegionPartition, RegionPartition> mapping = new HashMap<>();
            mapping.put(RegionPartition.US, RegionPartition.EU);
            mapping.put(RegionPartition.EU, RegionPartition.ASIA);
            mapping.put(RegionPartition.ASIA, RegionPartition.US);
            return mapping;
        }

        private String generateCrossRegionCacheKey(RegionPartition region, String operationName) {
            return "shipping:cross-region:" + region.getCode() + ":" + operationName;
        }

        @SuppressWarnings("unchecked")
        private <T> T getCachedResult(String cacheKey) {
            try {
                Object cached = crossRegionCache.get(cacheKey);
                if (cached != null) {
                    return (T) cached;
                }
                // Also try Redis cache
                return (T) redisTemplate.opsForValue().get(cacheKey);
            } catch (Exception e) {
                log.debug("Error retrieving cached shipping result: {}", e.getMessage());
                return null;
            }
        }

        private void cacheResultForCrossRegion(String cacheKey, Object result) {
            try {
                crossRegionCache.put(cacheKey, result);
                // Also cache in Redis with TTL
                redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(30));
            } catch (Exception e) {
                log.warn("Failed to cache shipping result for cross-region access: {}", e.getMessage());
            }
        }

        private void setCurrentRegion(RegionPartition region) {
            // Set region context for database routing
            // This would integrate with your database routing logic
        }
    }

    /**
     * Monitor that tracks the health of regional shipping services
     */
    @Service
    @Slf4j
    public static class RegionalHealthMonitor {
        
        private final Map<RegionPartition, RegionalHealthStatus> healthStatus = new ConcurrentHashMap<>();

        public RegionalHealthMonitor() {
            initializeHealthStatus();
        }

        /**
         * Get health status for all regions
         */
        public Map<RegionPartition, RegionalHealthStatus> getHealthStatus() {
            return new HashMap<>(healthStatus);
        }

        /**
         * Get health status for specific region
         */
        public RegionalHealthStatus getRegionHealth(RegionPartition region) {
            return healthStatus.get(region);
        }

        /**
         * Check if region is healthy
         */
        public boolean isRegionHealthy(RegionPartition region) {
            RegionalHealthStatus status = getRegionHealth(region);
            return status != null && status.isHealthy();
        }

        /**
         * Get current user's region from request context
         */
        public RegionPartition getCurrentUserRegion() {
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) 
                        RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    String regionHeader = attributes.getRequest().getHeader("X-Region-Code");
                    if (regionHeader != null) {
                        return RegionPartition.fromCode(regionHeader);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not determine user region from request: {}", e.getMessage());
            }
            return RegionPartition.US; // Default fallback
        }

        private void initializeHealthStatus() {
            for (RegionPartition region : RegionPartition.values()) {
                healthStatus.put(region, new RegionalHealthStatus(region, true, "HEALTHY", new Date()));
            }
        }

        /**
         * Update health status based on circuit breaker state
         */
        public void updateRegionHealth(RegionPartition region, boolean healthy, String status) {
            healthStatus.put(region, new RegionalHealthStatus(region, healthy, status, new Date()));
        }
    }

    /**
     * Represents the health status of a regional shipping service
     */
    public static class RegionalHealthStatus {
        private final RegionPartition region;
        private final boolean healthy;
        private final String status;
        private final Date lastChecked;

        public RegionalHealthStatus(RegionPartition region, boolean healthy, String status, Date lastChecked) {
            this.region = region;
            this.healthy = healthy;
            this.status = status;
            this.lastChecked = lastChecked;
        }

        public RegionPartition getRegion() { return region; }
        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
        public Date getLastChecked() { return lastChecked; }

        @Override
        public String toString() {
            return String.format("ShippingRegionalHealth{region=%s, healthy=%s, status='%s', lastChecked=%s}", 
                    region, healthy, status, lastChecked);
        }
    }
} 
