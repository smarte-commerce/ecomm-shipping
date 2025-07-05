package com.winnguyen1905.shipping.persistance.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.shipping.persistance.entity.EShippingRateCalculation;

@Repository
public interface ShippingRateCalculationRepository extends JpaRepository<EShippingRateCalculation, Long> {
    
    List<EShippingRateCalculation> findByOrderId(Long orderId);
    
    List<EShippingRateCalculation> findByFromZip(String fromZip);
    
    List<EShippingRateCalculation> findByToZip(String toZip);
    
    List<EShippingRateCalculation> findByFromZipAndToZip(String fromZip, String toZip);
    
    List<EShippingRateCalculation> findByRequestedServiceType(String requestedServiceType);
    
    List<EShippingRateCalculation> findBySelectedMethodId(Integer selectedMethodId);
    
    @Query("SELECT src FROM EShippingRateCalculation src WHERE src.createdAt BETWEEN :startDate AND :endDate")
    List<EShippingRateCalculation> findByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT src FROM EShippingRateCalculation src WHERE src.totalWeight BETWEEN :minWeight AND :maxWeight")
    List<EShippingRateCalculation> findByTotalWeightBetween(@Param("minWeight") BigDecimal minWeight, @Param("maxWeight") BigDecimal maxWeight);
    
    @Query("SELECT src FROM EShippingRateCalculation src WHERE src.totalValue BETWEEN :minValue AND :maxValue")
    List<EShippingRateCalculation> findByTotalValueBetween(@Param("minValue") BigDecimal minValue, @Param("maxValue") BigDecimal maxValue);
    
    @Query("SELECT src FROM EShippingRateCalculation src WHERE src.packageCount = :packageCount")
    List<EShippingRateCalculation> findByPackageCount(@Param("packageCount") Integer packageCount);
    
    @Query("SELECT src FROM EShippingRateCalculation src WHERE src.fromZip = :fromZip AND src.toZip = :toZip AND src.totalWeight = :weight AND src.totalValue = :value")
    List<EShippingRateCalculation> findSimilarCalculations(
        @Param("fromZip") String fromZip,
        @Param("toZip") String toZip,
        @Param("weight") BigDecimal weight,
        @Param("value") BigDecimal value
    );
    
    @Query("SELECT COUNT(src) FROM EShippingRateCalculation src WHERE src.fromZip = :fromZip AND src.toZip = :toZip")
    Long countByRoute(@Param("fromZip") String fromZip, @Param("toZip") String toZip);
    
    @Query("SELECT DISTINCT src.fromZip FROM EShippingRateCalculation src")
    List<String> findDistinctFromZips();
    
    @Query("SELECT DISTINCT src.toZip FROM EShippingRateCalculation src")
    List<String> findDistinctToZips();
    
    @Query("SELECT src FROM EShippingRateCalculation src WHERE src.createdAt >= :since ORDER BY src.createdAt DESC")
    List<EShippingRateCalculation> findRecentCalculations(@Param("since") Instant since);
} 