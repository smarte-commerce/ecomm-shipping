package com.winnguyen1905.shipping.core.model.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneResponse {

    private Integer zoneId;

    private String zoneName;

    private String zoneCode;

    private List<String> countries;

    private List<String> statesProvinces;

    private List<String> zipCodes;

    private Boolean isActive;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer shippingMethodsCount;
} 
