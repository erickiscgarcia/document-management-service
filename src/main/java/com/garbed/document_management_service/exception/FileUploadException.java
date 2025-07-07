package com.garbed.document_management_service.exception;

public class FileUploadException extends RuntimeException {
  public FileUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}
