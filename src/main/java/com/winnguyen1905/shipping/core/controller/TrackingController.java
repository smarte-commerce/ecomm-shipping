package com.winnguyen1905.shipping.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.winnguyen1905.shipping.core.model.request.CreateTrackingEventRequest;
import com.winnguyen1905.shipping.core.model.response.TrackingEventResponse;
import com.winnguyen1905.shipping.core.service.TrackingService;
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
@RequestMapping("/api/v1/tracking")
@Tag(name = "Tracking Management", description = "APIs for managing shipment tracking and events")
public class TrackingController {

    @Autowired
    private TrackingService trackingService;

    @PostMapping("/events")
    @Operation(summary = "Create tracking event", 
               description = "Creates a new tracking event for a shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tracking event created successfully",
                    content = @Content(schema = @Schema(implementation = TrackingEventResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TrackingEventResponse> createTrackingEvent(
            @Valid @RequestBody CreateTrackingEventRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        TrackingEventResponse response = trackingService.createTrackingEvent(request, accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/events/{id}")
    @Operation(summary = "Get tracking event by ID", 
               description = "Retrieves a specific tracking event by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tracking event found",
                    content = @Content(schema = @Schema(implementation = TrackingEventResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tracking event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TrackingEventResponse> getTrackingEvent(
            @Parameter(description = "Tracking event ID") @PathVariable Long id,
            @AccountRequest TAccountRequest accountRequest) {
        TrackingEventResponse response = trackingService.getTrackingEventById(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/events")
    @Operation(summary = "Get tracking events", 
               description = "Retrieves tracking events with optional filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tracking events retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<TrackingEventResponse>> getTrackingEvents(
            @Parameter(description = "Shipment ID filter") @RequestParam(required = false) Long shipmentId,
            @Parameter(description = "Tracking number filter") @RequestParam(required = false) String trackingNumber,
            @Parameter(description = "Event type filter") @RequestParam(required = false) String eventType,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AccountRequest TAccountRequest accountRequest) {
        Page<TrackingEventResponse> response = trackingService.getTrackingEvents(
            shipmentId, trackingNumber, eventType, pageable, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shipment/{shipmentId}/events")
    @Operation(summary = "Get tracking events by shipment ID", 
               description = "Retrieves all tracking events for a specific shipment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tracking events retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Shipment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<TrackingEventResponse>> getTrackingEventsByShipment(
            @Parameter(description = "Shipment ID") @PathVariable Long shipmentId,
            @AccountRequest TAccountRequest accountRequest) {
        List<TrackingEventResponse> response = trackingService.getTrackingEventsByShipment(shipmentId, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{trackingNumber}/events")
    @Operation(summary = "Get tracking events by tracking number", 
               description = "Retrieves all tracking events for a specific tracking number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tracking events retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Tracking number not found")
    })
    public ResponseEntity<List<TrackingEventResponse>> getTrackingEventsByNumber(
            @Parameter(description = "Tracking number") @PathVariable String trackingNumber) {
        List<TrackingEventResponse> response = trackingService.getTrackingEventsByNumber(trackingNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{trackingNumber}/status")
    @Operation(summary = "Get tracking status", 
               description = "Gets the current status of a shipment by tracking number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tracking status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Tracking number not found")
    })
    public ResponseEntity<TrackingEventResponse.TrackingStatus> getTrackingStatus(
            @Parameter(description = "Tracking number") @PathVariable String trackingNumber) {
        TrackingEventResponse.TrackingStatus response = trackingService.getTrackingStatus(trackingNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh/{trackingNumber}")
    @Operation(summary = "Refresh tracking information", 
               description = "Refreshes tracking information from the carrier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tracking information refreshed successfully"),
        @ApiResponse(responseCode = "404", description = "Tracking number not found"),
        @ApiResponse(responseCode = "400", description = "Unable to refresh tracking information"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<TrackingEventResponse>> refreshTrackingInfo(
            @Parameter(description = "Tracking number") @PathVariable String trackingNumber,
            @AccountRequest TAccountRequest accountRequest) {
        List<TrackingEventResponse> response = trackingService.refreshTrackingInfo(trackingNumber, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch-refresh")
    @Operation(summary = "Batch refresh tracking information", 
               description = "Refreshes tracking information for multiple tracking numbers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch tracking refresh completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TrackingEventResponse.BatchRefreshResponse> batchRefreshTrackingInfo(
            @Parameter(description = "List of tracking numbers") @RequestBody List<String> trackingNumbers,
            @AccountRequest TAccountRequest accountRequest) {
        TrackingEventResponse.BatchRefreshResponse response = 
            trackingService.batchRefreshTrackingInfo(trackingNumbers, accountRequest);
        return ResponseEntity.ok(response);
    }
} 
