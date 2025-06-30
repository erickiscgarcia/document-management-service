package com.garbed.document_management_service.service;

import com.garbed.document_management_service.configuration.MinioConfig;
import com.garbed.document_management_service.entity.Document;
import com.garbed.document_management_service.mapper.DocumentMapper;
import com.garbed.document_management_service.repository.DocumentRepository;
import com.garbed.document_management_service.vo.DocumentVo;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

  private final DocumentRepository repository;
  private final DocumentMapper mapper;
  private final MinioClient minioClient;
  private final MinioConfig minioConfig;

  @Override
  public Mono<DocumentVo> uploadDocument(Mono<DocumentVo> metadataMono, FilePart filePart) {
    return metadataMono.flatMap(metadata -> processUpload(metadata, filePart));
  }

  @Override
  public Flux<DocumentVo> getAllDocuments() {
    return repository.findAll().map(mapper::toDto);
  }

  private Mono<DocumentVo> processUpload(DocumentVo metadata, FilePart filePart) {
    String userFolder = metadata.getUser();
    String filename = metadata.getDocumentName();
    String path = userFolder + "/" + filename;

    return Mono.fromCallable(() -> java.io.File.createTempFile("upload-", ".pdf"))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(
            tempFile ->
                filePart
                    .transferTo(tempFile)
                    .then(
                        Mono.fromCallable(
                                () -> saveToMinioAndBuildEntity(metadata, filePart, tempFile, path))
                            .subscribeOn(Schedulers.boundedElastic())))
        .flatMap(repository::save)
        .map(mapper::toDto);
  }

  private Document saveToMinioAndBuildEntity(
      DocumentVo metadata, FilePart filePart, java.io.File tempFile, String path) {

    try (InputStream is = new FileInputStream(tempFile)) {
      long fileSize = tempFile.length();
      String contentType =
          filePart.headers().getContentType() != null
              ? Objects.requireNonNull(filePart.headers().getContentType()).toString()
              : "application/pdf";

      minioClient.putObject(
          PutObjectArgs.builder().bucket(minioConfig.getBucket()).object(path).stream(
                  is, fileSize, -1)
              .contentType(contentType)
              .build());

      if(!tempFile.delete()){
        log.warn("Failed to delete tmp file: {}", tempFile.getName());
      }

      return Document.builder()
          .user(metadata.getUser())
          .documentName(metadata.getDocumentName())
          .tags(metadata.getTags())
          .minioPath(path)
          .fileSize(fileSize)
          .fileType(contentType)
          .createdAt(LocalDateTime.now())
          .build();
    } catch (IOException | MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to upload file", e);
    }
  }
}
