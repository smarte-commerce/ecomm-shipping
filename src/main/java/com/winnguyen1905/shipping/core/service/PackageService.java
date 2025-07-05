package com.winnguyen1905.shipping.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.winnguyen1905.shipping.core.model.request.CreatePackageRequest;
import com.winnguyen1905.shipping.core.model.response.PackageResponse;
import com.winnguyen1905.shipping.secure.TAccountRequest;

import java.util.List;

public interface PackageService {

    /**
     * Creates a new package
     * @param request The package creation request
     * @param accountRequest The account request for authorization
     * @return The created package response
     */
    PackageResponse createPackage(CreatePackageRequest request, TAccountRequest accountRequest);

    /**
     * Retrieves a package by its ID
     * @param id The package ID
     * @param accountRequest The account request for authorization
     * @return The package response
     */
    PackageResponse getPackageById(Long id, TAccountRequest accountRequest);

    /**
     * Updates an existing package
     * @param id The package ID
     * @param request The package update request
     * @param accountRequest The account request for authorization
     * @return The updated package response
     */
    PackageResponse updatePackage(Long id, CreatePackageRequest request, TAccountRequest accountRequest);

    /**
     * Deletes a package
     * @param id The package ID
     * @param accountRequest The account request for authorization
     */
    void deletePackage(Long id, TAccountRequest accountRequest);

    /**
     * Retrieves all packages with optional filtering
     * @param shipmentId Filter by shipment ID
     * @param packageType Filter by package type
     * @param isFragile Filter by fragile packages
     * @param isHazardous Filter by hazardous packages
     * @param trackingNumber Filter by tracking number
     * @param pageable Pagination parameters
     * @param accountRequest The account request for authorization
     * @return Paginated list of package responses
     */
    Page<PackageResponse> getAllPackages(Long shipmentId, String packageType, Boolean isFragile, 
                                        Boolean isHazardous, String trackingNumber, 
                                        Pageable pageable, TAccountRequest accountRequest);

    /**
     * Retrieves all packages for a specific shipment
     * @param shipmentId The shipment ID
     * @param accountRequest The account request for authorization
     * @return List of package responses
     */
    List<PackageResponse> getPackagesByShipment(Long shipmentId, TAccountRequest accountRequest);

    /**
     * Retrieves package tracking information
     * @param id The package ID
     * @param accountRequest The account request for authorization
     * @return Package tracking information
     */
    PackageResponse.PackageTrackingInfo getPackageTrackingInfo(Long id, TAccountRequest accountRequest);

    /**
     * Retrieves package tracking by tracking number
     * @param trackingNumber The tracking number
     * @return Package tracking information
     */
    PackageResponse.PackageTrackingInfo trackPackageByNumber(String trackingNumber);

    /**
     * Validates package for shipping compliance
     * @param id The package ID
     * @param accountRequest The account request for authorization
     * @return Package validation result
     */
    PackageResponse.PackageValidationResult validatePackage(Long id, TAccountRequest accountRequest);

    /**
     * Bulk validates multiple packages
     * @param packageIds List of package IDs
     * @param accountRequest The account request for authorization
     * @return List of validation results
     */
    List<PackageResponse.PackageValidationResult> bulkValidatePackages(List<Long> packageIds, TAccountRequest accountRequest);

    /**
     * Assigns tracking number to package
     * @param id The package ID
     * @param trackingNumber The tracking number to assign
     * @param accountRequest The account request for authorization
     * @return The updated package response
     */
    PackageResponse assignTrackingNumber(Long id, String trackingNumber, TAccountRequest accountRequest);

    /**
     * Bulk assigns tracking numbers to packages
     * @param packageTrackingMap Map of package ID to tracking number
     * @param accountRequest The account request for authorization
     * @return Bulk operation result
     */
    PackageResponse.BulkPackageOperation bulkAssignTrackingNumbers(java.util.Map<Long, String> packageTrackingMap, 
                                                                  TAccountRequest accountRequest);

    /**
     * Splits a package into multiple packages
     * @param id The source package ID
     * @param newPackageRequests List of new package requests
     * @param accountRequest The account request for authorization
     * @return List of created package responses
     */
    List<PackageResponse> splitPackage(Long id, List<CreatePackageRequest> newPackageRequests, TAccountRequest accountRequest);

    /**
     * Merges multiple packages into one
     * @param packageIds List of package IDs to merge
     * @param mergedPackageRequest The merged package request
     * @param accountRequest The account request for authorization
     * @return The merged package response
     */
    PackageResponse mergePackages(List<Long> packageIds, CreatePackageRequest mergedPackageRequest, TAccountRequest accountRequest);

    /**
     * Gets packages requiring special handling
     * @param accountRequest The account request for authorization
     * @return List of packages requiring special handling
     */
    List<PackageResponse> getSpecialHandlingPackages(TAccountRequest accountRequest);
} 
