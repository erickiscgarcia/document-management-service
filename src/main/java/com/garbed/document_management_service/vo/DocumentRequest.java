package com.garbed.document_management_service.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * DTO used for incoming document data when uploading a document.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 7/4/25
 */
@Data
@Builder
public class DocumentRequest {

  @NotBlank(message = "User is required")
  private String user;

  @NotBlank(message = "Document name is required")
  private String documentName;

  @NotNull(message = "Tags cannot be null")
  private List<String> tags;
}
