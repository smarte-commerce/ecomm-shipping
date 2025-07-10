package com.winnguyen1905.shipping.core.service.impl;

import com.winnguyen1905.shipping.core.feign.client.NotificationServiceClient;
import com.winnguyen1905.shipping.core.feign.client.OrderServiceClient;
import com.winnguyen1905.shipping.core.feign.client.CustomerServiceClient;
import com.winnguyen1905.shipping.core.feign.dto.CustomerDto;
import com.winnguyen1905.shipping.core.feign.dto.OrderDto;
import com.winnguyen1905.shipping.core.model.request.CreateTrackingEventRequest;
import com.winnguyen1905.shipping.core.model.response.TrackingEventResponse;
import com.winnguyen1905.shipping.core.service.TrackingService;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;
import com.winnguyen1905.shipping.exception.BadRequestException;
import com.winnguyen1905.shipping.exception.BusinessLogicException;
import com.winnguyen1905.shipping.exception.ResourceNotFoundException;
import com.winnguyen1905.shipping.persistance.entity.EShipmentTrackingEvent;
import com.winnguyen1905.shipping.persistance.entity.EShipment;
import com.winnguyen1905.shipping.persistance.repository.ShipmentTrackingEventRepository;
import com.winnguyen1905.shipping.persistance.repository.ShipmentRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrackingServiceImpl implements TrackingService {

    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final ShipmentRepository shipmentRepository;
    
    // Feign clients for external service integration
    private final NotificationServiceClient notificationServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final CustomerServiceClient customerServiceClient;

    @Override
    public TrackingEventResponse createTrackingEvent(CreateTrackingEventRequest request, TAccountRequest accountRequest) {
        log.info("Creating tracking event for shipment ID: {} with tracking number: {} for account: {}", 
                request.getShipmentId(), request.getTrackingNumber(), accountRequest.username());
        
        // Validate request
        validateCreateTrackingEventRequest(request);
        
        // Validate shipment exists
        EShipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + request.getShipmentId()));
        
        // Validate tracking number matches shipment
        if (!shipment.getTrackingNumber().equals(request.getTrackingNumber())) {
            throw new BadRequestException("Tracking number does not match shipment");
        }
        
        // Create tracking event entity
        EShipmentTrackingEvent trackingEvent = EShipmentTrackingEvent.builder()
                .shipment(shipment)
                .trackingNumber(request.getTrackingNumber())
                .eventType(request.getEventType())
                .eventDescription(request.getEventDescription())
                .eventLocation(request.getEventLocation())
                .eventTimestamp(request.getEventTimestamp())
                .carrierEventCode(request.getCarrierEventCode())
                .build();
        
        trackingEvent = trackingEventRepository.save(trackingEvent);
        
        // Update shipment status based on tracking event
        updateShipmentStatusFromTrackingEvent(shipment, request.getEventType(), request.getEventTimestamp());
        
        // Send tracking notification to customer
        sendTrackingNotification(shipment, trackingEvent);
        
        log.info("Tracking event created successfully with ID: {}", trackingEvent.getEventId());
        return mapToTrackingEventResponse(trackingEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingEventResponse getTrackingEventById(Long id, TAccountRequest accountRequest) {
        log.info("Getting tracking event by ID: {} for account: {}", id, accountRequest.username());
        
        EShipmentTrackingEvent trackingEvent = trackingEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking event not found with ID: " + id));
        
        return mapToTrackingEventResponse(trackingEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrackingEventResponse> getTrackingEvents(Long shipmentId, String trackingNumber, String eventType, 
                                                       Pageable pageable, TAccountRequest accountRequest) {
        log.info("Getting tracking events with filters for account: {}", accountRequest.username());
        
        // Build query based on filters
        List<EShipmentTrackingEvent> events = trackingEventRepository.findAll();
        
        // Apply filters
        if (shipmentId != null) {
            events = events.stream()
                    .filter(e -> e.getShipment().getShipmentId().equals(shipmentId))
                    .collect(Collectors.toList());
        }
        
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            events = events.stream()
                    .filter(e -> e.getTrackingNumber().equals(trackingNumber))
                    .collect(Collectors.toList());
        }
        
        if (eventType != null && !eventType.trim().isEmpty()) {
            events = events.stream()
                    .filter(e -> e.getEventType().equals(eventType))
                    .collect(Collectors.toList());
        }
        
        // Sort by event timestamp descending
        events.sort((e1, e2) -> e2.getEventTimestamp().compareTo(e1.getEventTimestamp()));
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), events.size());
        
        List<TrackingEventResponse> responses = events.subList(start, end).stream()
                .map(this::mapToTrackingEventResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, events.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackingEventResponse> getTrackingEventsByShipment(Long shipmentId, TAccountRequest accountRequest) {
        log.info("Getting tracking events for shipment ID: {} for account: {}", shipmentId, accountRequest.username());
        
        // Validate shipment exists
        if (!shipmentRepository.existsById(shipmentId)) {
            throw new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
        }
        
        List<EShipmentTrackingEvent> events = trackingEventRepository.findByShipmentIdOrderByEventTimestampDesc(shipmentId);
        
        return events.stream()
                .map(this::mapToTrackingEventResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackingEventResponse> getTrackingEventsByNumber(String trackingNumber) {
        log.info("Getting tracking events for tracking number: {}", trackingNumber);
        
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new BadRequestException("Tracking number cannot be null or empty");
        }
        
        List<EShipmentTrackingEvent> events = trackingEventRepository.findByTrackingNumberOrderByEventTimestampDesc(trackingNumber);
        
        return events.stream()
                .map(this::mapToTrackingEventResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingEventResponse.TrackingStatus getTrackingStatus(String trackingNumber) {
        log.info("Getting tracking status for tracking number: {}", trackingNumber);
        
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new BadRequestException("Tracking number cannot be null or empty");
        }
        
        List<EShipmentTrackingEvent> events = trackingEventRepository.findByTrackingNumberOrderByEventTimestampDesc(trackingNumber);
        
        if (events.isEmpty()) {
            throw new ResourceNotFoundException("No tracking events found for tracking number: " + trackingNumber);
        }
        
        // Get the latest event for current status
        EShipmentTrackingEvent latestEvent = events.get(0);
        
        // Get shipment details
        EShipment shipment = latestEvent.getShipment();
        
        // Determine delivery status
        String deliveryStatus = determineDeliveryStatus(events);
        
        // Calculate estimated delivery (simplified logic)
        String estimatedDelivery = calculateEstimatedDelivery(shipment, events);
        
        return TrackingEventResponse.TrackingStatus.builder()
                .trackingNumber(trackingNumber)
                .currentStatus(latestEvent.getEventType())
                .currentLocation(latestEvent.getEventLocation())
                .lastUpdated(latestEvent.getEventTimestamp())
                .deliveryStatus(deliveryStatus)
                .estimatedDelivery(estimatedDelivery)
                .totalEvents(events.size())
                .carrierName(shipment.getCarrier().getCarrierName())
                .serviceType(shipment.getMethod().getServiceType().name())
                .build();
    }

    @Override
    public List<TrackingEventResponse> refreshTrackingInfo(String trackingNumber, TAccountRequest accountRequest) {
        log.info("Refreshing tracking info for tracking number: {} for account: {}", 
                trackingNumber, accountRequest.username());
        
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new BadRequestException("Tracking number cannot be null or empty");
        }
        
        // Check if tracking number exists
        List<EShipmentTrackingEvent> existingEvents = trackingEventRepository.findByTrackingNumberOrderByEventTimestampDesc(trackingNumber);
        if (existingEvents.isEmpty()) {
            throw new ResourceNotFoundException("No tracking events found for tracking number: " + trackingNumber);
        }
        
        // Simulate carrier API call to get latest tracking info
        List<EShipmentTrackingEvent> newEvents = simulateCarrierTrackingUpdate(existingEvents.get(0).getShipment(), trackingNumber);
        
        // Save new events
        List<EShipmentTrackingEvent> savedEvents = trackingEventRepository.saveAll(newEvents);
        
        log.info("Refreshed tracking info for tracking number: {}, added {} new events", 
                trackingNumber, savedEvents.size());
        
        return savedEvents.stream()
                .map(this::mapToTrackingEventResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TrackingEventResponse.BatchRefreshResponse batchRefreshTrackingInfo(List<String> trackingNumbers, TAccountRequest accountRequest) {
        log.info("Batch refreshing tracking info for {} tracking numbers for account: {}", 
                trackingNumbers.size(), accountRequest.username());
        
        if (trackingNumbers == null || trackingNumbers.isEmpty()) {
            throw new BadRequestException("Tracking numbers list cannot be null or empty");
        }
        
        List<TrackingEventResponse.BatchRefreshResponse.RefreshResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;
        
        for (String trackingNumber : trackingNumbers) {
            try {
                List<TrackingEventResponse> newEvents = refreshTrackingInfo(trackingNumber, accountRequest);
                
                results.add(TrackingEventResponse.BatchRefreshResponse.RefreshResult.builder()
                        .trackingNumber(trackingNumber)
                        .isSuccessful(true)
                        .newEventsCount(newEvents.size())
                        .build());
                successful++;
                
            } catch (Exception e) {
                log.error("Failed to refresh tracking info for tracking number: {}, error: {}", 
                        trackingNumber, e.getMessage());
                
                results.add(TrackingEventResponse.BatchRefreshResponse.RefreshResult.builder()
                        .trackingNumber(trackingNumber)
                        .isSuccessful(false)
                        .newEventsCount(0)
                        .errorMessage(e.getMessage())
                        .build());
                failed++;
            }
        }
        
        return TrackingEventResponse.BatchRefreshResponse.builder()
                .totalRequested(trackingNumbers.size())
                .successfulRefreshes(successful)
                .failedRefreshes(failed)
                .refreshResults(results)
                .processedAt(Instant.now())
                .build();
    }

    private void validateCreateTrackingEventRequest(CreateTrackingEventRequest request) {
        ShippingValidationUtils.validateNotNull(request, "Tracking event request cannot be null");
        ShippingValidationUtils.validateNotNull(request.getShipmentId(), "Shipment ID cannot be null");
        ShippingValidationUtils.validateNotBlank(request.getTrackingNumber(), "Tracking number cannot be blank");
        ShippingValidationUtils.validateNotBlank(request.getEventType(), "Event type cannot be blank");
        ShippingValidationUtils.validateNotNull(request.getEventTimestamp(), "Event timestamp cannot be null");
        
        // Validate event timestamp is not in the future
        if (request.getEventTimestamp().isAfter(Instant.now())) {
            throw new BadRequestException("Event timestamp cannot be in the future");
        }
    }

    private void updateShipmentStatusFromTrackingEvent(EShipment shipment, String eventType, Instant eventTimestamp) {
        // Update shipment status based on tracking event type
        switch (eventType.toUpperCase()) {
            case "PICKED_UP":
                shipment.setStatus(ShipmentStatus.PICKED_UP);
                shipment.setPickupDate(eventTimestamp);
                break;
            case "IN_TRANSIT":
                shipment.setStatus(ShipmentStatus.IN_TRANSIT);
                break;
            case "OUT_FOR_DELIVERY":
                shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
                break;
            case "DELIVERED":
                shipment.setStatus(ShipmentStatus.DELIVERED);
                shipment.setDeliveredDate(eventTimestamp);
                break;
            case "EXCEPTION":
                shipment.setStatus(ShipmentStatus.FAILED);
                break;
            case "RETURNED":
                shipment.setStatus(ShipmentStatus.RETURNED);
                break;
            default:
                // Don't update status for unknown event types
                break;
        }
        
        shipmentRepository.save(shipment);
    }

    private String determineDeliveryStatus(List<EShipmentTrackingEvent> events) {
        // Check if any event indicates delivery
        boolean hasDelivered = events.stream()
                .anyMatch(e -> "DELIVERED".equals(e.getEventType()));
        
        if (hasDelivered) {
            return "DELIVERED";
        }
        
        // Check if any event indicates exception
        boolean hasException = events.stream()
                .anyMatch(e -> "EXCEPTION".equals(e.getEventType()));
        
        if (hasException) {
            return "EXCEPTION";
        }
        
        // Check if any event indicates return
        boolean hasReturned = events.stream()
                .anyMatch(e -> "RETURNED".equals(e.getEventType()));
        
        if (hasReturned) {
            return "RETURNED";
        }
        
        // Check current status from latest event
        String latestEventType = events.get(0).getEventType();
        
        switch (latestEventType.toUpperCase()) {
            case "OUT_FOR_DELIVERY":
                return "OUT_FOR_DELIVERY";
            case "IN_TRANSIT":
                return "IN_TRANSIT";
            case "PICKED_UP":
                return "PICKED_UP";
            default:
                return "PENDING";
        }
    }

    private String calculateEstimatedDelivery(EShipment shipment, List<EShipmentTrackingEvent> events) {
        // Simple estimation based on shipping method delivery days
        if (shipment.getMethod() != null) {
            Integer maxDays = shipment.getMethod().getEstimatedDaysMax();
            if (maxDays != null && shipment.getCreatedAt() != null) {
                Instant estimatedDelivery = shipment.getCreatedAt().plusSeconds(maxDays * 24 * 60 * 60);
                return estimatedDelivery.toString();
            }
        }
        
        // If no method info, estimate based on latest event + 2 days
        if (!events.isEmpty()) {
            Instant lastEventTime = events.get(0).getEventTimestamp();
            Instant estimatedDelivery = lastEventTime.plusSeconds(2 * 24 * 60 * 60);
            return estimatedDelivery.toString();
        }
        
        return "Unknown";
    }

    private List<EShipmentTrackingEvent> simulateCarrierTrackingUpdate(EShipment shipment, String trackingNumber) {
        // Simulate getting new tracking events from carrier API
        List<EShipmentTrackingEvent> newEvents = new ArrayList<>();
        
        // Get existing events to avoid duplicates
        List<EShipmentTrackingEvent> existingEvents = trackingEventRepository.findByTrackingNumberOrderByEventTimestampDesc(trackingNumber);
        
        // Simulate adding a new event if the shipment is still in transit
        if (existingEvents.isEmpty() || 
            !existingEvents.get(0).getEventType().equals("DELIVERED")) {
            
            // Create a simulated progress event
            EShipmentTrackingEvent newEvent = EShipmentTrackingEvent.builder()
                    .shipment(shipment)
                    .trackingNumber(trackingNumber)
                    .eventType("IN_TRANSIT")
                    .eventDescription("Package is in transit - updated location")
                    .eventLocation("Transit Hub - " + System.currentTimeMillis())
                    .eventTimestamp(Instant.now())
                    .carrierEventCode("TU_" + System.currentTimeMillis())
                    .build();
            
            newEvents.add(newEvent);
        }
        
        return newEvents;
    }

    private TrackingEventResponse mapToTrackingEventResponse(EShipmentTrackingEvent event) {
        return TrackingEventResponse.builder()
                .eventId(event.getEventId())
                .shipmentId(event.getShipment().getShipmentId())
                .trackingNumber(event.getTrackingNumber())
                .eventType(event.getEventType())
                .eventDescription(event.getEventDescription())
                .eventLocation(event.getEventLocation())
                .eventTimestamp(event.getEventTimestamp())
                .carrierEventCode(event.getCarrierEventCode())
                .createdAt(event.getCreatedAt())
                .build();
    }
    
    private void sendTrackingNotification(EShipment shipment, EShipmentTrackingEvent trackingEvent) {
        try {
            log.debug("Sending tracking notification for shipment: {} event: {}", 
                shipment.getShipmentNumber(), trackingEvent.getEventType());
            
            // Get order information to determine customer
            OrderDto order = getOrderInfo(shipment.getOrderId());
            if (order == null) {
                log.warn("Could not get order info for shipment: {}", shipment.getShipmentNumber());
                return;
            }
            
            // Get customer preferences
            CustomerDto customer = getCustomerInfo(order.getCustomerId());
            
            // Create tracking notification based on event type
            String eventDescription = createTrackingEventDescription(trackingEvent);
            
            NotificationServiceClient.TrackingNotificationRequest notificationRequest = 
                new NotificationServiceClient.TrackingNotificationRequest(
                    order.getCustomerId().toString(),
                    customer != null ? customer.getEmail() : null,
                    customer != null ? customer.getPhoneNumber() : null,
                    determineNotificationTypeFromEvent(trackingEvent.getEventType()),
                    shipment.getTrackingNumber(),
                    trackingEvent.getEventType(),
                    eventDescription,
                    trackingEvent.getEventLocation(),
                    trackingEvent.getEventTimestamp().toString(),
                    java.util.Map.of(
                        "shipmentNumber", shipment.getShipmentNumber(),
                        "carrierName", shipment.getCarrier().getCarrierName(),
                        "orderNumber", order.getOrderNumber()
                    )
                );
            
            notificationServiceClient.sendTrackingUpdateNotification(notificationRequest);
            
            log.debug("Tracking notification sent successfully for shipment: {}", shipment.getShipmentNumber());
            
        } catch (Exception e) {
            log.warn("Failed to send tracking notification for shipment: {}", shipment.getShipmentNumber(), e);
            // Don't fail the process for notification failures
        }
    }
    
    private OrderDto getOrderInfo(Long orderId) {
        try {
            return orderServiceClient.getOrderById(orderId).getBody();
        } catch (Exception e) {
            log.warn("Failed to get order info for order ID: {}", orderId, e);
            return null;
        }
    }
    
    private CustomerDto getCustomerInfo(Long customerId) {
        try {
            return customerServiceClient.getCustomerById(customerId).getBody();
        } catch (Exception e) {
            log.warn("Failed to get customer info for customer ID: {}", customerId, e);
            return null;
        }
    }
    
    private String createTrackingEventDescription(EShipmentTrackingEvent event) {
        switch (event.getEventType().toUpperCase()) {
            case "PICKED_UP":
                return "Your package has been picked up and is on its way.";
            case "IN_TRANSIT":
                return "Your package is in transit and making progress toward its destination.";
            case "OUT_FOR_DELIVERY":
                return "Your package is out for delivery and should arrive today.";
            case "DELIVERED":
                return "Your package has been delivered successfully.";
            case "EXCEPTION":
                return "There was an issue with your package delivery. Please check tracking for details.";
            case "RETURNED":
                return "Your package is being returned to the sender.";
            default:
                return event.getEventDescription();
        }
    }
    
    private String determineNotificationTypeFromEvent(String eventType) {
        switch (eventType.toUpperCase()) {
            case "DELIVERED":
                return "EMAIL";
            case "EXCEPTION":
                return "EMAIL";
            case "OUT_FOR_DELIVERY":
                return "SMS";
            default:
                return "EMAIL";
        }
    }
} 
