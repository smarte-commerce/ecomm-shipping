package com.winnguyen1905.shipping.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.winnguyen1905.shipping.core.model.request.CalculateRateRequest;
import com.winnguyen1905.shipping.core.model.response.RateCalculationResponse;
import com.winnguyen1905.shipping.core.service.RateCalculationService;
import com.winnguyen1905.shipping.secure.AccountRequest;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rates")
@Tag(name = "Rate Calculation", description = "APIs for calculating shipping rates")
public class RateCalculationController {

    @Autowired
    private RateCalculationService rateCalculationService;

    @PostMapping("/calculate")
    @Operation(summary = "Calculate shipping rates", 
               description = "Calculates shipping rates for given shipment details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rates calculated successfully",
                    content = @Content(schema = @Schema(implementation = RateCalculationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<RateCalculationResponse> calculateRates(
            @Valid @RequestBody CalculateRateRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        RateCalculationResponse response = rateCalculationService.calculateRates(request, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get rate calculation by ID", 
               description = "Retrieves a specific rate calculation by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rate calculation found",
                    content = @Content(schema = @Schema(implementation = RateCalculationResponse.class))),
        @ApiResponse(responseCode = "404", description = "Rate calculation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<RateCalculationResponse> getRateCalculation(
            @Parameter(description = "Rate calculation ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        RateCalculationResponse response = rateCalculationService.getRateCalculationById(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all rate calculations", 
               description = "Retrieves a paginated list of rate calculations with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rate calculations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<RateCalculationResponse>> getAllRateCalculations(
            @Parameter(description = "Order ID filter") @RequestParam(required = false) Long orderId,
            @Parameter(description = "From ZIP filter") @RequestParam(required = false) String fromZip,
            @Parameter(description = "To ZIP filter") @RequestParam(required = false) String toZip,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AccountRequest TAccountRequest accountRequest) {
        Page<RateCalculationResponse> response = rateCalculationService.getAllRateCalculations(
            orderId, fromZip, toZip, pageable, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get rate calculations by order ID", 
               description = "Retrieves all rate calculations for a specific order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rate calculations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<RateCalculationResponse>> getRateCalculationsByOrderId(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @AccountRequest TAccountRequest accountRequest) {
        List<RateCalculationResponse> response = rateCalculationService.getRateCalculationsByOrderId(orderId, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/quick-estimate")
    @Operation(summary = "Quick rate estimate", 
               description = "Provides a quick estimate without saving the calculation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quick estimate calculated successfully",
                    content = @Content(schema = @Schema(implementation = RateCalculationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<RateCalculationResponse> quickRateEstimate(
            @Valid @RequestBody CalculateRateRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        RateCalculationResponse response = rateCalculationService.quickRateEstimate(request, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-calculate")
    @Operation(summary = "Bulk rate calculation", 
               description = "Calculates rates for multiple shipments in a single request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bulk rates calculated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<RateCalculationResponse>> bulkCalculateRates(
            @Valid @RequestBody List<CalculateRateRequest> requests,
            @AccountRequest TAccountRequest accountRequest) {
        List<RateCalculationResponse> response = rateCalculationService.bulkCalculateRates(requests, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare shipping rates", 
               description = "Compares rates across all available carriers for given shipment details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rate comparison completed successfully",
                    content = @Content(schema = @Schema(implementation = RateCalculationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<RateCalculationResponse> compareRates(
            @Valid @RequestBody CalculateRateRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        RateCalculationResponse response = rateCalculationService.compareRates(request, accountRequest);
        return ResponseEntity.ok(response);
    }
} 
