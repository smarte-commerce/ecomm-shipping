package com.winnguyen1905.shipping.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.shipping.core.feign.client.*;
import com.winnguyen1905.shipping.core.feign.dto.*;
import com.winnguyen1905.shipping.core.model.request.CreateShipmentRequest;
import com.winnguyen1905.shipping.core.model.request.UpdateShipmentRequest;
import com.winnguyen1905.shipping.core.model.response.ShipmentResponse;
import com.winnguyen1905.shipping.core.service.ShipmentService;
import com.winnguyen1905.shipping.exception.BadRequestException;
import com.winnguyen1905.shipping.exception.BusinessLogicException;
import com.winnguyen1905.shipping.exception.ResourceNotFoundException;
import com.winnguyen1905.shipping.persistance.entity.*;
import com.winnguyen1905.shipping.persistance.repository.*;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;
import com.winnguyen1905.shipping.common.constants.ShippingConstants;
import com.winnguyen1905.shipping.secure.TAccountRequest;
import com.winnguyen1905.shipping.util.ShippingValidationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShippingCarrierRepository carrierRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final ShipmentPackageRepository shipmentPackageRepository;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final ObjectMapper objectMapper;
    
    // Feign clients for external service integration
    private final OrderServiceClient orderServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request, TAccountRequest accountRequest) {
        log.info("Creating shipment for order: {} for account: {}", request.getOrderId(), accountRequest.username());
        
        // Validate request
        validateCreateShipmentRequest(request);
        
        // 1. Validate order exists and is ready for shipping
        OrderDto order = validateAndGetOrder(request.getOrderId());
        
        // 2. Validate customer exists and get shipping preferences
        CustomerDto customer = validateAndGetCustomer(order.getCustomerId());
        
        // 3. Validate products and check inventory
        validateProductsAndInventory(order);
        
        // Check if carrier exists and is active
        EShippingCarrier carrier = carrierRepository.findById(request.getCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + request.getCarrierId()));
        
        if (!carrier.getIsActive()) {
            throw new BadRequestException("Cannot create shipment with inactive carrier");
        }
        
        // Check if shipping method exists and is active
        EShippingMethod shippingMethod = shippingMethodRepository.findById(request.getMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + request.getMethodId()));
        
        if (!shippingMethod.getIsActive()) {
            throw new BadRequestException("Cannot create shipment with inactive shipping method");
        }
        
        // Verify carrier and method compatibility
        if (!shippingMethod.getCarrier().getCarrierId().equals(request.getCarrierId())) {
            throw new BadRequestException("Shipping method does not belong to the specified carrier");
        }
        
        // Calculate shipping cost based on weight, value, and method
        BigDecimal shippingCost = calculateShippingCost(shippingMethod, request);
        
        // Generate unique shipment number
        String shipmentNumber = generateShipmentNumber();
        
        // Create shipment entity
        EShipment shipment = EShipment.builder()
                .orderId(request.getOrderId())
                .shipmentNumber(shipmentNumber)
                .carrier(carrier)
                .method(shippingMethod)
                .fromAddress(convertAddressToJson(request.getFromAddress()))
                .toAddress(convertAddressToJson(request.getToAddress()))
                .packageCount(request.getShipmentPackages() != null ? request.getShipmentPackages().size() : 1)
                .totalWeight(request.getTotalWeight())
                .totalValue(request.getTotalValue())
                .shippingCost(shippingCost)
                .insuranceCost(request.getInsuranceCost() != null ? request.getInsuranceCost() : 
                        calculateInsuranceCost(request.getTotalValue()))
                .estimatedDeliveryDate(request.getEstimatedDeliveryDate() != null ? 
                        request.getEstimatedDeliveryDate() : calculateEstimatedDeliveryDate(shippingMethod))
                .deliveryNotes(request.getDeliveryNotes())
                .status(ShipmentStatus.PENDING)
                .build();
        
        shipment = shipmentRepository.save(shipment);
        
        // Create shipment items
        if (request.getShipmentItems() != null && !request.getShipmentItems().isEmpty()) {
            createShipmentItems(shipment, request.getShipmentItems());
        }
        
        // Create shipment packages
        if (request.getShipmentPackages() != null && !request.getShipmentPackages().isEmpty()) {
            createShipmentPackages(shipment, request.getShipmentPackages());
        }
        
        // Send shipment creation notification
        sendShipmentNotification(shipment, "SHIPMENT_CREATED", 
            "Your shipment has been created and is being prepared for delivery.");
        
        log.info("Shipment created successfully with ID: {} and number: {}", shipment.getShipmentId(), shipmentNumber);
        return mapToShipmentResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long id, TAccountRequest accountRequest) {
        log.info("Getting shipment by ID: {} for account: {}", id, accountRequest.username());
        
        EShipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        return mapToShipmentResponse(shipment);
    }

    @Override
    public ShipmentResponse updateShipment(Long id, UpdateShipmentRequest request, TAccountRequest accountRequest) {
        log.info("Updating shipment ID: {} for account: {}", id, accountRequest.username());
        
        EShipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        // Validate status transition if status is being updated
        if (request.getStatus() != null) {
            if (!ShippingValidationUtils.isValidStatusTransition(shipment.getStatus(), request.getStatus())) {
                throw new BadRequestException("Invalid status transition from " + shipment.getStatus() + " to " + request.getStatus());
            }
        }
        
        // Update shipment fields
        if (request.getTrackingNumber() != null) {
            if (shipmentRepository.existsByTrackingNumber(request.getTrackingNumber()) &&
                !request.getTrackingNumber().equals(shipment.getTrackingNumber())) {
                throw new BadRequestException("Tracking number already exists: " + request.getTrackingNumber());
            }
            shipment.setTrackingNumber(request.getTrackingNumber());
        }
        
        if (request.getShippingLabelUrl() != null) {
            shipment.setShippingLabelUrl(request.getShippingLabelUrl());
        }
        
        if (request.getToAddress() != null) {
            shipment.setToAddress(convertAddressToJson(request.getToAddress()));
        }
        
        if (request.getInsuranceCost() != null) {
            shipment.setInsuranceCost(request.getInsuranceCost());
        }
        
        if (request.getStatus() != null) {
            updateShipmentStatus(shipment, request.getStatus());
        }
        
        if (request.getEstimatedDeliveryDate() != null) {
            shipment.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        }
        
        if (request.getActualDeliveryDate() != null) {
            shipment.setActualDeliveryDate(request.getActualDeliveryDate());
            if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
                updateShipmentStatus(shipment, ShipmentStatus.DELIVERED);
            }
        }
        
        if (request.getDeliverySignature() != null) {
            shipment.setDeliverySignature(request.getDeliverySignature());
        }
        
        if (request.getDeliveryNotes() != null) {
            shipment.setDeliveryNotes(request.getDeliveryNotes());
        }
        
        shipment = shipmentRepository.save(shipment);
        
        log.info("Shipment updated successfully with ID: {}", id);
        return mapToShipmentResponse(shipment);
    }

    @Override
    public void cancelShipment(Long id, TAccountRequest accountRequest) {
        log.info("Cancelling shipment ID: {} for account: {}", id, accountRequest.username());
        
        EShipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        // Check if shipment can be cancelled
        if (shipment.getStatus().isFinalStatus()) {
            throw new BusinessLogicException("Cannot cancel shipment with final status: " + shipment.getStatus());
        }
        
        if (shipment.getStatus() == ShipmentStatus.IN_TRANSIT || shipment.getStatus() == ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new BusinessLogicException("Cannot cancel shipment that is already in transit");
        }
        
        // Process cancellation with external services
        try {
            // 1. Update order status to cancelled
            OrderServiceClient.UpdateOrderStatusRequest statusRequest = 
                new OrderServiceClient.UpdateOrderStatusRequest(
                    "CANCELLED", 
                    "Shipment cancelled by request", 
                    "Shipping cancelled by " + accountRequest.username()
                );
            orderServiceClient.updateOrderStatus(shipment.getOrderId(), statusRequest);
            
            // 2. Release inventory reservations
            releaseInventoryReservations(shipment);
            
            // 3. Process refund for shipping costs if payment was made
            processShippingRefund(shipment, "Shipment cancellation");
            
        } catch (Exception e) {
            log.warn("Some external services failed during cancellation, but proceeding with shipment cancellation: {}", e.getMessage());
        }
        
        updateShipmentStatus(shipment, ShipmentStatus.CANCELLED);
        shipmentRepository.save(shipment);
        
        // Send cancellation notification
        sendShipmentNotification(shipment, "SHIPMENT_CANCELLED", 
            "Your shipment has been cancelled as requested.");
        
        log.info("Shipment cancelled successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getAllShipments(Long orderId, ShipmentStatus status, Integer carrierId,
                                                 String trackingNumber, Pageable pageable, TAccountRequest accountRequest) {
        log.info("Getting all shipments with filters for account: {}", accountRequest.username());
        
        List<EShipment> shipments;
        
        // Apply filters
        if (orderId != null) {
            shipments = shipmentRepository.findByOrderId(orderId);
        } else if (status != null) {
            shipments = shipmentRepository.findByStatus(status);
        } else if (carrierId != null) {
            shipments = shipmentRepository.findByCarrierCarrierId(carrierId);
        } else if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            EShipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber).orElse(null);
            shipments = shipment != null ? List.of(shipment) : List.of();
        } else {
            shipments = shipmentRepository.findAll();
        }
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), shipments.size());
        
        List<ShipmentResponse> shipmentResponses = shipments.subList(start, end).stream()
                .map(this::mapToShipmentResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(shipmentResponses, pageable, shipments.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipmentsByOrderId(Long orderId, TAccountRequest accountRequest) {
        log.info("Getting shipments by order ID: {} for account: {}", orderId, accountRequest.username());
        
        List<EShipment> shipments = shipmentRepository.findByOrderId(orderId);
        
        return shipments.stream()
                .map(this::mapToShipmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse trackShipment(String trackingNumber) {
        log.info("Tracking shipment by tracking number: {}", trackingNumber);
        
        EShipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with tracking number: " + trackingNumber));
        
        return mapToShipmentResponse(shipment);
    }

    @Override
    public ShipmentResponse generateShippingLabel(Long id, TAccountRequest accountRequest) {
        log.info("Generating shipping label for shipment ID: {} for account: {}", id, accountRequest.username());
        
        EShipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        if (shipment.getStatus() != ShipmentStatus.PENDING) {
            throw new BusinessLogicException("Can only generate label for pending shipments");
        }
        
        // Generate tracking number if not exists
        if (shipment.getTrackingNumber() == null) {
            String trackingNumber = generateTrackingNumber(shipment.getCarrier());
            shipment.setTrackingNumber(trackingNumber);
        }
        
        // Simulate label generation (in real implementation, would call carrier API)
        String labelUrl = generateLabelUrl(shipment);
        shipment.setShippingLabelUrl(labelUrl);
        
        // Update status to label created
        updateShipmentStatus(shipment, ShipmentStatus.LABEL_CREATED);
        
        shipment = shipmentRepository.save(shipment);
        
        log.info("Shipping label generated successfully for shipment ID: {}", id);
        return mapToShipmentResponse(shipment);
    }

    @Override
    public ShipmentResponse markShipmentAsShipped(Long id, TAccountRequest accountRequest) {
        log.info("Marking shipment as shipped for ID: {} for account: {}", id, accountRequest.username());
        
        EShipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        if (shipment.getStatus() != ShipmentStatus.LABEL_CREATED) {
            throw new BusinessLogicException("Can only mark shipments with label created as shipped");
        }
        
        shipment.setShippedAt(Instant.now());
        updateShipmentStatus(shipment, ShipmentStatus.PICKED_UP);
        
        shipment = shipmentRepository.save(shipment);
        
        // Create initial tracking event
        createTrackingEvent(shipment, "PICKUP", "Package picked up by " + shipment.getCarrier().getCarrierName(), 
                "Origin facility");
        
        // Update order with shipping information
        updateOrderShippingInfo(shipment);
        
        // Send shipment notification
        sendShipmentNotification(shipment, "SHIPMENT_SHIPPED", 
            "Your shipment has been picked up by the carrier and is on its way to you.");
        
        log.info("Shipment marked as shipped successfully with ID: {}", id);
        return mapToShipmentResponse(shipment);
    }

    @Override
    public ShipmentResponse markShipmentAsDelivered(Long id, String signature, TAccountRequest accountRequest) {
        log.info("Marking shipment as delivered for ID: {} for account: {}", id, accountRequest.username());
        
        EShipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        if (!shipment.getStatus().isActiveStatus() && shipment.getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new BusinessLogicException("Can only mark active shipments as delivered");
        }
        
        shipment.setActualDeliveryDate(LocalDate.now());
        shipment.setDeliverySignature(signature);
        updateShipmentStatus(shipment, ShipmentStatus.DELIVERED);
        
        shipment = shipmentRepository.save(shipment);
        
        // Create delivery tracking event
        createTrackingEvent(shipment, "DELIVERED", "Package delivered successfully", "Destination");
        
        // Update order status to delivered
        try {
            OrderServiceClient.DeliverOrderRequest deliverRequest = 
                new OrderServiceClient.DeliverOrderRequest(
                    shipment.getTrackingNumber(),
                    signature,
                    shipment.getDeliveryNotes(),
                    Instant.now().toString()
                );
            orderServiceClient.markOrderAsDelivered(shipment.getOrderId(), deliverRequest);
        } catch (Exception e) {
            log.warn("Failed to update order delivery status: {}", e.getMessage());
        }
        
        // Send delivery notification
        sendShipmentNotification(shipment, "SHIPMENT_DELIVERED", 
            "Your shipment has been delivered successfully.");
        
        log.info("Shipment marked as delivered successfully with ID: {}", id);
        return mapToShipmentResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse.TrackingEventResponse> getShipmentTrackingEvents(Long id, TAccountRequest accountRequest) {
        log.info("Getting tracking events for shipment ID: {} for account: {}", id, accountRequest.username());
        
        EShipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + id));
        
        List<EShipmentTrackingEvent> events = trackingEventRepository.findByShipmentIdOrderByEventTimestampDesc(id);
        
        return events.stream()
                .map(this::mapToTrackingEventResponse)
                .collect(Collectors.toList());
    }

    private void validateCreateShipmentRequest(CreateShipmentRequest request) {
        if (request.getOrderId() == null) {
            throw new BadRequestException("Order ID is required");
        }
        if (request.getCarrierId() == null) {
            throw new BadRequestException("Carrier ID is required");
        }
        if (request.getMethodId() == null) {
            throw new BadRequestException("Shipping method ID is required");
        }
        if (request.getFromAddress() == null) {
            throw new BadRequestException("From address is required");
        }
        if (request.getToAddress() == null) {
            throw new BadRequestException("To address is required");
        }
        if (request.getTotalWeight() == null || request.getTotalWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Valid total weight is required");
        }
        if (request.getTotalValue() == null || request.getTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Valid total value is required");
        }
        if (!ShippingValidationUtils.isValidWeight(request.getTotalWeight())) {
            throw new BadRequestException("Total weight exceeds maximum allowed limit");
        }
        if (!ShippingValidationUtils.isValidDeclaredValue(request.getTotalValue())) {
            throw new BadRequestException("Total value exceeds maximum allowed limit");
        }
        if (request.getShipmentItems() == null || request.getShipmentItems().isEmpty()) {
            throw new BadRequestException("Shipment items are required");
        }
    }

    private BigDecimal calculateShippingCost(EShippingMethod method, CreateShipmentRequest request) {
        BigDecimal baseRate = method.getBaseRate();
        BigDecimal weightRate = method.getPerKgRate().multiply(request.getTotalWeight());
        BigDecimal itemRate = method.getPerItemRate().multiply(
                BigDecimal.valueOf(request.getShipmentItems().size()));
        
        return baseRate.add(weightRate).add(itemRate);
    }

    private BigDecimal calculateInsuranceCost(BigDecimal totalValue) {
        if (ShippingValidationUtils.requiresInsurance(totalValue)) {
            return totalValue.multiply(new BigDecimal("0.01")); // 1% of value
        }
        return ShippingConstants.DEFAULT_INSURANCE_COST;
    }

    private LocalDate calculateEstimatedDeliveryDate(EShippingMethod method) {
        return LocalDate.now().plusDays(method.getEstimatedDaysMax());
    }

    private String generateShipmentNumber() {
        return "SH" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateTrackingNumber(EShippingCarrier carrier) {
        return carrier.getCarrierCode() + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String generateLabelUrl(EShipment shipment) {
        return "https://labels.shipping.com/" + shipment.getShipmentNumber() + ".pdf";
    }

    private void updateShipmentStatus(EShipment shipment, ShipmentStatus newStatus) {
        ShipmentStatus oldStatus = shipment.getStatus();
        shipment.setStatus(newStatus);
        
        log.info("Shipment {} status changed from {} to {}", shipment.getShipmentNumber(), oldStatus, newStatus);
        
        // Create tracking event for status change
        if (shipment.getTrackingNumber() != null) {
            createTrackingEvent(shipment, newStatus.name(), newStatus.getDescription(), "System update");
        }
    }

    private void createShipmentItems(EShipment shipment, List<CreateShipmentRequest.ShipmentItemRequest> itemRequests) {
        for (CreateShipmentRequest.ShipmentItemRequest itemRequest : itemRequests) {
            BigDecimal totalWeight = itemRequest.getUnitWeight().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            
            EShipmentItem item = EShipmentItem.builder()
                    .shipment(shipment)
                    .orderItemId(itemRequest.getOrderItemId())
                    .productId(itemRequest.getProductId())
                    .productName(itemRequest.getProductName())
                    .productSku(itemRequest.getProductSku())
                    .quantity(itemRequest.getQuantity())
                    .unitWeight(itemRequest.getUnitWeight())
                    .totalWeight(totalWeight)
                    .dimensions(convertDimensionsToJson(itemRequest.getDimensions()))
                    .build();
            
            shipmentItemRepository.save(item);
        }
    }

    private void createShipmentPackages(EShipment shipment, List<CreateShipmentRequest.ShipmentPackageRequest> packageRequests) {
        int packageNumber = 1;
        for (CreateShipmentRequest.ShipmentPackageRequest packageRequest : packageRequests) {
            EShipmentPackage packageEntity = EShipmentPackage.builder()
                    .shipment(shipment)
                    .packageNumber(packageRequest.getPackageNumber() != null ? 
                            packageRequest.getPackageNumber() : "PKG-" + packageNumber++)
                    .weight(packageRequest.getWeight())
                    .dimensions(convertDimensionsToJson(packageRequest.getDimensions()))
                    .packageType(packageRequest.getPackageType() != null ? 
                            packageRequest.getPackageType() : ShippingConstants.DEFAULT_PACKAGE_TYPE)
                    .isFragile(packageRequest.getIsFragile() != null ? 
                            packageRequest.getIsFragile() : ShippingConstants.DEFAULT_IS_FRAGILE)
                    .isLiquid(packageRequest.getIsLiquid() != null ? 
                            packageRequest.getIsLiquid() : ShippingConstants.DEFAULT_IS_LIQUID)
                    .isHazardous(packageRequest.getIsHazardous() != null ? 
                            packageRequest.getIsHazardous() : ShippingConstants.DEFAULT_IS_HAZARDOUS)
                    .build();
            
            shipmentPackageRepository.save(packageEntity);
        }
    }

    private void createTrackingEvent(EShipment shipment, String eventType, String description, String location) {
        EShipmentTrackingEvent event = EShipmentTrackingEvent.builder()
                .shipment(shipment)
                .trackingNumber(shipment.getTrackingNumber())
                .eventType(eventType)
                .eventDescription(description)
                .eventLocation(location)
                .eventTimestamp(Instant.now())
                .carrierEventCode(eventType)
                .build();
        
        trackingEventRepository.save(event);
    }

    private ShipmentResponse mapToShipmentResponse(EShipment shipment) {
        // Load related entities if needed
        List<EShipmentItem> items = shipmentItemRepository.findByShipmentId(shipment.getShipmentId());
        List<EShipmentPackage> packages = shipmentPackageRepository.findByShipmentId(shipment.getShipmentId());
        List<EShipmentTrackingEvent> events = trackingEventRepository.findByShipmentIdOrderByEventTimestampDesc(shipment.getShipmentId());
        
        return ShipmentResponse.builder()
                .shipmentId(shipment.getShipmentId())
                .orderId(shipment.getOrderId())
                .shipmentNumber(shipment.getShipmentNumber())
                .trackingNumber(shipment.getTrackingNumber())
                .shippingLabelUrl(shipment.getShippingLabelUrl())
                .fromAddress(convertJsonToAddress(shipment.getFromAddress()))
                .toAddress(convertJsonToAddress(shipment.getToAddress()))
                .packageCount(shipment.getPackageCount())
                .totalWeight(shipment.getTotalWeight())
                .totalValue(shipment.getTotalValue())
                .shippingCost(shipment.getShippingCost())
                .insuranceCost(shipment.getInsuranceCost())
                .status(shipment.getStatus())
                .shippedAt(shipment.getShippedAt())
                .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                .actualDeliveryDate(shipment.getActualDeliveryDate())
                .deliverySignature(shipment.getDeliverySignature())
                .deliveryNotes(shipment.getDeliveryNotes())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .carrier(mapToCarrierResponse(shipment.getCarrier()))
                .shippingMethod(mapToShippingMethodResponse(shipment.getMethod()))
                .shipmentItems(items.stream().map(this::mapToShipmentItemResponse).collect(Collectors.toList()))
                .shipmentPackages(packages.stream().map(this::mapToShipmentPackageResponse).collect(Collectors.toList()))
                .trackingEvents(events.stream().map(this::mapToTrackingEventResponse).collect(Collectors.toList()))
                .build();
    }

    private ShipmentResponse.CarrierResponse mapToCarrierResponse(EShippingCarrier carrier) {
        return ShipmentResponse.CarrierResponse.builder()
                .carrierId(carrier.getCarrierId())
                .carrierName(carrier.getCarrierName())
                .carrierCode(carrier.getCarrierCode())
                .isActive(carrier.getIsActive())
                .build();
    }

    private ShipmentResponse.ShippingMethodResponse mapToShippingMethodResponse(EShippingMethod method) {
        return ShipmentResponse.ShippingMethodResponse.builder()
                .methodId(method.getMethodId())
                .methodName(method.getMethodName())
                .methodCode(method.getMethodCode())
                .serviceType(method.getServiceType().name())
                .baseRate(method.getBaseRate())
                .estimatedDaysMin(method.getEstimatedDaysMin())
                .estimatedDaysMax(method.getEstimatedDaysMax())
                .build();
    }

    private ShipmentResponse.ShipmentItemResponse mapToShipmentItemResponse(EShipmentItem item) {
        return ShipmentResponse.ShipmentItemResponse.builder()
                .shipmentItemId(item.getShipmentItemId())
                .orderItemId(item.getOrderItemId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQuantity())
                .unitWeight(item.getUnitWeight())
                .totalWeight(item.getTotalWeight())
                .dimensions(convertJsonToDimensions(item.getDimensions()))
                .build();
    }

    private ShipmentResponse.ShipmentPackageResponse mapToShipmentPackageResponse(EShipmentPackage packageEntity) {
        return ShipmentResponse.ShipmentPackageResponse.builder()
                .packageId(packageEntity.getPackageId())
                .packageNumber(packageEntity.getPackageNumber())
                .trackingNumber(packageEntity.getTrackingNumber())
                .weight(packageEntity.getWeight())
                .dimensions(convertJsonToDimensions(packageEntity.getDimensions()))
                .packageType(packageEntity.getPackageType())
                .isFragile(packageEntity.getIsFragile())
                .isLiquid(packageEntity.getIsLiquid())
                .isHazardous(packageEntity.getIsHazardous())
                .build();
    }

    private ShipmentResponse.TrackingEventResponse mapToTrackingEventResponse(EShipmentTrackingEvent event) {
        return ShipmentResponse.TrackingEventResponse.builder()
                .eventId(event.getEventId())
                .trackingNumber(event.getTrackingNumber())
                .eventType(event.getEventType())
                .eventDescription(event.getEventDescription())
                .eventLocation(event.getEventLocation())
                .eventTimestamp(event.getEventTimestamp())
                .carrierEventCode(event.getCarrierEventCode())
                .build();
    }

    private String convertAddressToJson(CreateShipmentRequest.AddressRequest address) {
        try {
            return objectMapper.writeValueAsString(address);
        } catch (JsonProcessingException e) {
            log.error("Error converting address to JSON", e);
            return "{}";
        }
    }

    private ShipmentResponse.AddressResponse convertJsonToAddress(String json) {
        if (json == null || json.trim().isEmpty()) {
            return ShipmentResponse.AddressResponse.builder().build();
        }
        try {
            CreateShipmentRequest.AddressRequest addr = objectMapper.readValue(json, CreateShipmentRequest.AddressRequest.class);
            return ShipmentResponse.AddressResponse.builder()
                    .addressLine1(addr.getAddressLine1())
                    .addressLine2(addr.getAddressLine2())
                    .city(addr.getCity())
                    .state(addr.getState())
                    .postalCode(addr.getPostalCode())
                    .country(addr.getCountry())
                    .companyName(addr.getCompanyName())
                    .contactName(addr.getContactName())
                    .phoneNumber(addr.getPhoneNumber())
                    .email(addr.getEmail())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to address", e);
            return ShipmentResponse.AddressResponse.builder().build();
        }
    }

    private String convertDimensionsToJson(CreateShipmentRequest.DimensionsRequest dimensions) {
        if (dimensions == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (JsonProcessingException e) {
            log.error("Error converting dimensions to JSON", e);
            return "{}";
        }
    }

    private ShipmentResponse.DimensionsResponse convertJsonToDimensions(String json) {
        if (json == null || json.trim().isEmpty()) {
            return ShipmentResponse.DimensionsResponse.builder().build();
        }
        try {
            CreateShipmentRequest.DimensionsRequest dims = objectMapper.readValue(json, CreateShipmentRequest.DimensionsRequest.class);
            return ShipmentResponse.DimensionsResponse.builder()
                    .length(dims.getLength())
                    .width(dims.getWidth())
                    .height(dims.getHeight())
                    .unit(dims.getUnit())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to dimensions", e);
            return ShipmentResponse.DimensionsResponse.builder().build();
        }
    }

    // External service integration methods
    
    private OrderDto validateAndGetOrder(Long orderId) {
        try {
            log.debug("Validating order with ID: {}", orderId);
            ResponseEntity<OrderDto> response = orderServiceClient.getOrderById(orderId);
            
            if (response.getBody() == null) {
                throw new ResourceNotFoundException("Order not found with ID: " + orderId);
            }
            
            OrderDto order = response.getBody();
            
            // Check if order is ready for shipping
            ResponseEntity<Boolean> readyResponse = orderServiceClient.isOrderReadyForShipping(orderId);
            if (!Boolean.TRUE.equals(readyResponse.getBody())) {
                throw new BadRequestException("Order is not ready for shipping: " + orderId);
            }
            
            log.debug("Order validation successful for ID: {}", orderId);
            return order;
            
        } catch (Exception e) {
            log.error("Failed to validate order with ID: {}", orderId, e);
            throw new BusinessLogicException("Unable to validate order: " + e.getMessage());
        }
    }
    
    private CustomerDto validateAndGetCustomer(Long customerId) {
        try {
            log.debug("Validating customer with ID: {}", customerId);
            ResponseEntity<CustomerDto> response = customerServiceClient.getCustomerById(customerId);
            
            if (response.getBody() == null) {
                throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
            }
            
            CustomerDto customer = response.getBody();
            
            // Validate customer for shipping
            ResponseEntity<CustomerServiceClient.CustomerValidationResult> validResponse = 
                customerServiceClient.validateCustomerForShipping(customerId);
            if (validResponse.getBody() == null || !validResponse.getBody().isValid()) {
                throw new BadRequestException("Customer is not valid for shipping: " + 
                    (validResponse.getBody() != null ? validResponse.getBody().validationErrors() : "Unknown error"));
            }
            
            log.debug("Customer validation successful for ID: {}", customerId);
            return customer;
            
        } catch (Exception e) {
            log.error("Failed to validate customer with ID: {}", customerId, e);
            throw new BusinessLogicException("Unable to validate customer: " + e.getMessage());
        }
    }
    
    private void validateProductsAndInventory(OrderDto order) {
        try {
            List<OrderDto.OrderItemDto> orderItems = order.getOrderItems();
            log.debug("Validating products and inventory for {} items", orderItems.size());
            
            for (OrderDto.OrderItemDto item : orderItems) {
                // Validate product exists and get shipping details
                ResponseEntity<ProductDto> productResponse = productServiceClient.getProductById(item.getProductId());
                if (productResponse.getBody() == null) {
                    throw new ResourceNotFoundException("Product not found with ID: " + item.getProductId());
                }
                
                ProductDto product = productResponse.getBody();
                
                // Check if product requires special handling
                ResponseEntity<List<ProductServiceClient.SpecialHandlingCheck>> specialHandlingResponse = 
                    productServiceClient.checkSpecialHandling(List.of(item.getProductId()));
                if (specialHandlingResponse.getBody() != null && !specialHandlingResponse.getBody().isEmpty()) {
                    ProductServiceClient.SpecialHandlingCheck check = specialHandlingResponse.getBody().get(0);
                    if (check.requiresSpecialHandling()) {
                        log.info("Product {} requires special handling: {}", item.getProductId(), check.handlingRequirements());
                    }
                }
                
                // Check inventory availability
                InventoryDto.AvailabilityRequest availabilityRequest = InventoryDto.AvailabilityRequest.builder()
                    .items(List.of(InventoryDto.AvailabilityRequest.AvailabilityItem.builder()
                        .productId(item.getProductId())
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .build()))
                    .build();
                
                ResponseEntity<InventoryDto.AvailabilityResponse> availabilityResponse = 
                    inventoryServiceClient.checkAvailability(availabilityRequest);
                
                if (availabilityResponse.getBody() == null || !availabilityResponse.getBody().getAllAvailable()) {
                    throw new BusinessLogicException("Insufficient inventory for product: " + item.getProductId());
                }
                
                // Reserve inventory for shipping
                InventoryDto.ReservationRequest reservationRequest = InventoryDto.ReservationRequest.builder()
                    .orderId(order.getOrderId()) // Get order ID from order
                    .items(List.of(InventoryDto.ReservationRequest.ReservationItem.builder()
                        .productId(item.getProductId())
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .build()))
                    .reservationTimeoutMinutes(60) // 1 hour timeout
                    .notes("Inventory reservation for shipment")
                    .build();
                
                ResponseEntity<InventoryDto.ReservationResponse> reservationResponse = 
                    inventoryServiceClient.reserveInventory(reservationRequest);
                
                if (reservationResponse.getBody() == null || 
                    !"CONFIRMED".equals(reservationResponse.getBody().getReservationStatus())) {
                    throw new BusinessLogicException("Failed to reserve inventory for product: " + item.getProductId());
                }
                
                log.debug("Successfully validated and reserved inventory for product: {}", item.getProductId());
            }
            
        } catch (Exception e) {
            log.error("Failed to validate products and inventory", e);
            throw new BusinessLogicException("Product/inventory validation failed: " + e.getMessage());
        }
    }
    
    private void sendShipmentNotification(EShipment shipment, String notificationType, String message) {
        try {
            log.debug("Sending {} notification for shipment: {}", notificationType, shipment.getShipmentNumber());
            
            NotificationServiceClient.ShippingNotificationRequest notificationRequest = 
                new NotificationServiceClient.ShippingNotificationRequest(
                    null, // recipientId - will be resolved by notification service
                    null, // recipientEmail - will be resolved by notification service  
                    null, // recipientPhone - will be resolved by notification service
                    notificationType,
                    shipment.getOrderId().toString(),
                    shipment.getTrackingNumber(),
                    shipment.getCarrier().getCarrierName(),
                    shipment.getEstimatedDeliveryDate() != null ? shipment.getEstimatedDeliveryDate().toString() : null,
                    java.util.Map.of("message", message, "shipmentNumber", shipment.getShipmentNumber())
                );
            
            notificationServiceClient.sendShippingNotification(notificationRequest);
            
            log.debug("Notification sent successfully for shipment: {}", shipment.getShipmentNumber());
            
        } catch (Exception e) {
            log.warn("Failed to send shipment notification: {}", e.getMessage());
            // Don't fail the entire process for notification failures
        }
    }
    
    private void updateOrderShippingInfo(EShipment shipment) {
        try {
            log.debug("Updating order shipping info for order: {}", shipment.getOrderId());
            
            OrderServiceClient.OrderShippingInfoRequest request = 
                new OrderServiceClient.OrderShippingInfoRequest(
                    shipment.getCarrier().getCarrierId().toString(),
                    shipment.getCarrier().getCarrierName(),
                    shipment.getTrackingNumber(),
                    shipment.getShippingLabelUrl(),
                    shipment.getEstimatedDeliveryDate() != null ? 
                        shipment.getEstimatedDeliveryDate().toString() : null
                );
            
            orderServiceClient.updateOrderShippingInfo(shipment.getOrderId(), request);
            
            log.debug("Order shipping info updated successfully for order: {}", shipment.getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to update order shipping info for order: {}", shipment.getOrderId(), e);
            // This is critical - we should ensure order is updated
            throw new BusinessLogicException("Failed to update order with shipping information: " + e.getMessage());
        }
    }
    
    private void releaseInventoryReservations(EShipment shipment) {
        try {
            log.debug("Releasing inventory reservations for shipment: {}", shipment.getShipmentNumber());
            
            // Get shipment items to release inventory
            List<EShipmentItem> shipmentItems = shipmentItemRepository.findByShipmentId(shipment.getShipmentId());
            
            for (EShipmentItem item : shipmentItems) {
                try {
                    // We would need a reservation ID to release - in a real implementation, this would be stored
                    // For now, we'll create a fictional reservation ID based on shipment and product
                    String reservationId = "RES-" + shipment.getShipmentNumber() + "-" + item.getProductId();
                    
                    inventoryServiceClient.releaseInventoryReservation(reservationId);
                    log.debug("Released inventory reservation for product: {}", item.getProductId());
                    
                } catch (Exception e) {
                    log.warn("Failed to release inventory reservation for product {}: {}", item.getProductId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to release inventory reservations for shipment: {}", shipment.getShipmentNumber(), e);
            // Don't throw exception as this shouldn't block cancellation
        }
    }
    
    private void processShippingRefund(EShipment shipment, String reason) {
        try {
            log.debug("Processing shipping refund for shipment: {} with reason: {}", shipment.getShipmentNumber(), reason);
            
            // Get payments for the order
            ResponseEntity<List<PaymentDto>> paymentsResponse = paymentServiceClient.getPaymentsByOrderId(shipment.getOrderId());
            
            if (paymentsResponse.getBody() == null || paymentsResponse.getBody().isEmpty()) {
                log.debug("No payments found for order: {}, skipping refund", shipment.getOrderId());
                return;
            }
            
            // Find shipping-related payments
            for (PaymentDto payment : paymentsResponse.getBody()) {
                if ("SHIPPING_FEE".equals(payment.getPaymentType()) && "COMPLETED".equals(payment.getPaymentStatus())) {
                    
                    // Process refund for shipping cost
                    PaymentDto.RefundRequest refundRequest = PaymentDto.RefundRequest.builder()
                        .paymentId(payment.getPaymentId())
                        .refundAmount(shipment.getShippingCost())
                        .reason(reason)
                        .description("Refund for cancelled shipment: " + shipment.getShipmentNumber())
                        .notifyCustomer(true)
                        .build();
                    
                    ResponseEntity<PaymentDto.RefundResponse> refundResponse = 
                        paymentServiceClient.processRefund(refundRequest);
                    
                    if (refundResponse.getBody() != null && "COMPLETED".equals(refundResponse.getBody().getRefundStatus())) {
                        log.info("Shipping refund processed successfully for shipment: {} amount: {}", 
                            shipment.getShipmentNumber(), shipment.getShippingCost());
                    } else {
                        log.warn("Shipping refund failed for shipment: {}", shipment.getShipmentNumber());
                    }
                    
                    break; // Only process first shipping fee payment
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to process shipping refund for shipment: {}", shipment.getShipmentNumber(), e);
            // Don't throw exception as this shouldn't block cancellation
        }
    }
} 
