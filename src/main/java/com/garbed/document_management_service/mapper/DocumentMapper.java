package com.garbed.document_management_service.mapper;

import com.garbed.document_management_service.entity.Document;
import com.garbed.document_management_service.vo.DocumentRequest;
import com.garbed.document_management_service.vo.DocumentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper interface for converting between Document entities and DTOs.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/25/25
 */
@Mapper(componentModel = "spring")
public interface DocumentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "minioPath", ignore = true)
  @Mapping(target = "fileSize", ignore = true)
  @Mapping(target = "fileType", ignore = true)
  @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
  Document toEntity(DocumentRequest dto);

  DocumentResponse toDto(Document entity);
}
