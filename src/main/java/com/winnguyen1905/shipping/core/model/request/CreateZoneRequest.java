package com.winnguyen1905.shipping.core.model.request;

import java.util.List;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateZoneRequest {

  @NotBlank(message = "Zone name is required")
  @Size(max = 100, message = "Zone name must not exceed 100 characters")
  private String zoneName;

  @NotBlank(message = "Zone code is required")
  @Size(max = 20, message = "Zone code must not exceed 20 characters")
  private String zoneCode;

  @NotEmpty(message = "Countries are required")
  private List<String> countries;

  private List<String> statesProvinces;

  private List<String> zipCodes;

  private Boolean isActive;
}
