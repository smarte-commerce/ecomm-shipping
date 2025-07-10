package com.winnguyen1905.shipping.core.service.impl;

import com.winnguyen1905.shipping.core.model.request.CreateShippingMethodRequest;
import com.winnguyen1905.shipping.core.model.response.ShippingMethodResponse;
import com.winnguyen1905.shipping.core.service.ShippingMethodService;
import com.winnguyen1905.shipping.common.enums.ServiceType;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;
import com.winnguyen1905.shipping.exception.BadRequestException;
import com.winnguyen1905.shipping.exception.BusinessLogicException;
import com.winnguyen1905.shipping.exception.ResourceNotFoundException;
import com.winnguyen1905.shipping.persistance.entity.EShippingMethod;
import com.winnguyen1905.shipping.persistance.entity.EShippingCarrier;
import com.winnguyen1905.shipping.persistance.entity.EShippingZone;
import com.winnguyen1905.shipping.persistance.entity.EShipment;
import com.winnguyen1905.shipping.persistance.repository.ShippingMethodRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingCarrierRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingZoneRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShippingMethodServiceImpl implements ShippingMethodService {

    private final ShippingMethodRepository shippingMethodRepository;
    private final ShippingCarrierRepository carrierRepository;
    private final ShippingZoneRepository zoneRepository;
    private final ShipmentRepository shipmentRepository;

    @Override
    public ShippingMethodResponse createShippingMethod(CreateShippingMethodRequest request, TAccountRequest accountRequest) {
        log.info("Creating shipping method: {} for account: {}", request.getMethodName(), accountRequest.username());
        
        // Validate request
        validateCreateShippingMethodRequest(request);
        
        // Check if method code already exists
        if (shippingMethodRepository.existsByMethodCode(request.getMethodCode())) {
            throw new BadRequestException("Shipping method code already exists: " + request.getMethodCode());
        }
        
        // Validate carrier exists and is active
        EShippingCarrier carrier = carrierRepository.findById(request.getCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + request.getCarrierId()));
        
        if (!carrier.getIsActive()) {
            throw new BusinessLogicException("Cannot create shipping method for inactive carrier");
        }
        
        // Validate zone exists and is active
        EShippingZone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + request.getZoneId()));
        
        if (!zone.getIsActive()) {
            throw new BusinessLogicException("Cannot create shipping method for inactive zone");
        }
        
        // Create shipping method entity
        EShippingMethod method = EShippingMethod.builder()
                .carrier(carrier)
                .zone(zone)
                .methodName(request.getMethodName())
                .methodCode(request.getMethodCode().toUpperCase())
                .serviceType(request.getServiceType())
                .baseRate(request.getBaseRate())
                .perKgRate(request.getPerKgRate())
                .perItemRate(request.getPerItemRate())
                .minWeight(request.getMinWeight())
                .maxWeight(request.getMaxWeight())
                .minOrderValue(request.getMinOrderValue())
                .maxOrderValue(request.getMaxOrderValue())
                .estimatedDaysMin(request.getEstimatedDaysMin())
                .estimatedDaysMax(request.getEstimatedDaysMax())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        method = shippingMethodRepository.save(method);
        
        log.info("Shipping method created successfully with ID: {}", method.getMethodId());
        return mapToShippingMethodResponse(method);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingMethodResponse getShippingMethodById(Integer id, TAccountRequest accountRequest) {
        log.info("Getting shipping method by ID: {} for account: {}", id, accountRequest.username());
        
        EShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));
        
        return mapToShippingMethodResponse(method);
    }

    @Override
    public ShippingMethodResponse updateShippingMethod(Integer id, CreateShippingMethodRequest request, TAccountRequest accountRequest) {
        log.info("Updating shipping method ID: {} for account: {}", id, accountRequest.username());
        
        // Validate request
        validateCreateShippingMethodRequest(request);
        
        EShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));
        
        // Check if method code already exists (excluding current method)
        if (!method.getMethodCode().equals(request.getMethodCode()) && 
            shippingMethodRepository.existsByMethodCode(request.getMethodCode())) {
            throw new BadRequestException("Shipping method code already exists: " + request.getMethodCode());
        }
        
        // Validate carrier exists and is active
        EShippingCarrier carrier = carrierRepository.findById(request.getCarrierId())
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + request.getCarrierId()));
        
        // Validate zone exists and is active
        EShippingZone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + request.getZoneId()));
        
        // Update method
        method.setCarrier(carrier);
        method.setZone(zone);
        method.setMethodName(request.getMethodName());
        method.setMethodCode(request.getMethodCode().toUpperCase());
        method.setServiceType(request.getServiceType());
        method.setBaseRate(request.getBaseRate());
        method.setPerKgRate(request.getPerKgRate());
        method.setPerItemRate(request.getPerItemRate());
        method.setMinWeight(request.getMinWeight());
        method.setMaxWeight(request.getMaxWeight());
        method.setMinOrderValue(request.getMinOrderValue());
        method.setMaxOrderValue(request.getMaxOrderValue());
        method.setEstimatedDaysMin(request.getEstimatedDaysMin());
        method.setEstimatedDaysMax(request.getEstimatedDaysMax());
        method.setIsActive(request.getIsActive() != null ? request.getIsActive() : method.getIsActive());
        
        method = shippingMethodRepository.save(method);
        
        log.info("Shipping method updated successfully with ID: {}", method.getMethodId());
        return mapToShippingMethodResponse(method);
    }

    @Override
    public void deleteShippingMethod(Integer id, TAccountRequest accountRequest) {
        log.info("Deleting shipping method ID: {} for account: {}", id, accountRequest.username());
        
        EShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));
        
        // Check if method has active shipments
        long activeShipmentsCount = shipmentRepository.countByMethodMethodIdAndStatusNot(id, ShipmentStatus.DELIVERED);
        if (activeShipmentsCount > 0) {
            throw new BusinessLogicException("Cannot delete shipping method with active shipments");
        }
        
        // Soft delete by deactivating
        method.setIsActive(false);
        shippingMethodRepository.save(method);
        
        log.info("Shipping method deactivated successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShippingMethodResponse> getAllShippingMethods(Integer carrierId, Integer zoneId, ServiceType serviceType, 
                                                            Boolean isActive, Pageable pageable, TAccountRequest accountRequest) {
        log.info("Getting all shipping methods with filters for account: {}", accountRequest.username());
        
        // Build query based on filters
        List<EShippingMethod> methods = shippingMethodRepository.findAll();
        
        // Apply filters
        if (carrierId != null) {
            methods = methods.stream()
                    .filter(m -> m.getCarrier().getCarrierId().equals(carrierId))
                    .collect(Collectors.toList());
        }
        
        if (zoneId != null) {
            methods = methods.stream()
                    .filter(m -> m.getZone().getZoneId().equals(zoneId))
                    .collect(Collectors.toList());
        }
        
        if (serviceType != null) {
            methods = methods.stream()
                    .filter(m -> m.getServiceType().equals(serviceType))
                    .collect(Collectors.toList());
        }
        
        if (isActive != null) {
            methods = methods.stream()
                    .filter(m -> m.getIsActive().equals(isActive))
                    .collect(Collectors.toList());
        }
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), methods.size());
        
        List<ShippingMethodResponse> responses = methods.subList(start, end).stream()
                .map(this::mapToShippingMethodResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, methods.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingMethodResponse> getShippingMethodsByCarrier(Integer carrierId, TAccountRequest accountRequest) {
        log.info("Getting shipping methods for carrier ID: {} for account: {}", carrierId, accountRequest.username());
        
        List<EShippingMethod> methods = shippingMethodRepository.findByCarrierCarrierIdAndIsActive(carrierId, true);
        
        return methods.stream()
                .map(this::mapToShippingMethodResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingMethodResponse> getShippingMethodsByZone(Integer zoneId, TAccountRequest accountRequest) {
        log.info("Getting shipping methods for zone ID: {} for account: {}", zoneId, accountRequest.username());
        
        List<EShippingMethod> methods = shippingMethodRepository.findByZoneZoneIdAndIsActive(zoneId, true);
        
        return methods.stream()
                .map(this::mapToShippingMethodResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingMethodResponse.ShippingMethodAvailability> getAvailableShippingMethods(
            Integer carrierId, Integer zoneId, BigDecimal weight, BigDecimal orderValue, TAccountRequest accountRequest) {
        log.info("Getting available shipping methods for carrier: {}, zone: {}, weight: {}, value: {} for account: {}", 
                carrierId, zoneId, weight, orderValue, accountRequest.username());
        
        List<EShippingMethod> methods;
        
        if (carrierId != null && zoneId != null) {
            methods = shippingMethodRepository.findAvailableMethodsForShipment(carrierId, zoneId, weight, orderValue);
        } else if (carrierId != null) {
            methods = shippingMethodRepository.findByCarrierCarrierIdAndIsActive(carrierId, true);
        } else if (zoneId != null) {
            methods = shippingMethodRepository.findByZoneZoneIdAndIsActive(zoneId, true);
        } else {
            methods = shippingMethodRepository.findByIsActive(true);
        }
        
        return methods.stream()
                .map(method -> mapToShippingMethodAvailability(method, weight, orderValue))
                .collect(Collectors.toList());
    }

    @Override
    public ShippingMethodResponse activateShippingMethod(Integer id, TAccountRequest accountRequest) {
        log.info("Activating shipping method ID: {} for account: {}", id, accountRequest.username());
        
        EShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));
        
        // Validate that carrier and zone are active
        if (!method.getCarrier().getIsActive()) {
            throw new BusinessLogicException("Cannot activate shipping method for inactive carrier");
        }
        
        if (!method.getZone().getIsActive()) {
            throw new BusinessLogicException("Cannot activate shipping method for inactive zone");
        }
        
        method.setIsActive(true);
        method = shippingMethodRepository.save(method);
        
        log.info("Shipping method activated successfully with ID: {}", method.getMethodId());
        return mapToShippingMethodResponse(method);
    }

    @Override
    public ShippingMethodResponse deactivateShippingMethod(Integer id, TAccountRequest accountRequest) {
        log.info("Deactivating shipping method ID: {} for account: {}", id, accountRequest.username());
        
        EShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));
        
        method.setIsActive(false);
        method = shippingMethodRepository.save(method);
        
        log.info("Shipping method deactivated successfully with ID: {}", method.getMethodId());
        return mapToShippingMethodResponse(method);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingMethodResponse.ShippingMethodStatistics getShippingMethodStatistics(Integer id, TAccountRequest accountRequest) {
        log.info("Getting statistics for shipping method ID: {} for account: {}", id, accountRequest.username());
        
        EShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));
        
        List<EShipment> shipments = shipmentRepository.findByMethodMethodId(id);
        
        long totalShipments = shipments.size();
        BigDecimal totalRevenue = shipments.stream()
                .map(EShipment::getShippingCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate average delivery days
        double averageDeliveryDays = shipments.stream()
                .filter(s -> s.getDeliveredDate() != null)
                .mapToDouble(s -> ChronoUnit.DAYS.between(s.getCreatedAt(), s.getDeliveredDate()))
                .average()
                .orElse(0.0);
        
        // Calculate success rate
        long deliveredShipments = shipments.stream()
                .filter(s -> "DELIVERED".equals(s.getStatus()))
                .count();
        double successRate = totalShipments > 0 ? (double) deliveredShipments / totalShipments : 0.0;
        
        Instant mostRecentShipment = shipments.stream()
                .map(EShipment::getCreatedAt)
                .max(Instant::compareTo)
                .orElse(null);
        
        return ShippingMethodResponse.ShippingMethodStatistics.builder()
                .methodId(id)
                .methodName(method.getMethodName())
                .totalShipments(totalShipments)
                .totalRevenue(totalRevenue)
                .averageDeliveryDays(averageDeliveryDays)
                .successRate(successRate)
                .mostRecentShipment(mostRecentShipment)
                .build();
    }

    @Override
    public ShippingMethodResponse.BulkUpdateResult bulkUpdateRates(List<Integer> methodIds, BigDecimal rateMultiplier, TAccountRequest accountRequest) {
        log.info("Bulk updating rates for {} methods with multiplier: {} for account: {}", 
                methodIds.size(), rateMultiplier, accountRequest.username());
        
        if (rateMultiplier == null || rateMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Rate multiplier must be greater than zero");
        }
        
        List<ShippingMethodResponse.BulkUpdateResult.UpdateResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;
        
        for (Integer methodId : methodIds) {
            try {
                EShippingMethod method = shippingMethodRepository.findById(methodId)
                        .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + methodId));
                
                // Update rates
                method.setBaseRate(method.getBaseRate().multiply(rateMultiplier));
                method.setPerKgRate(method.getPerKgRate().multiply(rateMultiplier));
                method.setPerItemRate(method.getPerItemRate().multiply(rateMultiplier));
                
                shippingMethodRepository.save(method);
                
                results.add(ShippingMethodResponse.BulkUpdateResult.UpdateResult.builder()
                        .methodId(methodId)
                        .isSuccessful(true)
                        .build());
                successful++;
                
            } catch (Exception e) {
                log.error("Failed to update shipping method ID: {}, error: {}", methodId, e.getMessage());
                results.add(ShippingMethodResponse.BulkUpdateResult.UpdateResult.builder()
                        .methodId(methodId)
                        .isSuccessful(false)
                        .errorMessage(e.getMessage())
                        .build());
                failed++;
            }
        }
        
        return ShippingMethodResponse.BulkUpdateResult.builder()
                .totalRequested(methodIds.size())
                .successfulUpdates(successful)
                .failedUpdates(failed)
                .updateResults(results)
                .build();
    }

    @Override
    public ShippingMethodResponse cloneShippingMethod(Integer methodId, Integer targetZoneId, TAccountRequest accountRequest) {
        log.info("Cloning shipping method ID: {} to zone ID: {} for account: {}", 
                methodId, targetZoneId, accountRequest.username());
        
        EShippingMethod sourceMethod = shippingMethodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + methodId));
        
        EShippingZone targetZone = zoneRepository.findById(targetZoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + targetZoneId));
        
        if (!targetZone.getIsActive()) {
            throw new BusinessLogicException("Cannot clone shipping method to inactive zone");
        }
        
        // Generate new method code
        String newMethodCode = sourceMethod.getMethodCode() + "_" + targetZone.getZoneCode();
        
        // Check if the new method code already exists
        if (shippingMethodRepository.existsByMethodCode(newMethodCode)) {
            throw new BadRequestException("Cloned method code already exists: " + newMethodCode);
        }
        
        // Clone the method
        EShippingMethod clonedMethod = EShippingMethod.builder()
                .carrier(sourceMethod.getCarrier())
                .zone(targetZone)
                .methodName(sourceMethod.getMethodName() + " - " + targetZone.getZoneName())
                .methodCode(newMethodCode)
                .serviceType(sourceMethod.getServiceType())
                .baseRate(sourceMethod.getBaseRate())
                .perKgRate(sourceMethod.getPerKgRate())
                .perItemRate(sourceMethod.getPerItemRate())
                .minWeight(sourceMethod.getMinWeight())
                .maxWeight(sourceMethod.getMaxWeight())
                .minOrderValue(sourceMethod.getMinOrderValue())
                .maxOrderValue(sourceMethod.getMaxOrderValue())
                .estimatedDaysMin(sourceMethod.getEstimatedDaysMin())
                .estimatedDaysMax(sourceMethod.getEstimatedDaysMax())
                .isActive(sourceMethod.getIsActive())
                .build();
        
        clonedMethod = shippingMethodRepository.save(clonedMethod);
        
        log.info("Shipping method cloned successfully with ID: {}", clonedMethod.getMethodId());
        return mapToShippingMethodResponse(clonedMethod);
    }

    private void validateCreateShippingMethodRequest(CreateShippingMethodRequest request) {
        ShippingValidationUtils.validateNotNull(request, "Shipping method request cannot be null");
        ShippingValidationUtils.validateNotBlank(request.getMethodName(), "Method name cannot be blank");
        ShippingValidationUtils.validateNotBlank(request.getMethodCode(), "Method code cannot be blank");
        ShippingValidationUtils.validateNotNull(request.getServiceType(), "Service type cannot be null");
        ShippingValidationUtils.validateNotNull(request.getCarrierId(), "Carrier ID cannot be null");
        ShippingValidationUtils.validateNotNull(request.getZoneId(), "Zone ID cannot be null");
        ShippingValidationUtils.validateNotNull(request.getBaseRate(), "Base rate cannot be null");
        ShippingValidationUtils.validateNotNull(request.getPerKgRate(), "Per kg rate cannot be null");
        ShippingValidationUtils.validateNotNull(request.getPerItemRate(), "Per item rate cannot be null");
        
        // Validate rate values
        if (request.getBaseRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Base rate cannot be negative");
        }
        if (request.getPerKgRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Per kg rate cannot be negative");
        }
        if (request.getPerItemRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Per item rate cannot be negative");
        }
        
        // Validate weight ranges
        if (request.getMinWeight() != null && request.getMaxWeight() != null) {
            if (request.getMinWeight().compareTo(request.getMaxWeight()) > 0) {
                throw new BadRequestException("Minimum weight cannot be greater than maximum weight");
            }
        }
        
        // Validate order value ranges
        if (request.getMinOrderValue() != null && request.getMaxOrderValue() != null) {
            if (request.getMinOrderValue().compareTo(request.getMaxOrderValue()) > 0) {
                throw new BadRequestException("Minimum order value cannot be greater than maximum order value");
            }
        }
        
        // Validate delivery days
        if (request.getEstimatedDaysMin() != null && request.getEstimatedDaysMax() != null) {
            if (request.getEstimatedDaysMin() > request.getEstimatedDaysMax()) {
                throw new BadRequestException("Minimum delivery days cannot be greater than maximum delivery days");
            }
        }
    }

    private ShippingMethodResponse mapToShippingMethodResponse(EShippingMethod method) {
        // Get shipment count for the method
        long shipmentCount = shipmentRepository.countByMethodMethodId(method.getMethodId());
        
        return ShippingMethodResponse.builder()
                .methodId(method.getMethodId())
                .carrierId(method.getCarrier().getCarrierId())
                .carrierName(method.getCarrier().getCarrierName())
                .carrierCode(method.getCarrier().getCarrierCode())
                .zoneId(method.getZone().getZoneId())
                .zoneName(method.getZone().getZoneName())
                .zoneCode(method.getZone().getZoneCode())
                .methodName(method.getMethodName())
                .methodCode(method.getMethodCode())
                .serviceType(method.getServiceType())
                .baseRate(method.getBaseRate())
                .perKgRate(method.getPerKgRate())
                .perItemRate(method.getPerItemRate())
                .minWeight(method.getMinWeight())
                .maxWeight(method.getMaxWeight())
                .minOrderValue(method.getMinOrderValue())
                .maxOrderValue(method.getMaxOrderValue())
                .estimatedDaysMin(method.getEstimatedDaysMin())
                .estimatedDaysMax(method.getEstimatedDaysMax())
                .isActive(method.getIsActive())
                .createdAt(method.getCreatedAt())
                .updatedAt(method.getUpdatedAt())
                .shipmentCount(shipmentCount)
                .build();
    }

    private ShippingMethodResponse.ShippingMethodAvailability mapToShippingMethodAvailability(
            EShippingMethod method, BigDecimal weight, BigDecimal orderValue) {
        
        boolean isAvailable = true;
        String unavailableReason = null;
        
        // Check weight constraints
        if (weight != null) {
            if (method.getMinWeight() != null && weight.compareTo(method.getMinWeight()) < 0) {
                isAvailable = false;
                unavailableReason = "Weight below minimum limit";
            } else if (method.getMaxWeight() != null && weight.compareTo(method.getMaxWeight()) > 0) {
                isAvailable = false;
                unavailableReason = "Weight above maximum limit";
            }
        }
        
        // Check order value constraints
        if (orderValue != null && isAvailable) {
            if (method.getMinOrderValue() != null && orderValue.compareTo(method.getMinOrderValue()) < 0) {
                isAvailable = false;
                unavailableReason = "Order value below minimum limit";
            } else if (method.getMaxOrderValue() != null && orderValue.compareTo(method.getMaxOrderValue()) > 0) {
                isAvailable = false;
                unavailableReason = "Order value above maximum limit";
            }
        }
        
        // Calculate estimated rate
        BigDecimal estimatedRate = method.getBaseRate();
        if (weight != null) {
            estimatedRate = estimatedRate.add(method.getPerKgRate().multiply(weight));
        }
        // Assuming one item for per-item rate
        estimatedRate = estimatedRate.add(method.getPerItemRate());
        
        return ShippingMethodResponse.ShippingMethodAvailability.builder()
                .methodId(method.getMethodId())
                .methodName(method.getMethodName())
                .methodCode(method.getMethodCode())
                .serviceType(method.getServiceType())
                .isAvailable(isAvailable)
                .unavailableReason(unavailableReason)
                .estimatedRate(estimatedRate)
                .estimatedDaysMin(method.getEstimatedDaysMin())
                .estimatedDaysMax(method.getEstimatedDaysMax())
                .build();
    }
} 
