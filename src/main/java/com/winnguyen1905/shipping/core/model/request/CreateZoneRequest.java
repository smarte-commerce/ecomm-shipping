package com.winnguyen1905.shipping.core.model.request;

import java.util.List;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateZoneRequest {

  @NotBlank(message = "Zone name is required")
  @Size(max = 100, message = "Zone name must not exceed 100 characters")
  @JsonProperty("zone_name")
  private String zoneName;

  @NotBlank(message = "Zone code is required")
  @Size(max = 20, message = "Zone code must not exceed 20 characters")
  @JsonProperty("zone_code")
  private String zoneCode;

  @NotEmpty(message = "Countries are required")
  @JsonProperty("countries")
  private List<String> countries;

  @JsonProperty("states_provinces")
  private List<String> statesProvinces;

  @JsonProperty("zip_codes")
  private List<String> zipCodes;

  @JsonProperty("is_active")
  private Boolean isActive;
}
