package com.winnguyen1905.shipping.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.shipping.core.model.request.CreateCarrierRequest;
import com.winnguyen1905.shipping.core.model.response.CarrierResponse;
import com.winnguyen1905.shipping.core.service.CarrierService;
import com.winnguyen1905.shipping.exception.BadRequestException;
import com.winnguyen1905.shipping.exception.BusinessLogicException;
import com.winnguyen1905.shipping.exception.ResourceNotFoundException;
import com.winnguyen1905.shipping.persistance.entity.EShippingCarrier;
import com.winnguyen1905.shipping.persistance.entity.EShippingMethod;
import com.winnguyen1905.shipping.persistance.repository.ShippingCarrierRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingMethodRepository;
import com.winnguyen1905.shipping.secure.TAccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CarrierServiceImpl implements CarrierService {

    private final ShippingCarrierRepository carrierRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final ObjectMapper objectMapper;

    @Override
    public CarrierResponse createCarrier(CreateCarrierRequest request, TAccountRequest accountRequest) {
        log.info("Creating carrier: {} for account: {}", request.getCarrierName(), accountRequest.username());
        
        // Validate request
        validateCreateCarrierRequest(request);
        
        // Check if carrier code or name already exists
        if (carrierRepository.existsByCarrierCode(request.getCarrierCode())) {
            throw new BadRequestException("Carrier code already exists: " + request.getCarrierCode());
        }
        if (carrierRepository.existsByCarrierName(request.getCarrierName())) {
            throw new BadRequestException("Carrier name already exists: " + request.getCarrierName());
        }
        
        // Create carrier entity
        EShippingCarrier carrier = EShippingCarrier.builder()
                .carrierName(request.getCarrierName())
                .carrierCode(request.getCarrierCode().toUpperCase())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .apiEndpoint(request.getApiEndpoint())
                .supportedCountries(convertListToJson(request.getSupportedCountries()))
                .configuration(convertConfigurationToJson(request.getConfiguration()))
                .build();
        
        carrier = carrierRepository.save(carrier);
        
        log.info("Carrier created successfully with ID: {}", carrier.getCarrierId());
        return mapToCarrierResponse(carrier);
    }

    @Override
    @Transactional(readOnly = true)
    public CarrierResponse getCarrierById(Integer id, TAccountRequest accountRequest) {
        log.info("Getting carrier by ID: {} for account: {}", id, accountRequest.username());
        
        EShippingCarrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + id));
        
        return mapToCarrierResponse(carrier);
    }

    @Override
    public CarrierResponse updateCarrier(Integer id, CreateCarrierRequest request, TAccountRequest accountRequest) {
        log.info("Updating carrier ID: {} for account: {}", id, accountRequest.username());
        
        // Validate request
        validateCreateCarrierRequest(request);
        
        EShippingCarrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + id));
        
        // Check if carrier code or name already exists (excluding current carrier)
        if (!carrier.getCarrierCode().equals(request.getCarrierCode()) && 
            carrierRepository.existsByCarrierCode(request.getCarrierCode())) {
            throw new BadRequestException("Carrier code already exists: " + request.getCarrierCode());
        }
        if (!carrier.getCarrierName().equals(request.getCarrierName()) && 
            carrierRepository.existsByCarrierName(request.getCarrierName())) {
            throw new BadRequestException("Carrier name already exists: " + request.getCarrierName());
        }
        
        // Update carrier
        carrier.setCarrierName(request.getCarrierName());
        carrier.setCarrierCode(request.getCarrierCode().toUpperCase());
        carrier.setIsActive(request.getIsActive() != null ? request.getIsActive() : carrier.getIsActive());
        carrier.setApiEndpoint(request.getApiEndpoint());
        carrier.setSupportedCountries(convertListToJson(request.getSupportedCountries()));
        carrier.setConfiguration(convertConfigurationToJson(request.getConfiguration()));
        
        carrier = carrierRepository.save(carrier);
        
        log.info("Carrier updated successfully with ID: {}", carrier.getCarrierId());
        return mapToCarrierResponse(carrier);
    }

    @Override
    public void deleteCarrier(Integer id, TAccountRequest accountRequest) {
        log.info("Deleting carrier ID: {} for account: {}", id, accountRequest.username());
        
        EShippingCarrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + id));
        
        // Check if carrier has active shipments or methods
        long activeMethodsCount = shippingMethodRepository.countByCarrierCarrierIdAndIsActive(id, true);
        if (activeMethodsCount > 0) {
            throw new BusinessLogicException("Cannot delete carrier with active shipping methods");
        }
        
        // Soft delete by deactivating
        carrier.setIsActive(false);
        carrierRepository.save(carrier);
        
        log.info("Carrier deactivated successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarrierResponse> getAllCarriers(Boolean isActive, String name, String code, String country,
                                              Pageable pageable, TAccountRequest accountRequest) {
        log.info("Getting all carriers with filters - isActive: {}, name: {}, code: {}, country: {} for account: {}", 
                isActive, name, code, country, accountRequest.username());
        
        // Build query based on filters
        List<EShippingCarrier> carriers;
        
        if (isActive != null) {
            carriers = carrierRepository.findByIsActiveOrderByCarrierName(isActive);
        } else {
            carriers = carrierRepository.findAll();
        }
        
        // Apply additional filters
        if (name != null && !name.trim().isEmpty()) {
            carriers = carriers.stream()
                    .filter(c -> c.getCarrierName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (code != null && !code.trim().isEmpty()) {
            carriers = carriers.stream()
                    .filter(c -> c.getCarrierCode().toLowerCase().contains(code.toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (country != null && !country.trim().isEmpty()) {
            carriers = carriers.stream()
                    .filter(c -> c.getSupportedCountries() != null && 
                               c.getSupportedCountries().toLowerCase().contains(country.toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), carriers.size());
        
        List<CarrierResponse> carrierResponses = carriers.subList(start, end).stream()
                .map(this::mapToCarrierResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(carrierResponses, pageable, carriers.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarrierResponse> getActiveCarriers(TAccountRequest accountRequest) {
        log.info("Getting active carriers for account: {}", accountRequest.username());
        
        List<EShippingCarrier> carriers = carrierRepository.findAllActiveCarriers();
        
        return carriers.stream()
                .map(this::mapToCarrierResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CarrierResponse activateCarrier(Integer id, TAccountRequest accountRequest) {
        log.info("Activating carrier ID: {} for account: {}", id, accountRequest.username());
        
        EShippingCarrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + id));
        
        carrier.setIsActive(true);
        carrier = carrierRepository.save(carrier);
        
        log.info("Carrier activated successfully with ID: {}", id);
        return mapToCarrierResponse(carrier);
    }

    @Override
    public CarrierResponse deactivateCarrier(Integer id, TAccountRequest accountRequest) {
        log.info("Deactivating carrier ID: {} for account: {}", id, accountRequest.username());
        
        EShippingCarrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + id));
        
        carrier.setIsActive(false);
        carrier = carrierRepository.save(carrier);
        
        log.info("Carrier deactivated successfully with ID: {}", id);
        return mapToCarrierResponse(carrier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarrierResponse.ShippingMethodResponse> getCarrierShippingMethods(Integer id, TAccountRequest accountRequest) {
        log.info("Getting shipping methods for carrier ID: {} for account: {}", id, accountRequest.username());
        
        EShippingCarrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + id));
        
        List<EShippingMethod> methods = shippingMethodRepository.findByCarrierCarrierIdAndIsActive(id, true);
        
        return methods.stream()
                .map(this::mapToShippingMethodResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CarrierResponse.ConnectionTestResponse testCarrierConnection(Integer id, TAccountRequest accountRequest) {
        log.info("Testing carrier connection for ID: {} for account: {}", id, accountRequest.username());
        
        EShippingCarrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with ID: " + id));
        
        // Simulate connection test (in real implementation, would call carrier API)
        long startTime = System.currentTimeMillis();
        
        try {
            // Simulate API call delay
            Thread.sleep(100);
            
            boolean isSuccessful = carrier.getApiEndpoint() != null && !carrier.getApiEndpoint().isEmpty();
            long responseTime = System.currentTimeMillis() - startTime;
            
            return CarrierResponse.ConnectionTestResponse.builder()
                    .isSuccessful(isSuccessful)
                    .responseTimeMs(responseTime)
                    .testMessage(isSuccessful ? "Connection successful" : "Connection failed")
                    .errorMessage(isSuccessful ? null : "API endpoint not configured")
                    .testedAt(Instant.now())
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CarrierResponse.ConnectionTestResponse.builder()
                    .isSuccessful(false)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .testMessage("Connection test interrupted")
                    .errorMessage(e.getMessage())
                    .testedAt(Instant.now())
                    .build();
        }
    }

    private void validateCreateCarrierRequest(CreateCarrierRequest request) {
        if (request.getCarrierName() == null || request.getCarrierName().trim().isEmpty()) {
            throw new BadRequestException("Carrier name is required");
        }
        if (request.getCarrierCode() == null || request.getCarrierCode().trim().isEmpty()) {
            throw new BadRequestException("Carrier code is required");
        }
        if (request.getCarrierCode().length() > 20) {
            throw new BadRequestException("Carrier code cannot exceed 20 characters");
        }
        if (request.getCarrierName().length() > 100) {
            throw new BadRequestException("Carrier name cannot exceed 100 characters");
        }
    }

    private CarrierResponse mapToCarrierResponse(EShippingCarrier carrier) {
        return CarrierResponse.builder()
                .carrierId(carrier.getCarrierId())
                .carrierName(carrier.getCarrierName())
                .carrierCode(carrier.getCarrierCode())
                .isActive(carrier.getIsActive())
                .supportedCountries(convertJsonToList(carrier.getSupportedCountries()))
                .apiEndpoint(carrier.getApiEndpoint())
                .configuration(convertJsonToConfiguration(carrier.getConfiguration()))
                .createdAt(carrier.getCreatedAt())
                .updatedAt(carrier.getUpdatedAt())
                .build();
    }

    private CarrierResponse.ShippingMethodResponse mapToShippingMethodResponse(EShippingMethod method) {
        return CarrierResponse.ShippingMethodResponse.builder()
                .methodId(method.getMethodId())
                .methodName(method.getMethodName())
                .methodCode(method.getMethodCode())
                .serviceType(method.getServiceType())
                .baseRate(method.getBaseRate())
                .perKgRate(method.getPerKgRate())
                .perItemRate(method.getPerItemRate())
                .estimatedDaysMin(method.getEstimatedDaysMin())
                .estimatedDaysMax(method.getEstimatedDaysMax())
                .isActive(method.getIsActive())
                .zoneName(method.getZone().getZoneName())
                .zoneCode(method.getZone().getZoneCode())
                .build();
    }

    private String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Error converting list to JSON", e);
            return "[]";
        }
    }

    private List<String> convertJsonToList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to list", e);
            return List.of();
        }
    }

    private String convertConfigurationToJson(CreateCarrierRequest.CarrierConfiguration config) {
        if (config == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Error converting configuration to JSON", e);
            return "{}";
        }
    }

    private CarrierResponse.CarrierConfiguration convertJsonToConfiguration(String json) {
        if (json == null || json.trim().isEmpty()) {
            return CarrierResponse.CarrierConfiguration.builder().build();
        }
        try {
            return objectMapper.readValue(json, CarrierResponse.CarrierConfiguration.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to configuration", e);
            return CarrierResponse.CarrierConfiguration.builder().build();
        }
    }
} 
