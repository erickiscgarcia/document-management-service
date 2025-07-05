package com.garbed.document_management_service.exception;

public class InvalidMetadataException extends RuntimeException {
  public InvalidMetadataException(String message, Throwable cause) {
    super(message, cause);
  }
}
