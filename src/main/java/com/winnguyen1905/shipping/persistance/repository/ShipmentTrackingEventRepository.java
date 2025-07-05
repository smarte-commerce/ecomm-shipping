package com.winnguyen1905.shipping.persistance.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.shipping.persistance.entity.EShipmentTrackingEvent;

@Repository
public interface ShipmentTrackingEventRepository extends JpaRepository<EShipmentTrackingEvent, Long> {
    
    List<EShipmentTrackingEvent> findByShipmentShipmentId(Long shipmentId);
    
    List<EShipmentTrackingEvent> findByTrackingNumber(String trackingNumber);
    
    List<EShipmentTrackingEvent> findByEventType(String eventType);
    
    List<EShipmentTrackingEvent> findByCarrierEventCode(String carrierEventCode);
    
    @Query("SELECT ste FROM EShipmentTrackingEvent ste WHERE ste.shipment.shipmentId = :shipmentId ORDER BY ste.eventTimestamp DESC")
    List<EShipmentTrackingEvent> findByShipmentIdOrderByEventTimestampDesc(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT ste FROM EShipmentTrackingEvent ste WHERE ste.trackingNumber = :trackingNumber ORDER BY ste.eventTimestamp DESC")
    List<EShipmentTrackingEvent> findByTrackingNumberOrderByEventTimestampDesc(@Param("trackingNumber") String trackingNumber);
    
    @Query("SELECT ste FROM EShipmentTrackingEvent ste WHERE ste.eventTimestamp BETWEEN :startDate AND :endDate")
    List<EShipmentTrackingEvent> findByEventTimestampBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT ste FROM EShipmentTrackingEvent ste WHERE ste.createdAt BETWEEN :startDate AND :endDate")
    List<EShipmentTrackingEvent> findByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT ste FROM EShipmentTrackingEvent ste WHERE ste.eventLocation LIKE %:location%")
    List<EShipmentTrackingEvent> findByEventLocationContaining(@Param("location") String location);
    
    @Query("SELECT ste FROM EShipmentTrackingEvent ste WHERE ste.eventDescription LIKE %:description%")
    List<EShipmentTrackingEvent> findByEventDescriptionContaining(@Param("description") String description);
    
    @Query("SELECT ste FROM EShipmentTrackingEvent ste WHERE ste.shipment.shipmentId = :shipmentId AND ste.eventTimestamp = (SELECT MAX(ste2.eventTimestamp) FROM EShipmentTrackingEvent ste2 WHERE ste2.shipment.shipmentId = :shipmentId)")
    List<EShipmentTrackingEvent> findLatestEventByShipment(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT COUNT(ste) FROM EShipmentTrackingEvent ste WHERE ste.shipment.shipmentId = :shipmentId")
    Long countByShipment(@Param("shipmentId") Long shipmentId);
    
    @Query("SELECT COUNT(ste) FROM EShipmentTrackingEvent ste WHERE ste.trackingNumber = :trackingNumber")
    Long countByTrackingNumber(@Param("trackingNumber") String trackingNumber);
    
    @Query("SELECT DISTINCT ste.eventType FROM EShipmentTrackingEvent ste")
    List<String> findDistinctEventTypes();
} 