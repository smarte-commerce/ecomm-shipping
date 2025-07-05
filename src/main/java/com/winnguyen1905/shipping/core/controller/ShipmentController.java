package com.winnguyen1905.shipping.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.winnguyen1905.shipping.core.model.request.CreateShipmentRequest;
import com.winnguyen1905.shipping.core.model.request.UpdateShipmentRequest;
import com.winnguyen1905.shipping.core.model.response.ShipmentResponse;
import com.winnguyen1905.shipping.core.service.ShipmentService;
import com.winnguyen1905.shipping.common.enums.ShipmentStatus;
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
@RequestMapping("/api/v1/shipments")
@Tag(name = "Shipment Management", description = "APIs for managing shipments")
public class ShipmentController {

    @Autowired
    private ShipmentService shipmentService;

    @PostMapping
    @Operation(summary = "Create a new shipment", 
               description = "Creates a new shipment with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Shipment created successfully",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        ShipmentResponse response = shipmentService.createShipment(request, accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shipment by ID", 
               description = "Retrieves a specific shipment by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment found",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Shipment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShipmentResponse> getShipment(
            @Parameter(description = "Shipment ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        ShipmentResponse response = shipmentService.getShipmentById(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update shipment", 
               description = "Updates an existing shipment with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment updated successfully",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Shipment not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShipmentResponse> updateShipment(
            @Parameter(description = "Shipment ID") @PathVariable Long id,
            @Valid @RequestBody UpdateShipmentRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        ShipmentResponse response = shipmentService.updateShipment(id, request, accountRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel shipment", 
               description = "Cancels a shipment (soft delete)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Shipment cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Shipment not found"),
        @ApiResponse(responseCode = "400", description = "Shipment cannot be cancelled"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> cancelShipment(
            @Parameter(description = "Shipment ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        shipmentService.cancelShipment(id, accountRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all shipments", 
               description = "Retrieves a paginated list of shipments with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<ShipmentResponse>> getAllShipments(
            @Parameter(description = "Order ID filter") @RequestParam(required = false) Long orderId,
            @Parameter(description = "Shipment status filter") @RequestParam(required = false) ShipmentStatus status,
            @Parameter(description = "Carrier ID filter") @RequestParam(required = false) Integer carrierId,
            @Parameter(description = "Tracking number filter") @RequestParam(required = false) String trackingNumber,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AccountRequest TAccountRequest accountRequest) {
        Page<ShipmentResponse> response = shipmentService.getAllShipments(
            orderId, status, carrierId, trackingNumber, pageable, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get shipments by order ID", 
               description = "Retrieves all shipments associated with a specific order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ShipmentResponse>> getShipmentsByOrderId(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @AccountRequest TAccountRequest accountRequest) {
        List<ShipmentResponse> response = shipmentService.getShipmentsByOrderId(orderId, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tracking/{trackingNumber}")
    @Operation(summary = "Track shipment", 
               description = "Tracks a shipment using its tracking number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment tracking information retrieved",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tracking number not found")
    })
    public ResponseEntity<ShipmentResponse> trackShipment(
            @Parameter(description = "Tracking number") @PathVariable String trackingNumber) {
        ShipmentResponse response = shipmentService.trackShipment(trackingNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/generate-label")
    @Operation(summary = "Generate shipping label", 
               description = "Generates a shipping label for the specified shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Label generated successfully"),
        @ApiResponse(responseCode = "404", description = "Shipment not found"),
        @ApiResponse(responseCode = "400", description = "Label cannot be generated"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShipmentResponse> generateShippingLabel(
            @Parameter(description = "Shipment ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        ShipmentResponse response = shipmentService.generateShippingLabel(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/ship")
    @Operation(summary = "Mark shipment as shipped", 
               description = "Marks a shipment as shipped and sets the shipped date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment marked as shipped",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Shipment not found"),
        @ApiResponse(responseCode = "400", description = "Shipment cannot be marked as shipped"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShipmentResponse> markShipmentAsShipped(
            @Parameter(description = "Shipment ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        ShipmentResponse response = shipmentService.markShipmentAsShipped(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "Mark shipment as delivered", 
               description = "Marks a shipment as delivered")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shipment marked as delivered",
                    content = @Content(schema = @Schema(implementation = ShipmentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Shipment not found"),
        @ApiResponse(responseCode = "400", description = "Shipment cannot be marked as delivered"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ShipmentResponse> markShipmentAsDelivered(
            @Parameter(description = "Shipment ID") @PathVariable Long id,
            @Parameter(description = "Delivery signature") @RequestParam(required = false) String signature,
            @AccountRequest TAccountRequest accountRequest) {
        ShipmentResponse response = shipmentService.markShipmentAsDelivered(id, signature, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get shipment tracking events", 
               description = "Retrieves all tracking events for a specific shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tracking events retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Shipment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ShipmentResponse.TrackingEventResponse>> getShipmentTrackingEvents(
            @Parameter(description = "Shipment ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        List<ShipmentResponse.TrackingEventResponse> response = 
            shipmentService.getShipmentTrackingEvents(id, accountRequest);
        return ResponseEntity.ok(response);
    }
}
