package com.garbed.document_management_service.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebInputException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  void handleFileUploadException() {
    Throwable mockCause = mock(Throwable.class);
    when(mockCause.getMessage()).thenReturn("Mocked root cause");

    FileUploadException ex = new FileUploadException("Upload exploded", mockCause);

    Mono<ResponseEntity<ApiErrorResponse>> responseMono = handler.handleFileUploadException(ex);

    StepVerifier.create(responseMono)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertNotNull(response.getHeaders().getContentType());
              assertEquals("application/json", response.getHeaders().getContentType().toString());
              assertNotNull(response.getBody());
              assertEquals(
                  "Failed to process file upload. Please try again later.",
                  response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleMinioUploadException() {
    Throwable mockCause = mock(Throwable.class);
    when(mockCause.getMessage()).thenReturn("Mocked root cause");

    MinioUploadException ex = new MinioUploadException("MinIO error", mockCause);

    StepVerifier.create(handler.handleMinioUploadException(ex))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(
                  "Storage error. Please try again later.", response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleResourceNotFound() {
    ResourceNotFoundException ex = new ResourceNotFoundException("Not found");

    StepVerifier.create(handler.handleNotFound(ex))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals("Not found", response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleInvalidMetadata() {
    Throwable mockCause = mock(Throwable.class);
    when(mockCause.getMessage()).thenReturn("Mocked root cause");

    InvalidMetadataException ex = new InvalidMetadataException("Bad JSON", mockCause);

    StepVerifier.create(handler.handleInvalidMetadata(ex))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals("Invalid metadata JSON format.", response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleBadRequest() {
    ServerWebInputException ex = new ServerWebInputException("Invalid param");

    StepVerifier.create(handler.handleBadRequest(ex))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals("Invalid request parameters.", response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleDecoding() {
    DecodingException ex = new DecodingException("Malformed");

    StepVerifier.create(handler.handleDecoding(ex))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals("Malformed request body.", response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleMaxFileSize() {
    MaxFileSizeException ex = new MaxFileSizeException("File size exceeds");

    StepVerifier.create(handler.handleMaxFile(ex))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals("File size exceeded.", response.getBody().getMessage());
            })
        .verifyComplete();
  }

  @Test
  void handleUnexpected() {
    Exception ex = new Exception("Unexpected error");

    StepVerifier.create(handler.handleUnexpected(ex))
        .assertNext(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertNotNull(response.getBody());
              assertEquals(
                  "Unexpected error occurred. Please try again later.",
                  response.getBody().getMessage());
            })
        .verifyComplete();
  }
}
