package com.winnguyen1905.shipping.secure;

import java.util.UUID;

import lombok.Builder;

@Builder
public record TAccountRequest(
    UUID id,
    String username,
    AccountType accountType,
    UUID socketClientId, RegionPartition region) {

  @Builder
  public TAccountRequest(
      UUID id,
      String username,
      AccountType accountType,
      UUID socketClientId, RegionPartition region) {
    this.id = id;
    this.username = username;
    this.accountType = accountType;
    this.region = region;
    this.socketClientId = socketClientId;
  }
}
