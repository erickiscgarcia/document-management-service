package com.garbed.document_management_service.exception;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * GlobalExceptionHandler class used for requests regarding GlobalExceptionHandler in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/30/25
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(FileUploadException.class)
  public Mono<ResponseEntity<ApiErrorResponse>> handleFileUploadException(FileUploadException ex) {
    log.error("Error during file upload: {}", ex.getMessage(), ex);
    return Mono.just(
        buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to process file upload. Please try again later."));
  }

  @ExceptionHandler(MinioUploadException.class)
  public Mono<ResponseEntity<ApiErrorResponse>> handleMinioUploadException(
      MinioUploadException ex) {
    log.error("MinIO storage failure: {}", ex.getMessage(), ex);
    return Mono.just(
        buildResponse(HttpStatus.BAD_GATEWAY, "Storage error. Please try again later."));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public Mono<ResponseEntity<ApiErrorResponse>> handleNotFound(ResourceNotFoundException ex) {
    log.warn("ResourceNotFound: {}", ex.getMessage());
    return Mono.just(buildResponse(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(InvalidMetadataException.class)
  public Mono<ResponseEntity<ApiErrorResponse>> handleInvalidMetadata(InvalidMetadataException ex) {
    log.warn("Invalid metadata input: {}", ex.getMessage());
    return Mono.just(buildResponse(HttpStatus.BAD_REQUEST, "Invalid metadata JSON format."));
  }

  @ExceptionHandler(ServerWebInputException.class)
  public Mono<ResponseEntity<ApiErrorResponse>> handleBadRequest(ServerWebInputException ex) {
    log.warn("Invalid input received: {}", ex.getReason());
    return Mono.just(buildResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters."));
  }

  @ExceptionHandler(DecodingException.class)
  public Mono<ResponseEntity<ApiErrorResponse>> handleDecoding(DecodingException ex) {
    log.warn("Failed to parse incoming JSON: {}", ex.getMessage());
    return Mono.just(buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body."));
  }

  @ExceptionHandler(MaxFileSizeException.class)
  public Mono<ResponseEntity<ApiErrorResponse>> handleMaxFile(MaxFileSizeException ex) {
    log.warn("Invalid file size: {}", ex.getMessage());
    return Mono.just(buildResponse(HttpStatus.BAD_REQUEST, "File size exceeded."));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<ApiErrorResponse>> handleUnexpected(Exception ex) {
    log.error("Unexpected server error: {}", ex.getMessage(), ex);
    return Mono.just(
        buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected error occurred. Please try again later."));
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message) {
    ApiErrorResponse error =
        ApiErrorResponse.builder()
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .timestamp(LocalDateTime.now())
            .traceId(UUID.randomUUID().toString())
            .build();

    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(error);
  }
}
