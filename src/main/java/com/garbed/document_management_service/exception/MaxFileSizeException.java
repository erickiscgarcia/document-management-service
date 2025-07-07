package com.garbed.document_management_service.exception;

public class MaxFileSizeException extends RuntimeException {
  public MaxFileSizeException(String message) {
    super(message);
  }
}
