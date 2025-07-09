package com.garbed.document_management_service.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * DTO used for incoming document data when uploading a document.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/25/25
 */
@Data
@Builder
public class DocumentResponse {

  private UUID id;

  private String user;

  private String documentName;

  private List<String> tags;

  private String minioPath;

  private long fileSize;

  private String fileType;

  private LocalDateTime createdAt;
}
