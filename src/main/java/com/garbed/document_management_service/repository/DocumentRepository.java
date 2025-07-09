package com.garbed.document_management_service.repository;

import com.garbed.document_management_service.entity.Document;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * DocumentRepository interface used to define DocumentRepository service contracts.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/25/25
 */
public interface DocumentRepository extends ReactiveCrudRepository<Document, UUID> {

  /**
   * Searches documents applying optional filters for user, document name, and tags. Results are
   * ordered by creation date descending and paginated using limit and offset.
   *
   * @param user filter by user ID (ILIKE for partial match)
   * @param documentName filter by document name (ILIKE for partial match)
   * @param tags filter by tags (checks for any tag overlap)
   * @param limit maximum number of records to return
   * @param offset starting position of the page
   * @return Flux of matching Document entities
   */
  @Query(
      """
    SELECT * FROM documents
    WHERE (:user IS NULL OR user_id ILIKE :user)
      AND (:documentName IS NULL OR document_name ILIKE :documentName)
      AND (:tags IS NULL OR tags && cast(:tags as text[]))
    ORDER BY created_at DESC
    LIMIT :limit OFFSET :offset
    """)
  Flux<Document> searchDocuments(
      String user, String documentName, String tags, int limit, int offset);

  /**
   * Counts the total number of documents matching optional filters for user, document name, and
   * tags.
   *
   * @param user filter by user ID (ILIKE for partial match)
   * @param documentName filter by document name (ILIKE for partial match)
   * @param tags filter by tags (checks for any tag overlap)
   * @return Mono containing the total count of matching documents
   */
  @Query(
      """
        SELECT COUNT(*) FROM documents
        WHERE (:user IS NULL OR user_id ILIKE :user)
          AND (:documentName IS NULL OR document_name ILIKE :documentName)
          AND (:tags IS NULL OR tags && cast(:tags as text[]))
        """)
  Mono<Long> countDocuments(String user, String documentName, String tags);

  Mono<Document> findByUserAndDocumentName(String user, String documentName);
}
