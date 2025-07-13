package com.winnguyen1905.shipping.persistance.repository;

import com.winnguyen1905.shipping.persistance.entity.EShippingQuote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingQuoteRepository extends JpaRepository<EShippingQuote, Long> {

    // Find quotes by customer
    List<EShippingQuote> findByCustomerId(Long customerId);
    
    Page<EShippingQuote> findByCustomerId(Long customerId, Pageable pageable);

    // Find quotes by external quote ID
    Optional<EShippingQuote> findByExternalQuoteId(String externalQuoteId);

    // Find quotes by vendor
    List<EShippingQuote> findByVendorId(String vendorId);

    // Find quotes by route
    List<EShippingQuote> findByOriginCountryAndDestinationCountry(String originCountry, String destinationCountry);

    // Find quotes by request type
    List<EShippingQuote> findByRequestType(String requestType);

    // Find quotes by selected provider
    List<EShippingQuote> findBySelectedProvider(String selectedProvider);

    // Find used quotes
    List<EShippingQuote> findByIsUsed(Boolean isUsed);

    // Find expired quotes
    @Query("SELECT sq FROM EShippingQuote sq WHERE sq.expiresAt IS NOT NULL AND sq.expiresAt < :now")
    List<EShippingQuote> findExpiredQuotes(@Param("now") Instant now);

    // Find quotes within date range
    @Query("SELECT sq FROM EShippingQuote sq WHERE sq.createdAt BETWEEN :startDate AND :endDate")
    List<EShippingQuote> findByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    // Find quotes with cache hits
    List<EShippingQuote> findByCacheHit(Boolean cacheHit);

    // Find quotes by customer and time range
    @Query("SELECT sq FROM EShippingQuote sq WHERE sq.customerId = :customerId AND sq.createdAt >= :since ORDER BY sq.createdAt DESC")
    List<EShippingQuote> findRecentQuotesByCustomer(@Param("customerId") Long customerId, @Param("since") Instant since);

    // Find quotes by calculation method
    List<EShippingQuote> findByCalculationMethod(String calculationMethod);

    // Find selected quotes (quotes where user selected an option)
    @Query("SELECT sq FROM EShippingQuote sq WHERE sq.selectedOptionId IS NOT NULL")
    List<EShippingQuote> findSelectedQuotes();

    // Find quotes by related shipment
    List<EShippingQuote> findByRelatedShipmentShipmentId(Long shipmentId);

    // Analytics queries
    @Query("SELECT COUNT(sq) FROM EShippingQuote sq WHERE sq.createdAt >= :since")
    Long countQuotesSince(@Param("since") Instant since);

    @Query("SELECT sq.selectedProvider, COUNT(sq) FROM EShippingQuote sq WHERE sq.selectedProvider IS NOT NULL GROUP BY sq.selectedProvider")
    List<Object[]> getProviderSelectionStats();

    @Query("SELECT sq.originCountry, sq.destinationCountry, COUNT(sq) FROM EShippingQuote sq GROUP BY sq.originCountry, sq.destinationCountry ORDER BY COUNT(sq) DESC")
    List<Object[]> getPopularRoutes();

    @Query("SELECT AVG(sq.processingTimeMs) FROM EShippingQuote sq WHERE sq.processingTimeMs IS NOT NULL")
    Double getAverageProcessingTime();

    @Query("SELECT COUNT(sq) FROM EShippingQuote sq WHERE sq.cacheHit = true AND sq.createdAt >= :since")
    Long countCacheHitsSince(@Param("since") Instant since);

    @Query("SELECT sq.calculationMethod, COUNT(sq) FROM EShippingQuote sq GROUP BY sq.calculationMethod")
    List<Object[]> getCalculationMethodStats();

    // Find quotes for optimization analysis
    @Query("SELECT sq FROM EShippingQuote sq WHERE sq.totalOptionsCount = 0 AND sq.createdAt >= :since")
    List<EShippingQuote> findQuotesWithNoOptions(@Param("since") Instant since);

    @Query("SELECT sq FROM EShippingQuote sq WHERE sq.processingTimeMs > :thresholdMs")
    List<EShippingQuote> findSlowQuotes(@Param("thresholdMs") Long thresholdMs);

    // Find quotes by domestic vs international
    List<EShippingQuote> findByIsDomestic(Boolean isDomestic);

    // Delete expired quotes (for cleanup)
    @Query("DELETE FROM EShippingQuote sq WHERE sq.expiresAt IS NOT NULL AND sq.expiresAt < :cutoffDate AND sq.isUsed = false")
    void deleteExpiredUnusedQuotes(@Param("cutoffDate") Instant cutoffDate);

    // Find similar quotes for caching optimization
    @Query("SELECT sq FROM EShippingQuote sq WHERE sq.originCountry = :originCountry AND sq.destinationCountry = :destinationCountry AND sq.totalWeight BETWEEN :minWeight AND :maxWeight AND sq.totalValue BETWEEN :minValue AND :maxValue AND sq.createdAt >= :since")
    List<EShippingQuote> findSimilarQuotes(
        @Param("originCountry") String originCountry,
        @Param("destinationCountry") String destinationCountry,
        @Param("minWeight") java.math.BigDecimal minWeight,
        @Param("maxWeight") java.math.BigDecimal maxWeight,
        @Param("minValue") java.math.BigDecimal minValue,
        @Param("maxValue") java.math.BigDecimal maxValue,
        @Param("since") Instant since
    );

    // Check for existing quote to avoid duplicates
    @Query("SELECT sq FROM EShippingQuote sq WHERE sq.customerId = :customerId AND sq.originCountry = :originCountry AND sq.destinationCountry = :destinationCountry AND sq.totalWeight = :weight AND sq.totalValue = :value AND sq.createdAt >= :since")
    List<EShippingQuote> findRecentSimilarQuotes(
        @Param("customerId") Long customerId,
        @Param("originCountry") String originCountry,
        @Param("destinationCountry") String destinationCountry,
        @Param("weight") java.math.BigDecimal weight,
        @Param("value") java.math.BigDecimal value,
        @Param("since") Instant since
    );

    // Performance monitoring queries
    @Query("SELECT sq.selectedProvider, AVG(sq.processingTimeMs) FROM EShippingQuote sq WHERE sq.selectedProvider IS NOT NULL AND sq.processingTimeMs IS NOT NULL GROUP BY sq.selectedProvider")
    List<Object[]> getAverageProcessingTimeByProvider();

    @Query("SELECT DATE(sq.createdAt), COUNT(sq) FROM EShippingQuote sq WHERE sq.createdAt >= :since GROUP BY DATE(sq.createdAt) ORDER BY DATE(sq.createdAt)")
    List<Object[]> getDailyQuoteVolume(@Param("since") Instant since);
} 
