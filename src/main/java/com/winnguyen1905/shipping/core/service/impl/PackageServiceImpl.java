package com.winnguyen1905.shipping.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.shipping.core.model.request.CreatePackageRequest;
import com.winnguyen1905.shipping.core.model.response.PackageResponse;
import com.winnguyen1905.shipping.core.service.PackageService;
import com.winnguyen1905.shipping.exception.BadRequestException;
import com.winnguyen1905.shipping.exception.BusinessLogicException;
import com.winnguyen1905.shipping.exception.ResourceNotFoundException;
import com.winnguyen1905.shipping.persistance.entity.EShipment;
import com.winnguyen1905.shipping.persistance.entity.EShipmentPackage;
import com.winnguyen1905.shipping.persistance.entity.EShipmentTrackingEvent;
import com.winnguyen1905.shipping.persistance.repository.ShipmentPackageRepository;
import com.winnguyen1905.shipping.persistance.repository.ShipmentRepository;
import com.winnguyen1905.shipping.persistance.repository.ShipmentTrackingEventRepository;
import com.winnguyen1905.shipping.common.utils.ShippingValidationUtils;
import com.winnguyen1905.shipping.common.constants.ShippingConstants;
import com.winnguyen1905.shipping.secure.TAccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PackageServiceImpl implements PackageService {

    private final ShipmentPackageRepository packageRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public PackageResponse createPackage(CreatePackageRequest request, TAccountRequest accountRequest) {
        log.info("Creating package for shipment: {} for account: {}", request.getShipmentId(), accountRequest.username());
        
        // Validate request
        validateCreatePackageRequest(request);
        
        // Check if shipment exists
        EShipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + request.getShipmentId()));
        
        // Check if package number already exists
        if (packageRepository.existsByPackageNumber(request.getPackageNumber())) {
            throw new BadRequestException("Package number already exists: " + request.getPackageNumber());
        }
        
        // Check if tracking number already exists (if provided)
        if (request.getTrackingNumber() != null && 
            packageRepository.existsByTrackingNumber(request.getTrackingNumber())) {
            throw new BadRequestException("Tracking number already exists: " + request.getTrackingNumber());
        }
        
        // Create package entity
        EShipmentPackage packageEntity = EShipmentPackage.builder()
                .shipment(shipment)
                .packageNumber(request.getPackageNumber())
                .trackingNumber(request.getTrackingNumber())
                .weight(request.getWeight())
                .dimensions(convertDimensionsToJson(request.getDimensions()))
                .packageType(request.getPackageType() != null ? request.getPackageType() : ShippingConstants.DEFAULT_PACKAGE_TYPE)
                .isFragile(request.getIsFragile() != null ? request.getIsFragile() : ShippingConstants.DEFAULT_IS_FRAGILE)
                .isLiquid(request.getIsLiquid() != null ? request.getIsLiquid() : ShippingConstants.DEFAULT_IS_LIQUID)
                .isHazardous(request.getIsHazardous() != null ? request.getIsHazardous() : ShippingConstants.DEFAULT_IS_HAZARDOUS)
                .build();
        
        packageEntity = packageRepository.save(packageEntity);
        
        log.info("Package created successfully with ID: {}", packageEntity.getPackageId());
        return mapToPackageResponse(packageEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PackageResponse getPackageById(Long id, TAccountRequest accountRequest) {
        log.info("Getting package by ID: {} for account: {}", id, accountRequest.username());
        
        EShipmentPackage packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + id));
        
        return mapToPackageResponse(packageEntity);
    }

    @Override
    public PackageResponse updatePackage(Long id, CreatePackageRequest request, TAccountRequest accountRequest) {
        log.info("Updating package ID: {} for account: {}", id, accountRequest.username());
        
        // Validate request
        validateUpdatePackageRequest(request);
        
        EShipmentPackage packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + id));
        
        // Check if package number already exists (excluding current package)
        if (!packageEntity.getPackageNumber().equals(request.getPackageNumber()) &&
            packageRepository.existsByPackageNumber(request.getPackageNumber())) {
            throw new BadRequestException("Package number already exists: " + request.getPackageNumber());
        }
        
        // Check if tracking number already exists (excluding current package)
        if (request.getTrackingNumber() != null && 
            (packageEntity.getTrackingNumber() == null || !packageEntity.getTrackingNumber().equals(request.getTrackingNumber())) &&
            packageRepository.existsByTrackingNumber(request.getTrackingNumber())) {
            throw new BadRequestException("Tracking number already exists: " + request.getTrackingNumber());
        }
        
        // Update package
        packageEntity.setPackageNumber(request.getPackageNumber());
        packageEntity.setTrackingNumber(request.getTrackingNumber());
        packageEntity.setWeight(request.getWeight());
        packageEntity.setDimensions(convertDimensionsToJson(request.getDimensions()));
        packageEntity.setPackageType(request.getPackageType() != null ? request.getPackageType() : packageEntity.getPackageType());
        packageEntity.setIsFragile(request.getIsFragile() != null ? request.getIsFragile() : packageEntity.getIsFragile());
        packageEntity.setIsLiquid(request.getIsLiquid() != null ? request.getIsLiquid() : packageEntity.getIsLiquid());
        packageEntity.setIsHazardous(request.getIsHazardous() != null ? request.getIsHazardous() : packageEntity.getIsHazardous());
        
        packageEntity = packageRepository.save(packageEntity);
        
        log.info("Package updated successfully with ID: {}", packageEntity.getPackageId());
        return mapToPackageResponse(packageEntity);
    }

    @Override
    public void deletePackage(Long id, TAccountRequest accountRequest) {
        log.info("Deleting package ID: {} for account: {}", id, accountRequest.username());
        
        EShipmentPackage packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + id));
        
        // Check if package is part of shipped shipment
        if (packageEntity.getShipment().getShippedAt() != null) {
            throw new BusinessLogicException("Cannot delete package from shipped shipment");
        }
        
        packageRepository.delete(packageEntity);
        
        log.info("Package deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PackageResponse> getAllPackages(Long shipmentId, String packageType, Boolean isFragile, 
                                               Boolean isHazardous, String trackingNumber, 
                                               Pageable pageable, TAccountRequest accountRequest) {
        log.info("Getting all packages with filters for account: {}", accountRequest.username());
        
        // Build query based on filters
        List<EShipmentPackage> packages = packageRepository.findAll();
        
        // Apply filters
        if (shipmentId != null) {
            packages = packages.stream()
                    .filter(p -> p.getShipment().getShipmentId().equals(shipmentId))
                    .collect(Collectors.toList());
        }
        
        if (packageType != null && !packageType.trim().isEmpty()) {
            packages = packages.stream()
                    .filter(p -> p.getPackageType().equals(packageType))
                    .collect(Collectors.toList());
        }
        
        if (isFragile != null) {
            packages = packages.stream()
                    .filter(p -> p.getIsFragile().equals(isFragile))
                    .collect(Collectors.toList());
        }
        
        if (isHazardous != null) {
            packages = packages.stream()
                    .filter(p -> p.getIsHazardous().equals(isHazardous))
                    .collect(Collectors.toList());
        }
        
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            packages = packages.stream()
                    .filter(p -> p.getTrackingNumber() != null && p.getTrackingNumber().contains(trackingNumber))
                    .collect(Collectors.toList());
        }
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), packages.size());
        
        List<PackageResponse> packageResponses = packages.subList(start, end).stream()
                .map(this::mapToPackageResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(packageResponses, pageable, packages.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageResponse> getPackagesByShipment(Long shipmentId, TAccountRequest accountRequest) {
        log.info("Getting packages by shipment ID: {} for account: {}", shipmentId, accountRequest.username());
        
        List<EShipmentPackage> packages = packageRepository.findByShipmentId(shipmentId);
        
        return packages.stream()
                .map(this::mapToPackageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PackageResponse.PackageTrackingInfo getPackageTrackingInfo(Long id, TAccountRequest accountRequest) {
        log.info("Getting package tracking info for ID: {} for account: {}", id, accountRequest.username());
        
        EShipmentPackage packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + id));
        
        if (packageEntity.getTrackingNumber() == null) {
            throw new BadRequestException("Package does not have tracking number");
        }
        
        return mapToPackageTrackingInfo(packageEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PackageResponse.PackageTrackingInfo trackPackageByNumber(String trackingNumber) {
        log.info("Tracking package by number: {}", trackingNumber);
        
        EShipmentPackage packageEntity = packageRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with tracking number: " + trackingNumber));
        
        return mapToPackageTrackingInfo(packageEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PackageResponse.PackageValidationResult validatePackage(Long id, TAccountRequest accountRequest) {
        log.info("Validating package ID: {} for account: {}", id, accountRequest.username());
        
        EShipmentPackage packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + id));
        
        return validatePackageCompliance(packageEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageResponse.PackageValidationResult> bulkValidatePackages(List<Long> packageIds, TAccountRequest accountRequest) {
        log.info("Bulk validating {} packages for account: {}", packageIds.size(), accountRequest.username());
        
        List<PackageResponse.PackageValidationResult> results = new ArrayList<>();
        
        for (Long packageId : packageIds) {
            try {
                EShipmentPackage packageEntity = packageRepository.findById(packageId)
                        .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + packageId));
                
                results.add(validatePackageCompliance(packageEntity));
            } catch (Exception e) {
                results.add(PackageResponse.PackageValidationResult.builder()
                        .packageId(packageId)
                        .isValid(false)
                        .validationErrors(List.of("Package not found or validation failed: " + e.getMessage()))
                        .build());
            }
        }
        
        return results;
    }

    @Override
    public PackageResponse assignTrackingNumber(Long id, String trackingNumber, TAccountRequest accountRequest) {
        log.info("Assigning tracking number to package ID: {} for account: {}", id, accountRequest.username());
        
        EShipmentPackage packageEntity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + id));
        
        // Validate tracking number
        if (!ShippingValidationUtils.isValidTrackingNumber(trackingNumber)) {
            throw new BadRequestException("Invalid tracking number format");
        }
        
        // Check if tracking number already exists
        if (packageRepository.existsByTrackingNumber(trackingNumber)) {
            throw new BadRequestException("Tracking number already exists: " + trackingNumber);
        }
        
        packageEntity.setTrackingNumber(trackingNumber);
        packageEntity = packageRepository.save(packageEntity);
        
        log.info("Tracking number assigned successfully to package ID: {}", id);
        return mapToPackageResponse(packageEntity);
    }

    @Override
    public PackageResponse.BulkPackageOperation bulkAssignTrackingNumbers(Map<Long, String> packageTrackingMap, TAccountRequest accountRequest) {
        log.info("Bulk assigning tracking numbers to {} packages for account: {}", packageTrackingMap.size(), accountRequest.username());
        
        List<PackageResponse.BulkPackageOperation.OperationResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        
        for (Map.Entry<Long, String> entry : packageTrackingMap.entrySet()) {
            Long packageId = entry.getKey();
            String trackingNumber = entry.getValue();
            
            try {
                assignTrackingNumber(packageId, trackingNumber, accountRequest);
                results.add(PackageResponse.BulkPackageOperation.OperationResult.builder()
                        .packageId(packageId)
                        .isSuccessful(true)
                        .build());
                successCount++;
            } catch (Exception e) {
                results.add(PackageResponse.BulkPackageOperation.OperationResult.builder()
                        .packageId(packageId)
                        .isSuccessful(false)
                        .errorMessage(e.getMessage())
                        .build());
                failedCount++;
            }
        }
        
        return PackageResponse.BulkPackageOperation.builder()
                .totalPackages(packageTrackingMap.size())
                .successfulOperations(successCount)
                .failedOperations(failedCount)
                .operationResults(results)
                .processedAt(Instant.now())
                .build();
    }

    @Override
    public List<PackageResponse> splitPackage(Long id, List<CreatePackageRequest> newPackageRequests, TAccountRequest accountRequest) {
        log.info("Splitting package ID: {} into {} packages for account: {}", id, newPackageRequests.size(), accountRequest.username());
        
        EShipmentPackage originalPackage = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with ID: " + id));
        
        // Validate that the package can be split
        if (originalPackage.getShipment().getShippedAt() != null) {
            throw new BusinessLogicException("Cannot split package from shipped shipment");
        }
        
        // Validate total weight doesn't exceed original
        BigDecimal totalNewWeight = newPackageRequests.stream()
                .map(CreatePackageRequest::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalNewWeight.compareTo(originalPackage.getWeight()) > 0) {
            throw new BadRequestException("Total weight of new packages cannot exceed original package weight");
        }
        
        List<PackageResponse> newPackages = new ArrayList<>();
        
        for (CreatePackageRequest request : newPackageRequests) {
            request.setShipmentId(originalPackage.getShipment().getShipmentId());
            PackageResponse newPackage = createPackage(request, accountRequest);
            newPackages.add(newPackage);
        }
        
        // Delete original package
        packageRepository.delete(originalPackage);
        
        log.info("Package split successfully into {} packages", newPackages.size());
        return newPackages;
    }

    @Override
    public PackageResponse mergePackages(List<Long> packageIds, CreatePackageRequest mergedPackageRequest, TAccountRequest accountRequest) {
        log.info("Merging {} packages for account: {}", packageIds.size(), accountRequest.username());
        
        List<EShipmentPackage> packages = packageRepository.findAllById(packageIds);
        
        if (packages.size() != packageIds.size()) {
            throw new BadRequestException("Some packages not found");
        }
        
        // Validate all packages belong to same shipment
        Long shipmentId = packages.get(0).getShipment().getShipmentId();
        boolean allSameShipment = packages.stream()
                .allMatch(p -> p.getShipment().getShipmentId().equals(shipmentId));
        
        if (!allSameShipment) {
            throw new BadRequestException("All packages must belong to the same shipment");
        }
        
        // Validate shipment not shipped yet
        if (packages.get(0).getShipment().getShippedAt() != null) {
            throw new BusinessLogicException("Cannot merge packages from shipped shipment");
        }
        
        // Calculate total weight
        BigDecimal totalWeight = packages.stream()
                .map(EShipmentPackage::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Set merged package details
        mergedPackageRequest.setShipmentId(shipmentId);
        mergedPackageRequest.setWeight(totalWeight);
        
        // Create merged package
        PackageResponse mergedPackage = createPackage(mergedPackageRequest, accountRequest);
        
        // Delete original packages
        packageRepository.deleteAll(packages);
        
        log.info("Packages merged successfully into package ID: {}", mergedPackage.getPackageId());
        return mergedPackage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageResponse> getSpecialHandlingPackages(TAccountRequest accountRequest) {
        log.info("Getting special handling packages for account: {}", accountRequest.username());
        
        List<EShipmentPackage> packages = packageRepository.findSpecialHandlingPackages();
        
        return packages.stream()
                .map(this::mapToPackageResponse)
                .collect(Collectors.toList());
    }

    private void validateCreatePackageRequest(CreatePackageRequest request) {
        if (request.getShipmentId() == null) {
            throw new BadRequestException("Shipment ID is required");
        }
        if (request.getPackageNumber() == null || request.getPackageNumber().trim().isEmpty()) {
            throw new BadRequestException("Package number is required");
        }
        if (request.getWeight() == null || request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Valid weight is required");
        }
        if (!ShippingValidationUtils.isValidWeight(request.getWeight())) {
            throw new BadRequestException("Weight exceeds maximum allowed limit");
        }
        if (request.getDimensions() == null) {
            throw new BadRequestException("Dimensions are required");
        }
        if (request.getPackageType() != null && !ShippingValidationUtils.isValidPackageType(request.getPackageType())) {
            throw new BadRequestException("Invalid package type");
        }
        if (request.getTrackingNumber() != null && !ShippingValidationUtils.isValidTrackingNumber(request.getTrackingNumber())) {
            throw new BadRequestException("Invalid tracking number format");
        }
    }

    private void validateUpdatePackageRequest(CreatePackageRequest request) {
        validateCreatePackageRequest(request);
    }

    private PackageResponse mapToPackageResponse(EShipmentPackage packageEntity) {
        return PackageResponse.builder()
                .packageId(packageEntity.getPackageId())
                .shipmentId(packageEntity.getShipment().getShipmentId())
                .shipmentNumber(packageEntity.getShipment().getShipmentNumber())
                .packageNumber(packageEntity.getPackageNumber())
                .trackingNumber(packageEntity.getTrackingNumber())
                .weight(packageEntity.getWeight())
                .dimensions(convertJsonToDimensions(packageEntity.getDimensions()))
                .packageType(packageEntity.getPackageType())
                .isFragile(packageEntity.getIsFragile())
                .isLiquid(packageEntity.getIsLiquid())
                .isHazardous(packageEntity.getIsHazardous())
                .createdAt(packageEntity.getCreatedAt())
                .carrierName(packageEntity.getShipment().getCarrier().getCarrierName())
                .currentStatus(packageEntity.getShipment().getStatus().name())
                .build();
    }

    private PackageResponse.PackageTrackingInfo mapToPackageTrackingInfo(EShipmentPackage packageEntity) {
        List<EShipmentTrackingEvent> events = trackingEventRepository.findByTrackingNumberOrderByEventTimestampDesc(packageEntity.getTrackingNumber());
        
        List<PackageResponse.PackageTrackingInfo.TrackingEvent> trackingEvents = events.stream()
                .map(e -> PackageResponse.PackageTrackingInfo.TrackingEvent.builder()
                        .eventType(e.getEventType())
                        .eventDescription(e.getEventDescription())
                        .eventLocation(e.getEventLocation())
                        .eventTimestamp(e.getEventTimestamp())
                        .build())
                .collect(Collectors.toList());
        
        return PackageResponse.PackageTrackingInfo.builder()
                .packageId(packageEntity.getPackageId())
                .packageNumber(packageEntity.getPackageNumber())
                .trackingNumber(packageEntity.getTrackingNumber())
                .currentStatus(packageEntity.getShipment().getStatus().name())
                .currentLocation(events.isEmpty() ? null : events.get(0).getEventLocation())
                .lastScanTime(events.isEmpty() ? null : events.get(0).getEventTimestamp())
                .estimatedDelivery(packageEntity.getShipment().getEstimatedDeliveryDate() != null ? 
                        packageEntity.getShipment().getEstimatedDeliveryDate().toString() : null)
                .trackingEvents(trackingEvents)
                .build();
    }

    private PackageResponse.PackageValidationResult validatePackageCompliance(EShipmentPackage packageEntity) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> complianceIssues = new ArrayList<>();
        
        // Weight validation
        if (!ShippingValidationUtils.isValidWeight(packageEntity.getWeight())) {
            errors.add("Package weight exceeds maximum allowed limit");
        }
        
        if (!ShippingValidationUtils.isStandardWeight(packageEntity.getWeight())) {
            warnings.add("Package weight exceeds standard shipping limits");
        }
        
        // Special handling validation
        if (packageEntity.getIsHazardous()) {
            complianceIssues.add("Hazardous materials require special documentation");
        }
        
        if (packageEntity.getIsLiquid()) {
            complianceIssues.add("Liquid packages require special handling");
        }
        
        if (packageEntity.getIsFragile()) {
            warnings.add("Fragile package requires careful handling");
        }
        
        // Package type validation
        if (!ShippingValidationUtils.isValidPackageType(packageEntity.getPackageType())) {
            errors.add("Invalid package type");
        }
        
        return PackageResponse.PackageValidationResult.builder()
                .packageId(packageEntity.getPackageId())
                .isValid(errors.isEmpty())
                .validationErrors(errors)
                .validationWarnings(warnings)
                .complianceIssues(complianceIssues)
                .build();
    }

    private String convertDimensionsToJson(CreatePackageRequest.DimensionsRequest dimensions) {
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

    private PackageResponse.DimensionsResponse convertJsonToDimensions(String json) {
        if (json == null || json.trim().isEmpty()) {
            return PackageResponse.DimensionsResponse.builder().build();
        }
        try {
            CreatePackageRequest.DimensionsRequest dims = objectMapper.readValue(json, CreatePackageRequest.DimensionsRequest.class);
            BigDecimal volume = dims.getLength().multiply(dims.getWidth()).multiply(dims.getHeight());
            BigDecimal dimensionalWeight = ShippingValidationUtils.calculateDimensionalWeight(dims.getLength(), dims.getWidth(), dims.getHeight());
            
            return PackageResponse.DimensionsResponse.builder()
                    .length(dims.getLength())
                    .width(dims.getWidth())
                    .height(dims.getHeight())
                    .unit(dims.getUnit())
                    .volume(volume)
                    .dimensionalWeight(dimensionalWeight)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to dimensions", e);
            return PackageResponse.DimensionsResponse.builder().build();
        }
    }
} 
