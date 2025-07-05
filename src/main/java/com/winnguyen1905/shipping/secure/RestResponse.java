package com.winnguyen1905.shipping.secure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Builder;

@Builder
@JsonInclude(value = Include.NON_NULL)
public record RestResponse<T>(
    Integer statusCode,
    String error,
    Object message,
    T data)  {
  @Builder
  public RestResponse(Integer statusCode, String error, Object message, T data) {
    this.statusCode = statusCode;
    this.error = error;
    this.message = message;
    this.data = data;
  }
}
