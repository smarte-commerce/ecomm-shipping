package com.winnguyen1905.shipping.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.winnguyen1905.shipping.core.model.request.CreateShippingMethodRequest;
import com.winnguyen1905.shipping.core.model.response.ShippingMethodResponse;
import com.winnguyen1905.shipping.core.service.ShippingMethodService;
import com.winnguyen1905.shipping.common.enums.ServiceType;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping-methods")
@Tag(name = "Shipping Method Management", description = "APIs for managing shipping methods")
public class ShippingMethodController {

    @Autowired
    private ShippingMethodService shippingMethodService;

    @PostMapping
    @Operation(summary = "Create a new shipping method", 
               description = "Creates a new shipping method for a carrier and zone")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Shipping method created successfully",
                    content = @Content(schema = @Schema(implementation = ShippingMethodResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Shipping method already exists")
    })
    public ResponseEntity<ShippingMethodResponse> createShippingMethod(
            @Valid @RequestBody CreateShippingMethodRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        ShippingMethodResponse response = shippingMethodService.createShippingMethod(request, accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shipping method by ID", 
               description = "Retrieves a specific shipping method by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipping method found",
                    content = @Content(schema = @Schema(implementation = ShippingMethodResponse.class))),
        @ApiResponse(responseCode = "404", description = "Shipping method not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShippingMethodResponse> getShippingMethod(
            @Parameter(description = "Shipping method ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        ShippingMethodResponse response = shippingMethodService.getShippingMethodById(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update shipping method", 
               description = "Updates an existing shipping method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipping method updated successfully",
                    content = @Content(schema = @Schema(implementation = ShippingMethodResponse.class))),
        @ApiResponse(responseCode = "404", description = "Shipping method not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShippingMethodResponse> updateShippingMethod(
            @Parameter(description = "Shipping method ID") @PathVariable Integer id,
            @Valid @RequestBody CreateShippingMethodRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        ShippingMethodResponse response = shippingMethodService.updateShippingMethod(id, request, accountRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete shipping method", 
               description = "Deletes a shipping method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Shipping method deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Shipping method not found"),
        @ApiResponse(responseCode = "400", description = "Shipping method cannot be deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteShippingMethod(
            @Parameter(description = "Shipping method ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        shippingMethodService.deleteShippingMethod(id, accountRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all shipping methods", 
               description = "Retrieves a paginated list of shipping methods with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipping methods retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<ShippingMethodResponse>> getAllShippingMethods(
            @Parameter(description = "Carrier ID filter") @RequestParam(required = false) Integer carrierId,
            @Parameter(description = "Zone ID filter") @RequestParam(required = false) Integer zoneId,
            @Parameter(description = "Service type filter") @RequestParam(required = false) ServiceType serviceType,
            @Parameter(description = "Active status filter") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AccountRequest TAccountRequest accountRequest) {
        Page<ShippingMethodResponse> response = shippingMethodService.getAllShippingMethods(
            carrierId, zoneId, serviceType, isActive, pageable, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/carrier/{carrierId}")
    @Operation(summary = "Get shipping methods by carrier", 
               description = "Retrieves all shipping methods for a specific carrier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipping methods retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ShippingMethodResponse>> getShippingMethodsByCarrier(
            @Parameter(description = "Carrier ID") @PathVariable Integer carrierId,
            @AccountRequest TAccountRequest accountRequest) {
        List<ShippingMethodResponse> response = shippingMethodService.getShippingMethodsByCarrier(carrierId, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/zone/{zoneId}")
    @Operation(summary = "Get shipping methods by zone", 
               description = "Retrieves all shipping methods for a specific zone")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipping methods retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ShippingMethodResponse>> getShippingMethodsByZone(
            @Parameter(description = "Zone ID") @PathVariable Integer zoneId,
            @AccountRequest TAccountRequest accountRequest) {
        List<ShippingMethodResponse> response = shippingMethodService.getShippingMethodsByZone(zoneId, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available shipping methods", 
               description = "Retrieves available shipping methods for given criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available shipping methods retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ShippingMethodResponse.ShippingMethodAvailability>> getAvailableShippingMethods(
            @Parameter(description = "Carrier ID filter") @RequestParam(required = false) Integer carrierId,
            @Parameter(description = "Zone ID filter") @RequestParam(required = false) Integer zoneId,
            @Parameter(description = "Shipment weight") @RequestParam BigDecimal weight,
            @Parameter(description = "Order value") @RequestParam BigDecimal orderValue,
            @AccountRequest TAccountRequest accountRequest) {
        List<ShippingMethodResponse.ShippingMethodAvailability> response = 
            shippingMethodService.getAvailableShippingMethods(carrierId, zoneId, weight, orderValue, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate shipping method", 
               description = "Activates a shipping method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipping method activated successfully",
                    content = @Content(schema = @Schema(implementation = ShippingMethodResponse.class))),
        @ApiResponse(responseCode = "404", description = "Shipping method not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShippingMethodResponse> activateShippingMethod(
            @Parameter(description = "Shipping method ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        ShippingMethodResponse response = shippingMethodService.activateShippingMethod(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate shipping method", 
               description = "Deactivates a shipping method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipping method deactivated successfully",
                    content = @Content(schema = @Schema(implementation = ShippingMethodResponse.class))),
        @ApiResponse(responseCode = "404", description = "Shipping method not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShippingMethodResponse> deactivateShippingMethod(
            @Parameter(description = "Shipping method ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        ShippingMethodResponse response = shippingMethodService.deactivateShippingMethod(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/statistics")
    @Operation(summary = "Get shipping method statistics", 
               description = "Retrieves statistics for a specific shipping method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Shipping method not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShippingMethodResponse.ShippingMethodStatistics> getShippingMethodStatistics(
            @Parameter(description = "Shipping method ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        ShippingMethodResponse.ShippingMethodStatistics response = 
            shippingMethodService.getShippingMethodStatistics(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk-update-rates")
    @Operation(summary = "Bulk update shipping method rates", 
               description = "Updates rates for multiple shipping methods")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bulk update completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShippingMethodResponse.BulkUpdateResult> bulkUpdateRates(
            @Parameter(description = "List of shipping method IDs") @RequestParam List<Integer> methodIds,
            @Parameter(description = "Rate multiplier") @RequestParam BigDecimal rateMultiplier,
            @AccountRequest TAccountRequest accountRequest) {
        ShippingMethodResponse.BulkUpdateResult response = 
            shippingMethodService.bulkUpdateRates(methodIds, rateMultiplier, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone shipping method", 
               description = "Clones a shipping method to a different zone")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Shipping method cloned successfully",
                    content = @Content(schema = @Schema(implementation = ShippingMethodResponse.class))),
        @ApiResponse(responseCode = "404", description = "Shipping method not found"),
        @ApiResponse(responseCode = "400", description = "Invalid clone request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShippingMethodResponse> cloneShippingMethod(
            @Parameter(description = "Source shipping method ID") @PathVariable Integer id,
            @Parameter(description = "Target zone ID") @RequestParam Integer targetZoneId,
            @AccountRequest TAccountRequest accountRequest) {
        ShippingMethodResponse response = shippingMethodService.cloneShippingMethod(id, targetZoneId, accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
} 
