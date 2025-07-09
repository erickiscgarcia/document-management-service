package com.garbed.document_management_service.exception;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * ApiErrorResponse class used for exception responses in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/30/25
 */
@Data
@Builder
public class ApiErrorResponse {
  private int status;
  private String error;
  private String message;
  private LocalDateTime timestamp;
  private String traceId;
}
