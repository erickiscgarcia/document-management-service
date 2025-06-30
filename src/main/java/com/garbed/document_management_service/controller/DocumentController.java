package com.garbed.document_management_service.controller;

import com.garbed.document_management_service.service.DocumentService;
import com.garbed.document_management_service.vo.DocumentVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * DocumentController class used for requests regarding DocumentController in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/25/25
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

  private final DocumentService documentService;

  @Operation(summary = "Upload a PDF document with metadata")
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<DocumentVo> upload(
      @RequestPart("metadata") DocumentVo metadata, @RequestPart("file") FilePart file) {
    return documentService.uploadDocument(Mono.just(metadata), file);
  }

  @Operation(
      summary = "Get all uploaded documents",
      description = "Returns a list of all documents ordered by creation date descending")
  @GetMapping
  public Flux<DocumentVo> getAllDocuments() {
    return documentService.getAllDocuments();
  }
}
