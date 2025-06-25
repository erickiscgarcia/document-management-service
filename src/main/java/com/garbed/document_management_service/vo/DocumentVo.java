package com.garbed.document_management_service.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * DocumentVo class used for requests/response regarding DocumentResponseDto in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/25/25
 */
@Data
@Builder
public class DocumentVo {
  private UUID id;

  @NotBlank private String user;

  @NotBlank private String documentName;

  @NotNull private List<String> tags;

  private String minioPath;
  private long fileSize;
  private String fileType;
  private LocalDateTime createdAt;
}
