package com.winnguyen1905.shipping.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.shipping.core.model.request.CreateWebhookRequest;
import com.winnguyen1905.shipping.core.model.response.WebhookResponse;
import com.winnguyen1905.shipping.core.service.WebhookService;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;
import com.winnguyen1905.shipping.exception.BadRequestException;
import com.winnguyen1905.shipping.exception.BusinessLogicException;
import com.winnguyen1905.shipping.exception.ResourceNotFoundException;
import com.winnguyen1905.shipping.persistance.entity.EShippingWebhook;
import com.winnguyen1905.shipping.persistance.entity.EShipment;
import com.winnguyen1905.shipping.persistance.entity.EShippingCarrier;
import com.winnguyen1905.shipping.persistance.repository.ShippingWebhookRepository;
import com.winnguyen1905.shipping.persistance.repository.ShipmentRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingCarrierRepository;
import com.winnguyen1905.shipping.secure.TAccountRequest;
import com.winnguyen1905.shipping.util.ShippingValidationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WebhookServiceImpl implements WebhookService {

  private final ShippingWebhookRepository webhookRepository;
  private final ShipmentRepository shipmentRepository;
  private final ShippingCarrierRepository carrierRepository;
  private final ObjectMapper objectMapper;

  @Override
  public WebhookResponse createWebhook(CreateWebhookRequest request, TAccountRequest accountRequest) {
    log.info("Creating webhook for carrier ID: {} with type: {} for account: {}",
        request.getCarrierId(), request.getWebhookType(), accountRequest.username());

    // Validate request
    validateCreateWebhookRequest(request);

    // Validate carrier exists and is active
    EShippingCarrier carrier = carrierRepository.findById(request.getCarrierId())
        .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + request.getCarrierId()));

    if (!carrier.getIsActive()) {
      throw new BusinessLogicException("Cannot create webhook for inactive carrier");
    }

    // Validate shipment exists if provided
    EShipment shipment = null;
    if (request.getShipmentId() != null) {
      shipment = shipmentRepository.findById(request.getShipmentId())
          .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + request.getShipmentId()));
    }

    // Validate webhook data is valid JSON
    validateWebhookData(request.getWebhookData());

    // Create webhook entity
    EShippingWebhook webhook = EShippingWebhook.builder()
        .shipment(shipment)
        .carrier(carrier)
        .webhookType(request.getWebhookType())
        .trackingNumber(request.getTrackingNumber())
        .webhookData(request.getWebhookData())
        .processed(false)
        .build();

    webhook = webhookRepository.save(webhook);

    log.info("Webhook created successfully with ID: {}", webhook.getWebhookId());
    return mapToWebhookResponse(webhook);
  }

  @Override
  @Transactional(readOnly = true)
  public WebhookResponse getWebhookById(Long id, TAccountRequest accountRequest) {
    log.info("Getting webhook by ID: {} for account: {}", id, accountRequest.username());

    EShippingWebhook webhook = webhookRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Webhook not found with ID: " + id));

    return mapToWebhookResponse(webhook);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<WebhookResponse> getAllWebhooks(Long shipmentId, Integer carrierId, String webhookType,
      Boolean processed, String trackingNumber,
      Pageable pageable, TAccountRequest accountRequest) {
    log.info("Getting all webhooks with filters for account: {}", accountRequest.username());

    // Build query based on filters
    List<EShippingWebhook> webhooks = webhookRepository.findAll();

    // Apply filters
    if (shipmentId != null) {
      webhooks = webhooks.stream()
          .filter(w -> w.getShipment() != null && w.getShipment().getShipmentId().equals(shipmentId))
          .collect(Collectors.toList());
    }

    if (carrierId != null) {
      webhooks = webhooks.stream()
          .filter(w -> w.getCarrier().getCarrierId().equals(carrierId))
          .collect(Collectors.toList());
    }

    if (webhookType != null && !webhookType.trim().isEmpty()) {
      webhooks = webhooks.stream()
          .filter(w -> w.getWebhookType().equals(webhookType))
          .collect(Collectors.toList());
    }

    if (processed != null) {
      webhooks = webhooks.stream()
          .filter(w -> w.getProcessed().equals(processed))
          .collect(Collectors.toList());
    }

    if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
      webhooks = webhooks.stream()
          .filter(w -> trackingNumber.equals(w.getTrackingNumber()))
          .collect(Collectors.toList());
    }

    // Sort by received date descending
    webhooks.sort((w1, w2) -> w2.getReceivedAt().compareTo(w1.getReceivedAt()));

    // Apply pagination
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), webhooks.size());

    List<WebhookResponse> responses = webhooks.subList(start, end).stream()
        .map(this::mapToWebhookResponse)
        .collect(Collectors.toList());

    return new PageImpl<>(responses, pageable, webhooks.size());
  }

  @Override
  @Transactional(readOnly = true)
  public List<WebhookResponse> getWebhooksByShipment(Long shipmentId, TAccountRequest accountRequest) {
    log.info("Getting webhooks for shipment ID: {} for account: {}", shipmentId, accountRequest.username());

    // Validate shipment exists
    if (!shipmentRepository.existsById(shipmentId)) {
      throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
    }

    List<EShippingWebhook> webhooks = webhookRepository.findByShipmentIdOrderByReceivedAtDesc(shipmentId);

    return webhooks.stream()
        .map(this::mapToWebhookResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<WebhookResponse> getWebhooksByCarrier(Integer carrierId, TAccountRequest accountRequest) {
    log.info("Getting webhooks for carrier ID: {} for account: {}", carrierId, accountRequest.username());

    // Validate carrier exists
    if (!carrierRepository.existsById(carrierId)) {
      throw new ResourceNotFoundException("Carrier not found with ID: " + carrierId);
    }

    List<EShippingWebhook> webhooks = webhookRepository.findByCarrierCarrierId(carrierId);

    // Sort by received date descending
    webhooks.sort((w1, w2) -> w2.getReceivedAt().compareTo(w1.getReceivedAt()));

    return webhooks.stream()
        .map(this::mapToWebhookResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<WebhookResponse> getUnprocessedWebhooks(TAccountRequest accountRequest) {
    log.info("Getting unprocessed webhooks for account: {}", accountRequest.username());

    List<EShippingWebhook> webhooks = webhookRepository.findUnprocessedWebhooks();

    return webhooks.stream()
        .map(this::mapToWebhookResponse)
        .collect(Collectors.toList());
  }

  @Override
  public WebhookResponse processWebhook(Long id, TAccountRequest accountRequest) {
    log.info("Processing webhook ID: {} for account: {}", id, accountRequest.username());

    EShippingWebhook webhook = webhookRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Webhook not found with ID: " + id));

    if (webhook.getProcessed()) {
      throw new BusinessLogicException("Webhook is already processed");
    }

    try {
      // Process the webhook data
      processWebhookData(webhook);

      // Mark as processed
      webhook.setProcessed(true);
      webhook.setProcessedAt(Instant.now());

      webhook = webhookRepository.save(webhook);

      log.info("Webhook processed successfully with ID: {}", webhook.getWebhookId());
      return mapToWebhookResponse(webhook);

    } catch (Exception e) {
      log.error("Failed to process webhook ID: {}, error: {}", id, e.getMessage());
      throw new BusinessLogicException("Failed to process webhook: " + e.getMessage());
    }
  }

  @Override
  public WebhookResponse.BatchProcessResponse processBatchWebhooks(List<Long> webhookIds,
      TAccountRequest accountRequest) {
    log.info("Batch processing {} webhooks for account: {}", webhookIds.size(), accountRequest.username());

    if (webhookIds == null || webhookIds.isEmpty()) {
      throw new BadRequestException("Webhook IDs list cannot be null or empty");
    }

    List<WebhookResponse.BatchProcessResponse.ProcessingResult> results = new ArrayList<>();
    int successful = 0;
    int failed = 0;

    for (Long webhookId : webhookIds) {
      long startTime = System.currentTimeMillis();

      try {
        processWebhook(webhookId, accountRequest);

        long processingTime = System.currentTimeMillis() - startTime;
        results.add(WebhookResponse.BatchProcessResponse.ProcessingResult.builder()
            .webhookId(webhookId)
            .isSuccessful(true)
            .processingTimeMs(processingTime)
            .build());
        successful++;

      } catch (Exception e) {
        log.error("Failed to process webhook ID: {}, error: {}", webhookId, e.getMessage());

        long processingTime = System.currentTimeMillis() - startTime;
        results.add(WebhookResponse.BatchProcessResponse.ProcessingResult.builder()
            .webhookId(webhookId)
            .isSuccessful(false)
            .errorMessage(e.getMessage())
            .processingTimeMs(processingTime)
            .build());
        failed++;
      }
    }

    return WebhookResponse.BatchProcessResponse.builder()
        .totalProcessed(webhookIds.size())
        .successfulProcessed(successful)
        .failedProcessed(failed)
        .processingResults(results)
        .processedAt(Instant.now())
        .build();
  }

  @Override
  public WebhookResponse.BatchProcessResponse processAllUnprocessedWebhooks(TAccountRequest accountRequest) {
    log.info("Processing all unprocessed webhooks for account: {}", accountRequest.username());

    List<EShippingWebhook> unprocessedWebhooks = webhookRepository.findUnprocessedWebhooks();

    if (unprocessedWebhooks.isEmpty()) {
      log.info("No unprocessed webhooks found");
      return WebhookResponse.BatchProcessResponse.builder()
          .totalProcessed(0)
          .successfulProcessed(0)
          .failedProcessed(0)
          .processingResults(new ArrayList<>())
          .processedAt(Instant.now())
          .build();
    }

    List<Long> webhookIds = unprocessedWebhooks.stream()
        .map(EShippingWebhook::getWebhookId)
        .collect(Collectors.toList());

    return processBatchWebhooks(webhookIds, accountRequest);
  }

  @Override
  public WebhookResponse retryWebhookProcessing(Long id, TAccountRequest accountRequest) {
    log.info("Retrying webhook processing for ID: {} for account: {}", id, accountRequest.username());

    EShippingWebhook webhook = webhookRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Webhook not found with ID: " + id));

    // Reset processed status to allow retry
    webhook.setProcessed(false);
    webhook.setProcessedAt(null);

    webhook = webhookRepository.save(webhook);

    // Process the webhook
    return processWebhook(id, accountRequest);
  }

  @Override
  @Transactional(readOnly = true)
  public WebhookResponse.WebhookStatistics getWebhookStatistics(TAccountRequest accountRequest) {
    log.info("Getting webhook statistics for account: {}", accountRequest.username());

    long totalWebhooks = webhookRepository.count();
    long processedWebhooks = webhookRepository.findProcessedWebhooks().size();
    long pendingWebhooks = webhookRepository.countUnprocessedWebhooks();
    long failedWebhooks = 0; // Simplified - would need additional fields to track failures

    List<String> webhookTypes = webhookRepository.findDistinctWebhookTypes();

    double processingRate = totalWebhooks > 0 ? (double) processedWebhooks / totalWebhooks : 0.0;

    return WebhookResponse.WebhookStatistics.builder()
        .totalWebhooks(totalWebhooks)
        .processedWebhooks(processedWebhooks)
        .pendingWebhooks(pendingWebhooks)
        .failedWebhooks(failedWebhooks)
        .webhookTypes(webhookTypes)
        .processingRate(processingRate)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public WebhookResponse.WebhookStatistics getWebhookStatisticsByCarrier(Integer carrierId,
      TAccountRequest accountRequest) {
    log.info("Getting webhook statistics for carrier ID: {} for account: {}", carrierId, accountRequest.username());

    // Validate carrier exists
    if (!carrierRepository.existsById(carrierId)) {
      throw new ResourceNotFoundException("Carrier not found with ID: " + carrierId);
    }

    List<EShippingWebhook> carrierWebhooks = webhookRepository.findByCarrierCarrierId(carrierId);

    long totalWebhooks = carrierWebhooks.size();
    long processedWebhooks = carrierWebhooks.stream()
        .filter(EShippingWebhook::getProcessed)
        .count();
    long pendingWebhooks = totalWebhooks - processedWebhooks;
    long failedWebhooks = 0; // Simplified

    List<String> webhookTypes = carrierWebhooks.stream()
        .map(EShippingWebhook::getWebhookType)
        .distinct()
        .collect(Collectors.toList());

    double processingRate = totalWebhooks > 0 ? (double) processedWebhooks / totalWebhooks : 0.0;

    return WebhookResponse.WebhookStatistics.builder()
        .totalWebhooks(totalWebhooks)
        .processedWebhooks(processedWebhooks)
        .pendingWebhooks(pendingWebhooks)
        .failedWebhooks(failedWebhooks)
        .webhookTypes(webhookTypes)
        .processingRate(processingRate)
        .build();
  }

  private void validateCreateWebhookRequest(CreateWebhookRequest request) {
    ShippingValidationUtils.validateNotNull(request, "Webhook request cannot be null");
    ShippingValidationUtils.validateNotNull(request.getCarrierId(), "Carrier ID cannot be null");
    ShippingValidationUtils.validateNotBlank(request.getWebhookType(), "Webhook type cannot be blank");
    ShippingValidationUtils.validateNotBlank(request.getWebhookData(), "Webhook data cannot be blank");
  }

  private void validateWebhookData(String webhookData) {
    try {
      objectMapper.readValue(webhookData, Object.class);
    } catch (JsonProcessingException e) {
      throw new BadRequestException("Webhook data must be valid JSON: " + e.getMessage());
    }
  }

  private void processWebhookData(EShippingWebhook webhook) {
    // Process webhook data based on type
    String webhookType = webhook.getWebhookType();
    String webhookData = webhook.getWebhookData();

    try {
      Map<String, Object> data = objectMapper.readValue(webhookData, Map.class);

      switch (webhookType.toUpperCase()) {
        case "SHIPMENT_UPDATE":
          processShipmentUpdate(webhook, data);
          break;
        case "TRACKING_UPDATE":
          processTrackingUpdate(webhook, data);
          break;
        case "DELIVERY_STATUS":
          processDeliveryStatus(webhook, data);
          break;
        case "EXCEPTION":
          processException(webhook, data);
          break;
        default:
          log.warn("Unknown webhook type: {}", webhookType);
          break;
      }

    } catch (JsonProcessingException e) {
      throw new BusinessLogicException("Failed to parse webhook data: " + e.getMessage());
    }
  }

  private void processShipmentUpdate(EShippingWebhook webhook, Map<String, Object> data) {
    log.info("Processing shipment update webhook for webhook ID: {}", webhook.getWebhookId());

    // Extract shipment information and update accordingly
    if (webhook.getShipment() != null) {
      EShipment shipment = webhook.getShipment();

      // Update shipment status if provided
      if (data.containsKey("status")) {
        String statusString = (String) data.get("status");
        try {
          ShipmentStatus status = ShipmentStatus.valueOf(statusString.toUpperCase());
          shipment.setStatus(status);
        } catch (IllegalArgumentException e) {
          log.warn("Invalid shipment status received in webhook: {}", statusString);
        }
      }

      // Update tracking number if provided
      if (data.containsKey("tracking_number")) {
        shipment.setTrackingNumber((String) data.get("tracking_number"));
      }

      shipmentRepository.save(shipment);
      log.info("Updated shipment ID: {} from webhook", shipment.getShipmentId());
    }
  }

  private void processTrackingUpdate(EShippingWebhook webhook, Map<String, Object> data) {
    log.info("Processing tracking update webhook for webhook ID: {}", webhook.getWebhookId());

    // This would typically create tracking events
    // For now, just log the processing
    log.info("Tracking update processed for tracking number: {}", webhook.getTrackingNumber());
  }

  private void processDeliveryStatus(EShippingWebhook webhook, Map<String, Object> data) {
    log.info("Processing delivery status webhook for webhook ID: {}", webhook.getWebhookId());

    if (webhook.getShipment() != null) {
      EShipment shipment = webhook.getShipment();

      // Update delivery status
      if (data.containsKey("delivered") && Boolean.TRUE.equals(data.get("delivered"))) {
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredDate(Instant.now());
        shipmentRepository.save(shipment);
        log.info("Marked shipment ID: {} as delivered", shipment.getShipmentId());
      }
    }
  }

  private void processException(EShippingWebhook webhook, Map<String, Object> data) {
    log.info("Processing exception webhook for webhook ID: {}", webhook.getWebhookId());

    if (webhook.getShipment() != null) {
      EShipment shipment = webhook.getShipment();
      shipment.setStatus(ShipmentStatus.FAILED);
      shipmentRepository.save(shipment);
      log.info("Marked shipment ID: {} as exception", shipment.getShipmentId());
    }
  }

  private WebhookResponse mapToWebhookResponse(EShippingWebhook webhook) {
    WebhookResponse.WebhookResponseBuilder builder = WebhookResponse.builder()
        .webhookId(webhook.getWebhookId())
        .carrierId(webhook.getCarrier().getCarrierId())
        .carrierName(webhook.getCarrier().getCarrierName())
        .carrierCode(webhook.getCarrier().getCarrierCode())
        .webhookType(webhook.getWebhookType())
        .trackingNumber(webhook.getTrackingNumber())
        .webhookData(webhook.getWebhookData())
        .processed(webhook.getProcessed())
        .receivedAt(webhook.getReceivedAt())
        .processedAt(webhook.getProcessedAt())
        .retryCount(0); // Simplified - would need additional field

    if (webhook.getShipment() != null) {
      builder.shipmentId(webhook.getShipment().getShipmentId());
    }

    return builder.build();
  }
}
