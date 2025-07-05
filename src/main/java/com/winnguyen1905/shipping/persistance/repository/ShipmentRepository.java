package com.winnguyen1905.shipping.persistance.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.shipping.persistance.entity.EShipment;
import com.winnguyen1905.shipping.persistance.entity.EShipment.ShipmentStatus;

@Repository
public interface ShipmentRepository extends JpaRepository<EShipment, Long> {
    
    Optional<EShipment> findByShipmentNumber(String shipmentNumber);
    
    Optional<EShipment> findByTrackingNumber(String trackingNumber);
    
    List<EShipment> findByOrderId(Long orderId);
    
    List<EShipment> findByStatus(ShipmentStatus status);
    
    Page<EShipment> findByStatus(ShipmentStatus status, Pageable pageable);
    
    List<EShipment> findByCarrierCarrierId(Integer carrierId);
    
    List<EShipment> findByMethodMethodId(Integer methodId);
    
    @Query("SELECT s FROM EShipment s WHERE s.status = :status AND s.estimatedDeliveryDate <= :date")
    List<EShipment> findOverdueShipments(@Param("status") ShipmentStatus status, @Param("date") LocalDate date);
    
    @Query("SELECT s FROM EShipment s WHERE s.status IN :statuses")
    List<EShipment> findByStatusIn(@Param("statuses") List<ShipmentStatus> statuses);
    
    @Query("SELECT s FROM EShipment s WHERE s.shippedAt BETWEEN :startDate AND :endDate")
    List<EShipment> findByShippedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT s FROM EShipment s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    List<EShipment> findByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT s FROM EShipment s WHERE s.estimatedDeliveryDate BETWEEN :startDate AND :endDate")
    List<EShipment> findByEstimatedDeliveryDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT s FROM EShipment s WHERE s.actualDeliveryDate BETWEEN :startDate AND :endDate")
    List<EShipment> findByActualDeliveryDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(s) FROM EShipment s WHERE s.status = :status")
    Long countByStatus(@Param("status") ShipmentStatus status);
    
    @Query("SELECT COUNT(s) FROM EShipment s WHERE s.carrier.carrierId = :carrierId")
    Long countByCarrier(@Param("carrierId") Integer carrierId);
    
    @Query("SELECT s FROM EShipment s WHERE s.trackingNumber IS NOT NULL AND s.status NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED')")
    List<EShipment> findActiveShipmentsWithTracking();
    
    boolean existsByShipmentNumber(String shipmentNumber);
    
    boolean existsByTrackingNumber(String trackingNumber);
} 