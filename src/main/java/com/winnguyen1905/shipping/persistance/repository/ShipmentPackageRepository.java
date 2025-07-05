package com.winnguyen1905.shipping.persistance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.shipping.persistance.entity.EShipmentPackage;

@Repository
public interface ShipmentPackageRepository extends JpaRepository<EShipmentPackage, Long> {
    
    List<EShipmentPackage> findByShipmentShipmentId(Long shipmentId);
    
    Optional<EShipmentPackage> findByPackageNumber(String packageNumber);
    
    Optional<EShipmentPackage> findByTrackingNumber(String trackingNumber);
    
    List<EShipmentPackage> findByPackageType(String packageType);
    
    List<EShipmentPackage> findByIsFragile(Boolean isFragile);
    
    List<EShipmentPackage> findByIsLiquid(Boolean isLiquid);
    
    List<EShipmentPackage> findByIsHazardous(Boolean isHazardous);
    
    @Query("SELECT sp FROM EShipmentPackage sp WHERE sp.shipment.shipmentId = :shipmentId")
    List<EShipmentPackage> findByShipmentId(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT sp FROM EShipmentPackage sp WHERE sp.shipment.orderId = :orderId")
    List<EShipmentPackage> findByOrderId(@Param("orderId") Long orderId);
    
    @Query("SELECT SUM(sp.weight) FROM EShipmentPackage sp WHERE sp.shipment.shipmentId = :shipmentId")
    java.math.BigDecimal getTotalWeightByShipment(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT COUNT(sp) FROM EShipmentPackage sp WHERE sp.shipment.shipmentId = :shipmentId")
    Long countByShipment(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT sp FROM EShipmentPackage sp WHERE sp.trackingNumber IS NOT NULL")
    List<EShipmentPackage> findAllWithTracking();
    
    @Query("SELECT sp FROM EShipmentPackage sp WHERE sp.isFragile = true OR sp.isLiquid = true OR sp.isHazardous = true")
    List<EShipmentPackage> findSpecialHandlingPackages();
    
    boolean existsByPackageNumber(String packageNumber);
    
    boolean existsByTrackingNumber(String trackingNumber);
} 