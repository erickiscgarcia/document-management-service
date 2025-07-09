package com.garbed.document_management_service.util;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PageRequest class used for page request in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/30/25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

  @Schema(description = "Page number (0-based)", example = "0")
  @Min(value = 0, message = "Page number must be 0 or greater")
  private int page = 0;

  @Schema(description = "Number of records per page", example = "10")
  @Min(value = 1, message = "Page size must be at least 1")
  private int size = 10;
}
