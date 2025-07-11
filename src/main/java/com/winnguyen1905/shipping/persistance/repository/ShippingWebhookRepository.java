package com.winnguyen1905.shipping.persistance.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.winnguyen1905.shipping.persistance.entity.EShippingWebhook;

@Repository
public interface ShippingWebhookRepository extends JpaRepository<EShippingWebhook, Long> {
  List<EShippingWebhook> findByShipmentShipmentId(Long shipmentId);

  List<EShippingWebhook> findByCarrierCarrierId(Integer carrierId);

  List<EShippingWebhook> findByWebhookType(String webhookType);

  List<EShippingWebhook> findByTrackingNumber(String trackingNumber);

  List<EShippingWebhook> findByProcessed(Boolean processed);

  @Query("SELECT sw FROM EShippingWebhook sw WHERE sw.processed = false ORDER BY sw.receivedAt ASC")
  List<EShippingWebhook> findUnprocessedWebhooks();

  @Query("SELECT sw FROM EShippingWebhook sw WHERE sw.processed = true ORDER BY sw.processedAt DESC")
  List<EShippingWebhook> findProcessedWebhooks();

  @Query("SELECT sw FROM EShippingWebhook sw WHERE sw.receivedAt BETWEEN :startDate AND :endDate")
  List<EShippingWebhook> findByReceivedAtBetween(@Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  @Query("SELECT sw FROM EShippingWebhook sw WHERE sw.processedAt BETWEEN :startDate AND :endDate")
  List<EShippingWebhook> findByProcessedAtBetween(@Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  @Query("SELECT sw FROM EShippingWebhook sw WHERE sw.carrier.carrierId = :carrierId AND sw.webhookType = :webhookType")
  List<EShippingWebhook> findByCarrierAndWebhookType(@Param("carrierId") Integer carrierId,
      @Param("webhookType") String webhookType);

  @Query("SELECT sw FROM EShippingWebhook sw WHERE sw.processed = false AND sw.receivedAt < :threshold")
  List<EShippingWebhook> findStaleUnprocessedWebhooks(@Param("threshold") Instant threshold);

  @Query("SELECT COUNT(sw) FROM EShippingWebhook sw WHERE sw.processed = false")
  Long countUnprocessedWebhooks();

  @Query("SELECT COUNT(sw) FROM EShippingWebhook sw WHERE sw.carrier.carrierId = :carrierId")
  Long countByCarrier(@Param("carrierId") Integer carrierId);

  @Query("SELECT COUNT(sw) FROM EShippingWebhook sw WHERE sw.webhookType = :webhookType")
  Long countByWebhookType(@Param("webhookType") String webhookType);

  @Query("SELECT DISTINCT sw.webhookType FROM EShippingWebhook sw")
  List<String> findDistinctWebhookTypes();

  @Query("SELECT sw FROM EShippingWebhook sw WHERE sw.shipment.shipmentId = :shipmentId ORDER BY sw.receivedAt DESC")
  List<EShippingWebhook> findByShipmentIdOrderByReceivedAtDesc(@Param("shipmentId") Long shipmentId);
}
