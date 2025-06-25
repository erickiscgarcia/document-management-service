package com.garbed.document_management_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Document class used for modeling Document information in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/25/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("documents")
public class Document {

  @Id private UUID id;

  @Column("user_id")
  private String user;

  @Column("document_name")
  private String documentName;

  @Column("tags")
  private String tags;

  @Column("minio_path")
  private String minioPath;

  @Column("file_size")
  private long fileSize;

  @Column("file_type")
  private String fileType;

  @Column("created_at")
  private LocalDateTime createdAt;
}
