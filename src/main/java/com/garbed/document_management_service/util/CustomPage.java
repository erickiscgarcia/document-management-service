package com.garbed.document_management_service.util;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * CustomPage class used for paginated responses in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/30/25
 */
@Data
@AllArgsConstructor
@Schema(description = "Paginated response")
public class CustomPage<T> {

  @Schema(description = "List of items in the current page")
  private List<T> content;

  @Schema(description = "Current page number (0-based)")
  private int page;

  @Schema(description = "Number of items per page")
  private int size;

  @Schema(description = "Total number of elements in the dataset")
  private long totalElements;
}
