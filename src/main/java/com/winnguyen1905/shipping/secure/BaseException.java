package com.winnguyen1905.shipping.secure;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class BaseException extends RuntimeException {
  private String message;
  private Integer code;
  private Object error;

  public BaseException(String message) {
    this.message = message;
    this.code = 400;
    this.error = "Exception occurs";
  }

  public BaseException(String message, int code) {
    this.message = message;
    this.code = code;
    this.error = "Exception occurs";
  }
}
