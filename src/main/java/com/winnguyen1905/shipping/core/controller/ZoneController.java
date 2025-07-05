package com.winnguyen1905.shipping.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.winnguyen1905.shipping.core.model.request.CreateZoneRequest;
import com.winnguyen1905.shipping.core.model.response.ZoneResponse;
import com.winnguyen1905.shipping.core.service.ZoneService;
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
@RequestMapping("/api/v1/zones")
@Tag(name = "Zone Management", description = "APIs for managing shipping zones")
public class ZoneController {

    @Autowired
    private ZoneService zoneService;

    @PostMapping
    @Operation(summary = "Create a new shipping zone", 
               description = "Creates a new shipping zone with countries and regions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Zone created successfully",
                    content = @Content(schema = @Schema(implementation = ZoneResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "409", description = "Zone already exists")
    })
    public ResponseEntity<ZoneResponse> createZone(
            @Valid @RequestBody CreateZoneRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        ZoneResponse response = zoneService.createZone(request, accountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get zone by ID", 
               description = "Retrieves a specific shipping zone by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Zone found",
                    content = @Content(schema = @Schema(implementation = ZoneResponse.class))),
        @ApiResponse(responseCode = "404", description = "Zone not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ZoneResponse> getZone(
            @Parameter(description = "Zone ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        ZoneResponse response = zoneService.getZoneById(id, accountRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update zone", 
               description = "Updates an existing shipping zone")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Zone updated successfully",
                    content = @Content(schema = @Schema(implementation = ZoneResponse.class))),
        @ApiResponse(responseCode = "404", description = "Zone not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ZoneResponse> updateZone(
            @Parameter(description = "Zone ID") @PathVariable Integer id,
            @Valid @RequestBody CreateZoneRequest request,
            @AccountRequest TAccountRequest accountRequest) {
        ZoneResponse response = zoneService.updateZone(id, request, accountRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete zone", 
               description = "Deletes a shipping zone")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Zone deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Zone not found"),
        @ApiResponse(responseCode = "400", description = "Zone cannot be deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteZone(
            @Parameter(description = "Zone ID") @PathVariable Integer id,
            @AccountRequest TAccountRequest accountRequest) {
        zoneService.deleteZone(id, accountRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all zones", 
               description = "Retrieves a paginated list of shipping zones")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Zones retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<ZoneResponse>> getAllZones(
            @Parameter(description = "Active status filter") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Zone name filter") @RequestParam(required = false) String name,
            @Parameter(description = "Country filter") @RequestParam(required = false) String country,
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @AccountRequest TAccountRequest accountRequest) {
        Page<ZoneResponse> response = zoneService.getAllZones(isActive, name, country, pageable, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active zones", 
               description = "Retrieves all active shipping zones")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active zones retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ZoneResponse>> getActiveZones(
            @AccountRequest TAccountRequest accountRequest) {
        List<ZoneResponse> response = zoneService.getActiveZones(accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/country/{country}")
    @Operation(summary = "Get zones by country", 
               description = "Retrieves all zones that serve a specific country")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Zones retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ZoneResponse>> getZonesByCountry(
            @Parameter(description = "Country code") @PathVariable String country,
            @AccountRequest TAccountRequest accountRequest) {
        List<ZoneResponse> response = zoneService.getZonesByCountry(country, accountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup zone by address", 
               description = "Finds the appropriate zone for a given address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Zone found for address",
                    content = @Content(schema = @Schema(implementation = ZoneResponse.class))),
        @ApiResponse(responseCode = "404", description = "No zone found for address"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ZoneResponse> lookupZone(
            @Parameter(description = "Country code") @RequestParam String country,
            @Parameter(description = "State/Province") @RequestParam(required = false) String state,
            @Parameter(description = "Postal code") @RequestParam(required = false) String postalCode,
            @AccountRequest TAccountRequest accountRequest) {
        ZoneResponse response = zoneService.lookupZone(country, state, postalCode, accountRequest);
        return ResponseEntity.ok(response);
    }
} 
