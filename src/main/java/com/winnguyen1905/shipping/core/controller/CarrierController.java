package com.winnguyen1905.shipping.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.winnguyen1905.shipping.core.model.request.CreateCarrierRequest;
import com.winnguyen1905.shipping.core.model.response.CarrierResponse;
import com.winnguyen1905.shipping.core.service.CarrierService;
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
@RequestMapping("/api/v1/carriers")
@Tag(name = "Carrier Management", description = "APIs for managing shipping carriers")
public class CarrierController {

    @Autowired
    private CarrierService carrierService;

    @PostMapping
    @Operation(summary = "Create a new carrier", 
               description = "Creates a new shipping carrier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Carrier created successfully",
                    content = @Content(schema = @Schema(implementation = CarrierResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Carrier already exists")
    })
    public ResponseEntity<CarrierResponse> createCarrier(
            @Valid @RequestBody CreateCarrierRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        CarrierResponse response = carrierService.createCarrier(request, accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get carrier by ID", 
               description = "Retrieves a specific carrier by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Carrier found",
                    content = @Content(schema = @Schema(implementation = CarrierResponse.class))),
        @ApiResponse(responseCode = "404", description = "Carrier not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CarrierResponse> getCarrier(
            @Parameter(description = "Carrier ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        CarrierResponse response = carrierService.getCarrierById(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update carrier", 
               description = "Updates an existing carrier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Carrier updated successfully",
                    content = @Content(schema = @Schema(implementation = CarrierResponse.class))),
        @ApiResponse(responseCode = "404", description = "Carrier not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CarrierResponse> updateCarrier(
            @Parameter(description = "Carrier ID") @PathVariable Integer id,
            @Valid @RequestBody CreateCarrierRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        CarrierResponse response = carrierService.updateCarrier(id, request, accountRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete carrier", 
               description = "Deletes a carrier (soft delete)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Carrier deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Carrier not found"),
        @ApiResponse(responseCode = "400", description = "Carrier cannot be deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteCarrier(
            @Parameter(description = "Carrier ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        carrierService.deleteCarrier(id, accountRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all carriers", 
               description = "Retrieves a paginated list of carriers with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Carriers retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<CarrierResponse>> getAllCarriers(
            @Parameter(description = "Active status filter") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Carrier name filter") @RequestParam(required = false) String name,
            @Parameter(description = "Carrier code filter") @RequestParam(required = false) String code,
            @Parameter(description = "Country filter") @RequestParam(required = false) String country,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AccountRequest TAccountRequest accountRequest) {
        Page<CarrierResponse> response = carrierService.getAllCarriers(
            isActive, name, code, country, pageable, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active carriers", 
               description = "Retrieves all active carriers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active carriers retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<CarrierResponse>> getActiveCarriers(
            @AccountRequest TAccountRequest accountRequest) {
        List<CarrierResponse> response = carrierService.getActiveCarriers(accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate carrier", 
               description = "Activates a carrier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Carrier activated successfully",
                    content = @Content(schema = @Schema(implementation = CarrierResponse.class))),
        @ApiResponse(responseCode = "404", description = "Carrier not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CarrierResponse> activateCarrier(
            @Parameter(description = "Carrier ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        CarrierResponse response = carrierService.activateCarrier(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate carrier", 
               description = "Deactivates a carrier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Carrier deactivated successfully",
                    content = @Content(schema = @Schema(implementation = CarrierResponse.class))),
        @ApiResponse(responseCode = "404", description = "Carrier not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CarrierResponse> deactivateCarrier(
            @Parameter(description = "Carrier ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        CarrierResponse response = carrierService.deactivateCarrier(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/methods")
    @Operation(summary = "Get carrier shipping methods", 
               description = "Retrieves all shipping methods for a specific carrier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipping methods retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Carrier not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<CarrierResponse.ShippingMethodResponse>> getCarrierShippingMethods(
            @Parameter(description = "Carrier ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        List<CarrierResponse.ShippingMethodResponse> response = 
            carrierService.getCarrierShippingMethods(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/test-connection")
    @Operation(summary = "Test carrier connection", 
               description = "Tests the connection to a carrier's API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connection test successful"),
        @ApiResponse(responseCode = "404", description = "Carrier not found"),
        @ApiResponse(responseCode = "400", description = "Connection test failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CarrierResponse.ConnectionTestResponse> testCarrierConnection(
            @Parameter(description = "Carrier ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        CarrierResponse.ConnectionTestResponse response = 
            carrierService.testCarrierConnection(id, accountRequest);
        return ResponseEntity.ok(response);
    }
} 
