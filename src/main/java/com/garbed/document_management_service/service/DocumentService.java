package com.garbed.document_management_service.service;

import com.garbed.document_management_service.exception.ResourceNotFoundException;
import com.garbed.document_management_service.util.CustomPage;
import com.garbed.document_management_service.vo.DocumentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * DocumentService interface used to define DocumentService service contracts.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/25/25
 */
public interface DocumentService {
  /**
   * Uploads a PDF file and its metadata to the system. The file is stored in MinIO and the metadata
   * is persisted in the database.
   *
   * @param metadataJson String containing the document metadata
   * @param filePart the uploaded file
   * @return Mono with the saved DocumentVo
   */
  public Mono<DocumentResponse> uploadDocument(String metadataJson, FilePart filePart);

  /**
   * Retrieves all documents from the database, ordered by creation date descending.
   *
   * @return Flux of DocumentVo containing all stored documents
   */
  Flux<DocumentResponse> getAllDocuments();

  /**
   * Generates a temporary download URL for a document stored in MinIO using its ID.
   *
   * @param documentId UUID of the document
   * @return Mono containing the download URL
   * @throws ResourceNotFoundException if the document is not found
   */
  Mono<String> generateDownloadUrl(UUID documentId);

  /**
   * Searches documents by optional filters (user, name, tags) and returns paginated results.
   *
   * @param user filter by user
   * @param documentName filter by document name
   * @param tags filter by tags
   * @param page the page number to retrieve
   * @param size the number of items per page
   * @return Mono containing a paginated list of matching documents
   */
  Mono<CustomPage<DocumentResponse>> searchDocuments(
      String user, String documentName, List<String> tags, int page, int size);
}
