package com.garbed.document_management_service.repository;

import com.garbed.document_management_service.entity.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

/**
 * DocumentRepository interface used to define DocumentRepository service contracts.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/25/25
 */
public interface DocumentRepository extends ReactiveCrudRepository<Document, UUID> {}
