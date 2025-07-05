package com.winnguyen1905.shipping.core.model.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneResponse {

    @JsonProperty("zone_id")
    private Integer zoneId;

    @JsonProperty("zone_name")
    private String zoneName;

    @JsonProperty("zone_code")
    private String zoneCode;

    @JsonProperty("countries")
    private List<String> countries;

    @JsonProperty("states_provinces")
    private List<String> statesProvinces;

    @JsonProperty("zip_codes")
    private List<String> zipCodes;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("shipping_methods_count")
    private Integer shippingMethodsCount;
} 
