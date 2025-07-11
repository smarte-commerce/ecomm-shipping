package com.winnguyen1905.shipping.persistance.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.shipping.persistance.entity.EShippingMethod;
import com.winnguyen1905.shipping.common.enums.ServiceType;

@Repository
public interface ShippingMethodRepository extends JpaRepository<EShippingMethod, Integer> {
    
    Optional<EShippingMethod> findByMethodCode(String methodCode);
    
    List<EShippingMethod> findByIsActive(Boolean isActive);
    
    List<EShippingMethod> findByCarrierCarrierIdAndIsActive(Integer carrierId, Boolean isActive);
    
    List<EShippingMethod> findByZoneZoneIdAndIsActive(Integer zoneId, Boolean isActive);
    
    List<EShippingMethod> findByServiceTypeAndIsActive(ServiceType serviceType, Boolean isActive);
    
    @Query("SELECT m FROM EShippingMethod m WHERE m.carrier.carrierId = :carrierId AND m.zone.zoneId = :zoneId AND m.isActive = true")
    List<EShippingMethod> findByCarrierAndZone(@Param("carrierId") Integer carrierId, @Param("zoneId") Integer zoneId);
    
    @Query("SELECT m FROM EShippingMethod m WHERE m.isActive = true AND m.minWeight <= :weight AND m.maxWeight >= :weight")
    List<EShippingMethod> findByWeightRange(@Param("weight") BigDecimal weight);
    
    @Query("SELECT m FROM EShippingMethod m WHERE m.isActive = true AND m.minOrderValue <= :value AND m.maxOrderValue >= :value")
    List<EShippingMethod> findByOrderValueRange(@Param("value") BigDecimal value);
    
    @Query("SELECT m FROM EShippingMethod m WHERE m.carrier.carrierId = :carrierId AND m.zone.zoneId = :zoneId AND m.isActive = true AND m.minWeight <= :weight AND m.maxWeight >= :weight AND m.minOrderValue <= :value AND m.maxOrderValue >= :value")
    List<EShippingMethod> findAvailableMethodsForShipment(
        @Param("carrierId") Integer carrierId,
        @Param("zoneId") Integer zoneId,
        @Param("weight") BigDecimal weight,
        @Param("value") BigDecimal value
    );
    
    @Query("SELECT m FROM EShippingMethod m WHERE m.isActive = true ORDER BY m.baseRate ASC")
    List<EShippingMethod> findAllOrderByBaseRateAsc();
    
    @Query("SELECT m FROM EShippingMethod m WHERE m.serviceType = :serviceType AND m.isActive = true ORDER BY m.estimatedDaysMin ASC")
    List<EShippingMethod> findByServiceTypeOrderByDeliveryTime(@Param("serviceType") ServiceType serviceType);
    
    @Query("SELECT COUNT(m) FROM EShippingMethod m WHERE m.carrier.carrierId = :carrierId AND m.isActive = :isActive")
    long countByCarrierCarrierIdAndIsActive(@Param("carrierId") Integer carrierId, @Param("isActive") Boolean isActive);
    
    @Query("SELECT COUNT(m) FROM EShippingMethod m WHERE m.zone.zoneId = :zoneId AND m.isActive = :isActive")
    long countByZoneZoneIdAndIsActive(@Param("zoneId") Integer zoneId, @Param("isActive") Boolean isActive);
    
    boolean existsByMethodCode(String methodCode);
} 
