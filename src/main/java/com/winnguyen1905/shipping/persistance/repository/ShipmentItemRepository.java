package com.winnguyen1905.shipping.persistance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.shipping.persistance.entity.EShipmentItem;

@Repository
public interface ShipmentItemRepository extends JpaRepository<EShipmentItem, Long> {
    
    List<EShipmentItem> findByShipmentShipmentId(Long shipmentId);
    
    List<EShipmentItem> findByOrderItemId(Long orderItemId);
    
    List<EShipmentItem> findByProductId(Long productId);
    
    List<EShipmentItem> findByProductSku(String productSku);
    
    @Query("SELECT si FROM EShipmentItem si WHERE si.shipment.shipmentId = :shipmentId")
    List<EShipmentItem> findByShipmentId(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT si FROM EShipmentItem si WHERE si.productName LIKE %:name%")
    List<EShipmentItem> findByProductNameContaining(@Param("name") String name);
    
    @Query("SELECT SUM(si.quantity) FROM EShipmentItem si WHERE si.shipment.shipmentId = :shipmentId")
    Integer getTotalQuantityByShipment(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT SUM(si.totalWeight) FROM EShipmentItem si WHERE si.shipment.shipmentId = :shipmentId")
    java.math.BigDecimal getTotalWeightByShipment(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT COUNT(si) FROM EShipmentItem si WHERE si.shipment.shipmentId = :shipmentId")
    Long countByShipment(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT si FROM EShipmentItem si WHERE si.shipment.orderId = :orderId")
    List<EShipmentItem> findByOrderId(@Param("orderId") Long orderId);
} 