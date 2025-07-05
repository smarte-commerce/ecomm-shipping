package com.winnguyen1905.shipping.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
  public ResourceAlreadyExistsException(String message) {
      super(message);
  }
}
