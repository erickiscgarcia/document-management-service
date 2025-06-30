package com.garbed.document_management_service.service;

import com.garbed.document_management_service.vo.DocumentVo;
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
  Mono<DocumentVo> uploadDocument(Mono<DocumentVo> metadataMono, FilePart filePart);

  Flux<DocumentVo> getAllDocuments();
}
