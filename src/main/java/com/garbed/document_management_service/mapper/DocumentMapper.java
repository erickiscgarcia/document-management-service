package com.garbed.document_management_service.mapper;

import com.garbed.document_management_service.entity.Document;
import com.garbed.document_management_service.vo.DocumentVo;
import java.util.Arrays;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * DocumentMapper class used for requests regarding DocumentMapper in the system.
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
  @Mapping(target = "tags", expression = "java(mapper.tagsToString(dto.getTags()))")
  Document toEntity(DocumentVo dto);

  default DocumentVo toDto(Document doc) {
    return DocumentVo.builder()
        .id(doc.getId())
        .user(doc.getUser())
        .documentName(doc.getDocumentName())
        .tags(doc.getTags() != null ? Arrays.asList(doc.getTags().split(",")) : List.of())
        .minioPath(doc.getMinioPath())
        .fileSize(doc.getFileSize())
        .fileType(doc.getFileType())
        .createdAt(doc.getCreatedAt())
        .build();
  }

  default String tagsToString(List<String> tags) {
    return tags != null ? String.join(",", tags) : "";
  }
}
