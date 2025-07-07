package com.garbed.document_management_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbed.document_management_service.configuration.MinioConfig;
import com.garbed.document_management_service.entity.Document;
import com.garbed.document_management_service.exception.*;
import com.garbed.document_management_service.mapper.DocumentMapper;
import com.garbed.document_management_service.repository.DocumentRepository;
import com.garbed.document_management_service.util.CustomPage;
import com.garbed.document_management_service.util.SystemConstants;
import com.garbed.document_management_service.vo.DocumentRequest;
import com.garbed.document_management_service.vo.DocumentResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final ObjectMapper objectMapper;

  @Override
  public Mono<DocumentResponse> uploadDocument(String metadataJson, FilePart filePart) {
    DocumentRequest metadata = parseMetadata(metadataJson);
    long contentLength = filePart.headers().getContentLength();

    if (contentLength > SystemConstants.Util.MAX_FILE_SIZE_BYTES || contentLength <= 0) {
      return Mono.error(
          new MaxFileSizeException(SystemConstants.Exception.MAX_FILE_SIZE_EXCEPTION));
    }

    return processUpload(metadata, filePart);
  }

  @Override
  public Flux<DocumentResponse> getAllDocuments() {
    return repository.findAll().map(mapper::toDto);
  }

  @Override
  public Mono<String> generateDownloadUrl(UUID documentId) {
    return repository
        .findById(documentId)
        .switchIfEmpty(
            Mono.error(new ResourceNotFoundException(SystemConstants.Exception.DOCUMENT_NOT_FOUND)))
        .flatMap(
            doc -> {
              String bucket = minioConfig.getBucket();
              String objectPath = doc.getMinioPath();

              return Mono.fromCallable(
                      () ->
                          minioClient.getPresignedObjectUrl(
                              io.minio.GetPresignedObjectUrlArgs.builder()
                                  .bucket(bucket)
                                  .object(objectPath)
                                  .method(io.minio.http.Method.GET)
                                  .expiry(60 * 10)
                                  .build()))
                  .subscribeOn(Schedulers.boundedElastic());
            });
  }

  @Override
  public Mono<CustomPage<DocumentResponse>> searchDocuments(
      String user, String documentName, List<String> tags, int page, int size) {
    int offset = page * size;

    String userFilter = normalizeForLike(user);
    String nameFilter = normalizeForLike(documentName);
    String tagsArray = convertTagsToPostgresArray(tags);

    Mono<List<DocumentResponse>> documents =
        fetchDocuments(userFilter, nameFilter, tagsArray, size, offset);
    Mono<Long> totalCount = countMatchingDocuments(userFilter, nameFilter, tagsArray);

    return documents
        .zipWith(totalCount)
        .map(tuple -> buildPage(tuple.getT1(), page, size, tuple.getT2()));
  }

  /**
   * Parses a JSON string into a {@link DocumentRequest} object.
   *
   * <p>If the input JSON is invalid or cannot be mapped to the expected structure, this method
   * throws an {@link InvalidMetadataException}.
   *
   * @param json the metadata JSON string
   * @return the parsed {@link DocumentRequest} object
   * @throws InvalidMetadataException if the JSON is malformed or parsing fails
   */
  private DocumentRequest parseMetadata(String json) {
    try {
      return objectMapper.readValue(json, DocumentRequest.class);
    } catch (JsonProcessingException e) {
      throw new InvalidMetadataException("Invalid metadata JSON format", e);
    }
  }

  /**
   * Adds wildcard characters to a string for SQL LIKE operations.
   *
   * @param value input string
   * @return formatted string with wildcards, or null if blank
   */
  private String normalizeForLike(String value) {
    return (value != null && !value.isBlank())
        ? SystemConstants.Util.PERCENTAGE_CHAR + value.trim() + SystemConstants.Util.PERCENTAGE_CHAR
        : null;
  }

  /**
   * Retrieves filtered documents from the repository and maps them to DTOs.
   *
   * @param user username filter
   * @param name document name filter
   * @param tags list of tags
   * @param size max number of results
   * @param offset number of items to skip
   * @return Mono containing the list of matching documents
   */
  private Mono<List<DocumentResponse>> fetchDocuments(
      String user, String name, String tags, int size, int offset) {
    return repository
        .searchDocuments(user, name, tags, size, offset)
        .map(mapper::toDto)
        .collectList();
  }

  /**
   * Counts the total number of documents matching the given filters.
   *
   * @param user username filter
   * @param name document name filter
   * @param tags list of tags
   * @return Mono with the total count
   */
  private Mono<Long> countMatchingDocuments(String user, String name, String tags) {
    return repository.countDocuments(user, name, tags);
  }

  /**
   * Builds a CustomPage object using the result list and pagination parameters.
   *
   * @param content list of DocumentVo
   * @param page current page number
   * @param size number of items per page
   * @param total total number of matching items
   * @return CustomPage of DocumentVo
   */
  private CustomPage<DocumentResponse> buildPage(
      List<DocumentResponse> content, int page, int size, long total) {
    return new CustomPage<>(content, page, size, total);
  }

  /**
   * Handles the complete process of uploading a file: - Creates a temporary file. - Transfers
   * content from FilePart to the temp file. - Uploads the file to MinIO. - Saves metadata in the
   * database.
   *
   * @param metadata document metadata
   * @param filePart file uploaded by the user
   * @return Mono with the saved document
   */
  private Mono<DocumentResponse> processUpload(DocumentRequest metadata, FilePart filePart) {
    String user = metadata.getUser();
    String filename = metadata.getDocumentName();
    String path = user + SystemConstants.Util.SLASH_CHAR + filename;

    long contentLength = filePart.headers().getContentLength();

    if (contentLength > SystemConstants.Util.MAX_FILE_SIZE_BYTES || contentLength <= 0) {
      throw new MaxFileSizeException(SystemConstants.Exception.MAX_FILE_SIZE_EXCEPTION);
    }

    return repository
        .findByUserAndDocumentName(user, filename)
        .switchIfEmpty(Mono.defer(() -> Mono.just(new Document())))
        .flatMap(
            existingDoc -> {
              boolean isNew = existingDoc.getId() == null;

              return Mono.fromCallable(
                      () ->
                          File.createTempFile(
                              SystemConstants.File.UPLOAD_PREFIX,
                              SystemConstants.File.PDF_EXTENSION))
                  .subscribeOn(Schedulers.boundedElastic())
                  .flatMap(
                      tempFile ->
                          filePart
                              .transferTo(tempFile)
                              .then(
                                  Mono.fromCallable(
                                          () -> {
                                            Document doc =
                                                saveToMinioAndBuildEntity(
                                                    metadata, filePart, tempFile, path);
                                            if (!isNew) {
                                              doc.setId(existingDoc.getId());
                                            }
                                            return doc;
                                          })
                                      .subscribeOn(Schedulers.boundedElastic())));
            })
        .flatMap(repository::save)
        .map(mapper::toDto);
  }

  /**
   * Uploads the given file to MinIO and constructs the corresponding Document entity. It also
   * handles cleanup and error wrapping in custom exceptions.
   *
   * @param metadata document metadata
   * @param filePart uploaded file part
   * @param tempFile temporary file created for upload
   * @param path storage path in MinIO
   * @return Document entity ready to be saved
   * @throws FileUploadException if there’s an I/O problem
   * @throws MinioUploadException if MinIO upload fails
   */
  private Document saveToMinioAndBuildEntity(
      DocumentRequest metadata, FilePart filePart, File tempFile, String path) {

    long fileSize = tempFile.length();
    String contentType =
        filePart.headers().getContentType() != null
            ? Objects.requireNonNull(filePart.headers().getContentType()).toString()
            : SystemConstants.File.CONTENT_TYPE;

    try (InputStream is = new FileInputStream(tempFile)) {
      try {
        minioClient.putObject(
            PutObjectArgs.builder().bucket(minioConfig.getBucket()).object(path).stream(
                    is, fileSize, -1)
                .contentType(contentType)
                .build());
      } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
        throw new MinioUploadException(SystemConstants.Exception.MINIO_UPLOAD_EXCEPTION, e);
      }

    } catch (IOException e) {
      throw new FileUploadException(SystemConstants.Exception.FILE_UPLOAD_EXCEPTION, e);
    } finally {
      if (!tempFile.delete()) {
        log.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath());
      }
    }

    return this.buildDocument(metadata, path, fileSize, contentType);
  }

  /**
   * @param metadata
   * @param path
   * @param fileSize
   * @param contentType
   * @return
   */
  private Document buildDocument(
      DocumentRequest metadata, String path, long fileSize, String contentType) {
    return Document.builder()
        .user(metadata.getUser())
        .documentName(metadata.getDocumentName())
        .tags(metadata.getTags())
        .minioPath(path)
        .fileSize(fileSize)
        .fileType(contentType)
        .createdAt(LocalDateTime.now())
        .build();
  }

  /**
   * Converts a list of tags into a PostgreSQL-compatible text array string.
   *
   * <p>For example, a list {@code ["java", "dev"]} will be converted to {@code {"java","dev"}}.
   * Returns {@code null} if the list is {@code null} or empty.
   *
   * @param tags the list of tags to convert
   * @return a PostgreSQL-compatible text array string or {@code null} if the input is {@code null}
   *     or empty
   */
  private static String convertTagsToPostgresArray(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return null;
    }
    return tags.stream()
        .map(
            tag ->
                SystemConstants.Pg.ARRAY_QUOTE_START
                    + tag.replace(
                        SystemConstants.Pg.ARRAY_QUOTE_START, SystemConstants.Pg.ARRAY_QUOTE_END)
                    + SystemConstants.Pg.ARRAY_QUOTE_START)
        .collect(
            Collectors.joining(
                SystemConstants.Pg.ARRAY_SEPARATOR,
                SystemConstants.Pg.ARRAY_START,
                SystemConstants.Pg.ARRAY_END));
  }
}
