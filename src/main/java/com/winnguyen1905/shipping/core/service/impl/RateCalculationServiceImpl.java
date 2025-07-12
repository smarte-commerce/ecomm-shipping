package com.winnguyen1905.shipping.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winnguyen1905.shipping.core.model.request.CalculateRateRequest;
import com.winnguyen1905.shipping.core.model.response.RateCalculationResponse;
import com.winnguyen1905.shipping.core.service.RateCalculationService;
import com.winnguyen1905.shipping.exception.BadRequestException;
import com.winnguyen1905.shipping.exception.ResourceNotFoundException;
import com.winnguyen1905.shipping.persistance.entity.EShippingCarrier;
import com.winnguyen1905.shipping.persistance.entity.EShippingMethod;
import com.winnguyen1905.shipping.persistance.entity.EShippingRateCalculation;
import com.winnguyen1905.shipping.persistance.entity.EShippingZone;
import com.winnguyen1905.shipping.persistance.repository.ShippingCarrierRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingMethodRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingRateCalculationRepository;
import com.winnguyen1905.shipping.persistance.repository.ShippingZoneRepository;
import com.winnguyen1905.shipping.common.enums.ServiceType;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RateCalculationServiceImpl implements RateCalculationService {

    private final ShippingRateCalculationRepository rateCalculationRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final ShippingCarrierRepository carrierRepository;
    private final ShippingZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;

    @Override
    public RateCalculationResponse calculateRates(CalculateRateRequest request, TAccountRequest accountRequest) {
        log.info("Calculating rates for order: {} for account: {}", request.getOrderId(), accountRequest.username());
        
        // Validate request
        validateCalculateRateRequest(request);
        
        // Find applicable zones for destination
        List<EShippingZone> applicableZones = findApplicableZones(request.getToAddress().getCountry(), 
                request.getToAddress().getState(), request.getToAddress().getPostalCode());
        
        if (applicableZones.isEmpty()) {
            throw new BadRequestException("No shipping zones found for destination address");
        }
        
        // Get available shipping methods
        List<EShippingMethod> availableMethods = getAvailableShippingMethods(
                request.getSpecificCarrierId(), 
                applicableZones,
                request.getTotalWeight(),
                request.getTotalValue(),
                request.getRequestedServiceType()
        );
        
        if (availableMethods.isEmpty()) {
            throw new BadRequestException("No shipping methods available for the given criteria");
        }
        
        // Calculate rates for each method
        List<RateCalculationResponse.ShippingRate> calculatedRates = availableMethods.stream()
                .map(method -> calculateRateForMethod(method, request))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RateCalculationResponse.ShippingRate::getTotalRate))
                .collect(Collectors.toList());
        
        // Save calculation
        EShippingRateCalculation calculation = saveRateCalculation(request, calculatedRates);
        
        // Find cheapest and fastest rates
        RateCalculationResponse.ShippingRate cheapestRate = calculatedRates.stream()
                .min(Comparator.comparing(RateCalculationResponse.ShippingRate::getTotalRate))
                .orElse(null);
        
        RateCalculationResponse.ShippingRate fastestRate = calculatedRates.stream()
                .min(Comparator.comparing(RateCalculationResponse.ShippingRate::getEstimatedDaysMax))
                .orElse(null);
        
        log.info("Rate calculation completed with {} available rates", calculatedRates.size());
        
        return RateCalculationResponse.builder()
                .calculationId(calculation.getCalculationId())
                .orderId(request.getOrderId())
                .fromZip(request.getFromAddress().getPostalCode())
                .toZip(request.getToAddress().getPostalCode())
                .totalWeight(request.getTotalWeight())
                .totalValue(request.getTotalValue())
                .packageCount(request.getPackageCount())
                .requestedServiceType(request.getRequestedServiceType())
                .calculatedAt(calculation.getCreatedAt())
                .availableRates(calculatedRates)
                .cheapestRate(cheapestRate)
                .fastestRate(fastestRate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RateCalculationResponse getRateCalculationById(Long id, TAccountRequest accountRequest) {
        log.info("Getting rate calculation by ID: {} for account: {}", id, accountRequest.username());
        
        EShippingRateCalculation calculation = rateCalculationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rate calculation not found with ID: " + id));
        
        return mapToRateCalculationResponse(calculation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RateCalculationResponse> getAllRateCalculations(Long orderId, String fromZip, String toZip,
                                                               Pageable pageable, TAccountRequest accountRequest) {
        log.info("Getting all rate calculations with filters for account: {}", accountRequest.username());
        
        List<EShippingRateCalculation> calculations;
        
        if (orderId != null) {
            calculations = rateCalculationRepository.findByOrderId(orderId);
        } else if (fromZip != null && toZip != null) {
            calculations = rateCalculationRepository.findByFromZipAndToZip(fromZip, toZip);
        } else if (fromZip != null) {
            calculations = rateCalculationRepository.findByFromZip(fromZip);
        } else if (toZip != null) {
            calculations = rateCalculationRepository.findByToZip(toZip);
        } else {
            calculations = rateCalculationRepository.findAll();
        }
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), calculations.size());
        
        List<RateCalculationResponse> responses = calculations.subList(start, end).stream()
                .map(this::mapToRateCalculationResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, calculations.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RateCalculationResponse> getRateCalculationsByOrderId(Long orderId, TAccountRequest accountRequest) {
        log.info("Getting rate calculations by order ID: {} for account: {}", orderId, accountRequest.username());
        
        List<EShippingRateCalculation> calculations = rateCalculationRepository.findByOrderId(orderId);
        
        return calculations.stream()
                .map(this::mapToRateCalculationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RateCalculationResponse quickRateEstimate(CalculateRateRequest request, TAccountRequest accountRequest) {
        log.info("Calculating quick rate estimate for account: {}", accountRequest.username());
        
        // Validate request
        validateCalculateRateRequest(request);
        
        // Find applicable zones for destination
        List<EShippingZone> applicableZones = findApplicableZones(request.getToAddress().getCountry(), 
                request.getToAddress().getState(), request.getToAddress().getPostalCode());
        
        if (applicableZones.isEmpty()) {
            throw new BadRequestException("No shipping zones found for destination address");
        }
        
        // Get available shipping methods
        List<EShippingMethod> availableMethods = getAvailableShippingMethods(
                request.getSpecificCarrierId(), 
                applicableZones,
                request.getTotalWeight(),
                request.getTotalValue(),
                request.getRequestedServiceType()
        );
        
        // Calculate rates for each method
        List<RateCalculationResponse.ShippingRate> calculatedRates = availableMethods.stream()
                .map(method -> calculateRateForMethod(method, request))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RateCalculationResponse.ShippingRate::getTotalRate))
                .collect(Collectors.toList());
        
        // Find cheapest and fastest rates
        RateCalculationResponse.ShippingRate cheapestRate = calculatedRates.stream()
                .min(Comparator.comparing(RateCalculationResponse.ShippingRate::getTotalRate))
                .orElse(null);
        
        RateCalculationResponse.ShippingRate fastestRate = calculatedRates.stream()
                .min(Comparator.comparing(RateCalculationResponse.ShippingRate::getEstimatedDaysMax))
                .orElse(null);
        
        return RateCalculationResponse.builder()
                .fromZip(request.getFromAddress().getPostalCode())
                .toZip(request.getToAddress().getPostalCode())
                .totalWeight(request.getTotalWeight())
                .totalValue(request.getTotalValue())
                .packageCount(request.getPackageCount())
                .requestedServiceType(request.getRequestedServiceType())
                .availableRates(calculatedRates)
                .cheapestRate(cheapestRate)
                .fastestRate(fastestRate)
                .build();
    }

    @Override
    public List<RateCalculationResponse> bulkCalculateRates(List<CalculateRateRequest> requests, TAccountRequest accountRequest) {
        log.info("Bulk calculating rates for {} requests for account: {}", requests.size(), accountRequest.username());
        
        return requests.stream()
                .map(request -> {
                    try {
                        return calculateRates(request, accountRequest);
                    } catch (Exception e) {
                        log.error("Error calculating rates for request: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RateCalculationResponse compareRates(CalculateRateRequest request, TAccountRequest accountRequest) {
        log.info("Comparing rates across all carriers for account: {}", accountRequest.username());
        
        // Validate request
        validateCalculateRateRequest(request);
        
        // Get all active carriers
        List<EShippingCarrier> activeCarriers = carrierRepository.findAllActiveCarriers();
        
        List<RateCalculationResponse.ShippingRate> allRates = new ArrayList<>();
        
        for (EShippingCarrier carrier : activeCarriers) {
            try {
                // Create request for specific carrier
                CalculateRateRequest carrierRequest = CalculateRateRequest.builder()
                        .orderId(request.getOrderId())
                        .fromAddress(request.getFromAddress())
                        .toAddress(request.getToAddress())
                        .totalWeight(request.getTotalWeight())
                        .totalValue(request.getTotalValue())
                        .packageCount(request.getPackageCount())
                        .requestedServiceType(request.getRequestedServiceType())
                        .specificCarrierId(carrier.getCarrierId())
                        .packages(request.getPackages())
                        .build();
                
                RateCalculationResponse carrierResponse = quickRateEstimate(carrierRequest, accountRequest);
                allRates.addAll(carrierResponse.getAvailableRates());
            } catch (Exception e) {
                log.warn("Error calculating rates for carrier {}: {}", carrier.getCarrierName(), e.getMessage());
            }
        }
        
        // Sort by total rate
        allRates.sort(Comparator.comparing(RateCalculationResponse.ShippingRate::getTotalRate));
        
        // Find cheapest and fastest rates
        RateCalculationResponse.ShippingRate cheapestRate = allRates.stream()
                .min(Comparator.comparing(RateCalculationResponse.ShippingRate::getTotalRate))
                .orElse(null);
        
        RateCalculationResponse.ShippingRate fastestRate = allRates.stream()
                .min(Comparator.comparing(RateCalculationResponse.ShippingRate::getEstimatedDaysMax))
                .orElse(null);
        
        return RateCalculationResponse.builder()
                .fromZip(request.getFromAddress().getPostalCode())
                .toZip(request.getToAddress().getPostalCode())
                .totalWeight(request.getTotalWeight())
                .totalValue(request.getTotalValue())
                .packageCount(request.getPackageCount())
                .requestedServiceType(request.getRequestedServiceType())
                .availableRates(allRates)
                .cheapestRate(cheapestRate)
                .fastestRate(fastestRate)
                .build();
    }

    private void validateCalculateRateRequest(CalculateRateRequest request) {
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
        if (!ShippingValidationUtils.isValidZipCode(request.getFromAddress().getPostalCode())) {
            throw new BadRequestException("Invalid from postal code format");
        }
        if (!ShippingValidationUtils.isValidZipCode(request.getToAddress().getPostalCode())) {
            throw new BadRequestException("Invalid to postal code format");
        }
    }

    private List<EShippingZone> findApplicableZones(String country, String state, String postalCode) {
        List<EShippingZone> zones = zoneRepository.findAllActiveZones();
        
        return zones.stream()
                .filter(zone -> isZoneApplicable(zone, country, state, postalCode))
                .collect(Collectors.toList());
    }

    private boolean isZoneApplicable(EShippingZone zone, String country, String state, String postalCode) {
        try {
            // Check country
            List<String> countries = objectMapper.readValue(zone.getCountries(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            
            if (!countries.contains(country.toUpperCase())) {
                return false;
            }
            
            // Check state if specified
            if (zone.getStatesProvinces() != null && !zone.getStatesProvinces().isEmpty()) {
                List<String> states = objectMapper.readValue(zone.getStatesProvinces(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                
                if (!states.isEmpty() && !states.contains(state.toUpperCase())) {
                    return false;
                }
            }
            
            // Check postal code if specified
            if (zone.getZipCodes() != null && !zone.getZipCodes().isEmpty()) {
                List<String> zipCodes = objectMapper.readValue(zone.getZipCodes(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                
                if (!zipCodes.isEmpty()) {
                    return zipCodes.stream().anyMatch(zip -> postalCode.startsWith(zip));
                }
            }
            
            return true;
        } catch (JsonProcessingException e) {
            log.error("Error parsing zone data for zone ID: {}", zone.getZoneId(), e);
            return false;
        }
    }

    private List<EShippingMethod> getAvailableShippingMethods(Integer specificCarrierId, 
                                                             List<EShippingZone> applicableZones,
                                                             BigDecimal totalWeight,
                                                             BigDecimal totalValue,
                                                             ServiceType requestedServiceType) {
        List<EShippingMethod> methods = new ArrayList<>();
        
        for (EShippingZone zone : applicableZones) {
            List<EShippingMethod> zoneMethods;
            
            if (specificCarrierId != null) {
                zoneMethods = shippingMethodRepository.findByCarrierAndZone(specificCarrierId, zone.getZoneId());
            } else {
                zoneMethods = shippingMethodRepository.findByZoneZoneIdAndIsActive(zone.getZoneId(), true);
            }
            
            // Filter by service type if specified
            if (requestedServiceType != null) {
                zoneMethods = zoneMethods.stream()
                        .filter(method -> method.getServiceType().equals(requestedServiceType))
                        .collect(Collectors.toList());
            }
            
            // Filter by weight and value constraints
            zoneMethods = zoneMethods.stream()
                    .filter(method -> 
                            (method.getMinWeight() == null || totalWeight.compareTo(method.getMinWeight()) >= 0) &&
                            (method.getMaxWeight() == null || totalWeight.compareTo(method.getMaxWeight()) <= 0) &&
                            (method.getMinOrderValue() == null || totalValue.compareTo(method.getMinOrderValue()) >= 0) &&
                            (method.getMaxOrderValue() == null || totalValue.compareTo(method.getMaxOrderValue()) <= 0)
                    )
                    .collect(Collectors.toList());
            
            methods.addAll(zoneMethods);
        }
        
        // Remove duplicates
        return methods.stream()
                .collect(Collectors.toMap(
                        EShippingMethod::getMethodId,
                        method -> method,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private RateCalculationResponse.ShippingRate calculateRateForMethod(EShippingMethod method, CalculateRateRequest request) {
        try {
            // Calculate base components
            BigDecimal baseRate = method.getBaseRate();
            BigDecimal weightRate = method.getPerKgRate().multiply(request.getTotalWeight());
            BigDecimal itemRate = method.getPerItemRate().multiply(
                    BigDecimal.valueOf(request.getPackageCount() != null ? request.getPackageCount() : 1));
            
            // Calculate total rate
            BigDecimal totalRate = baseRate.add(weightRate).add(itemRate);
            
            // Apply any additional fees or discounts (can be extended)
            totalRate = applyAdditionalFees(totalRate, method, request);
            
            // Round to 2 decimal places
            totalRate = totalRate.setScale(2, RoundingMode.HALF_UP);
            
            // Calculate estimated delivery date
            LocalDate estimatedDeliveryDate = LocalDate.now().plusDays(method.getEstimatedDaysMax());
            
            return RateCalculationResponse.ShippingRate.builder()
                    .methodId(method.getMethodId())
                    .carrierId(method.getCarrier().getCarrierId())
                    .carrierName(method.getCarrier().getCarrierName())
                    .carrierCode(method.getCarrier().getCarrierCode())
                    .methodName(method.getMethodName())
                    .methodCode(method.getMethodCode())
                    .serviceType(method.getServiceType())
                    .baseRate(baseRate)
                    .weightRate(weightRate)
                    .totalRate(totalRate)
                    .currency("USD")
                    .estimatedDaysMin(method.getEstimatedDaysMin())
                    .estimatedDaysMax(method.getEstimatedDaysMax())
                    .estimatedDeliveryDate(estimatedDeliveryDate.toString())
                    .isAvailable(true)
                    .zoneName(method.getZone().getZoneName())
                    .zoneCode(method.getZone().getZoneCode())
                    .build();
        } catch (Exception e) {
            log.error("Error calculating rate for method {}: {}", method.getMethodId(), e.getMessage());
            
            return RateCalculationResponse.ShippingRate.builder()
                    .methodId(method.getMethodId())
                    .carrierId(method.getCarrier().getCarrierId())
                    .carrierName(method.getCarrier().getCarrierName())
                    .methodName(method.getMethodName())
                    .serviceType(method.getServiceType())
                    .isAvailable(false)
                    .unavailableReason(e.getMessage())
                    .build();
        }
    }

    private BigDecimal applyAdditionalFees(BigDecimal baseRate, EShippingMethod method, CalculateRateRequest request) {
        BigDecimal finalRate = baseRate;
        
        // Apply insurance fee if required
        if (ShippingValidationUtils.requiresInsurance(request.getTotalValue())) {
            BigDecimal insuranceFee = request.getTotalValue().multiply(new BigDecimal("0.01")); // 1% of value
            finalRate = finalRate.add(insuranceFee);
        }
        
        // Apply fuel surcharge (example: 10% fuel surcharge)
        BigDecimal fuelSurcharge = baseRate.multiply(new BigDecimal("0.10"));
        finalRate = finalRate.add(fuelSurcharge);
        
        return finalRate;
    }

    private EShippingRateCalculation saveRateCalculation(CalculateRateRequest request, 
                                                        List<RateCalculationResponse.ShippingRate> calculatedRates) {
        try {
            String ratesJson = objectMapper.writeValueAsString(calculatedRates);
            
            EShippingRateCalculation calculation = EShippingRateCalculation.builder()
                    .orderId(request.getOrderId())
                    .fromZip(request.getFromAddress().getPostalCode())
                    .toZip(request.getToAddress().getPostalCode())
                    .totalWeight(request.getTotalWeight())
                    .totalValue(request.getTotalValue())
                    .packageCount(request.getPackageCount() != null ? request.getPackageCount() : 1)
                    .requestedServiceType(request.getRequestedServiceType() != null ? 
                            request.getRequestedServiceType().name() : null)
                    .calculatedRates(ratesJson)
                    .build();
            
            return rateCalculationRepository.save(calculation);
        } catch (JsonProcessingException e) {
            log.error("Error saving rate calculation", e);
            throw new BadRequestException("Error saving rate calculation");
        }
    }

    private RateCalculationResponse mapToRateCalculationResponse(EShippingRateCalculation calculation) {
        try {
            List<RateCalculationResponse.ShippingRate> rates = objectMapper.readValue(
                    calculation.getCalculatedRates(),
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, RateCalculationResponse.ShippingRate.class));
            
            // Find cheapest and fastest rates
            RateCalculationResponse.ShippingRate cheapestRate = rates.stream()
                    .filter(RateCalculationResponse.ShippingRate::getIsAvailable)
                    .min(Comparator.comparing(RateCalculationResponse.ShippingRate::getTotalRate))
                    .orElse(null);
            
            RateCalculationResponse.ShippingRate fastestRate = rates.stream()
                    .filter(RateCalculationResponse.ShippingRate::getIsAvailable)
                    .min(Comparator.comparing(RateCalculationResponse.ShippingRate::getEstimatedDaysMax))
                    .orElse(null);
            
            return RateCalculationResponse.builder()
                    .calculationId(calculation.getCalculationId())
                    .orderId(calculation.getOrderId())
                    .fromZip(calculation.getFromZip())
                    .toZip(calculation.getToZip())
                    .totalWeight(calculation.getTotalWeight())
                    .totalValue(calculation.getTotalValue())
                    .packageCount(calculation.getPackageCount())
                    .requestedServiceType(calculation.getRequestedServiceType() != null ? 
                            ServiceType.valueOf(calculation.getRequestedServiceType()) : null)
                    .calculatedAt(calculation.getCreatedAt())
                    .availableRates(rates)
                    .cheapestRate(cheapestRate)
                    .fastestRate(fastestRate)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Error parsing calculated rates for calculation ID: {}", calculation.getCalculationId(), e);
            throw new BadRequestException("Error parsing calculated rates");
        }
    }
} 
