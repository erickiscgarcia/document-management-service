package com.garbed.document_management_service.controller;

import com.garbed.document_management_service.service.DocumentService;
import com.garbed.document_management_service.util.CustomPage;
import com.garbed.document_management_service.util.PageRequest;
import com.garbed.document_management_service.vo.DocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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

  @Operation(
      summary = "Upload a PDF with metadata and file",
      description = "Uploads a PDF document with metadata. The file must not exceed 500MB.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Document uploaded successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid input"),
    @ApiResponse(responseCode = "500", description = "Server error")
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<DocumentResponse> upload(
      @Parameter(
              name = "metadata",
              description = "JSON information of the file data",
              example =
                  """
              {"user":"erick","documentName":"otro.pdf","tags":["java","dev"]}
              """,
              required = true)
          @RequestPart("metadata")
          String metadata,
      @Parameter(
              name = "file",
              description = "PDF file to upload (max size 500MB)",
              required = true)
          @RequestPart("file")
          FilePart file) {
    return documentService.uploadDocument(metadata, file);
  }

  @Operation(
      summary = "Get all uploaded documents",
      description = "Returns a list of all documents ordered by creation date descending")
  @GetMapping
  public Flux<DocumentResponse> getAllDocuments() {
    return documentService.getAllDocuments();
  }

  @Operation(summary = "Generate a temporary download URL for a document by ID")
  @GetMapping("/{id}/download-url")
  public Mono<String> getDownloadUrl(
      @Parameter(name = "id", description = "PDF file identifier", required = true) @PathVariable
          UUID id) {
    return documentService.generateDownloadUrl(id);
  }

  @Operation(
      summary = "Search documents with optional filters and pagination",
      description =
          "Allows filtering documents by user, name, or tag. Results are paginated and ordered by creation date descending.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Search results returned successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid query parameters")
  })
  @GetMapping("/search")
  public Mono<CustomPage<DocumentResponse>> searchDocuments(
      @Parameter(description = "Filter by user", example = "erick") @RequestParam(required = false)
          String user,
      @Parameter(description = "Filter by document name", example = "cv.pdf")
          @RequestParam(required = false)
          String documentName,
      @Parameter(description = "Filter by tag", example = "java") @RequestParam(required = false)
          List<String> tags,
      @Parameter(description = "Page request") @Valid PageRequest pageRequest) {
    return documentService.searchDocuments(
        user, documentName, tags, pageRequest.getPage(), pageRequest.getSize());
  }
}
