package com.garbed.document_management_service.service;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbed.document_management_service.configuration.MinioConfig;
import com.garbed.document_management_service.entity.Document;
import com.garbed.document_management_service.mapper.DocumentMapper;
import com.garbed.document_management_service.repository.DocumentRepository;
import com.garbed.document_management_service.util.CustomPage;
import com.garbed.document_management_service.vo.DocumentRequest;
import com.garbed.document_management_service.vo.DocumentResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class DocumentServiceImplTest {

  @Mock private DocumentRepository repository;
  @Mock private DocumentMapper mapper;
  @Mock private MinioClient minioClient;
  @Mock private MinioConfig minioConfig;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private DocumentServiceImpl service;

  private Document document;
  private DocumentResponse documentResponse;
  private DocumentRequest documentRequest;

  @BeforeEach
  void setUp() {
    UUID docId = UUID.randomUUID();
    document =
        Document.builder()
            .id(docId)
            .user("erick")
            .documentName("cv.pdf")
            .tags(List.of("java"))
            .minioPath("erick/cv.pdf")
            .fileSize(12345L)
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();

    documentRequest =
        DocumentRequest.builder()
            .user("erick")
            .documentName("cv.pdf")
            .tags(List.of("java"))
            .build();

    documentResponse =
        DocumentResponse.builder()
            .id(docId)
            .user("erick")
            .documentName("cv.pdf")
            .tags(List.of("java"))
            .fileSize(12345L)
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  void getAllDocuments() {
    when(repository.findAll()).thenReturn(Flux.just(document));
    when(mapper.toDto(document)).thenReturn(documentResponse);

    StepVerifier.create(service.getAllDocuments()).expectNext(documentResponse).verifyComplete();
  }

  @Test
  void generateDownloadUrl() throws Exception {
    String expectedUrl = "http://localhost:9000/documents-bucket/erick/cv.pdf";
    UUID id = document.getId();

    when(repository.findById(id)).thenReturn(Mono.just(document));
    when(minioConfig.getBucket()).thenReturn("documents-bucket");
    when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
        .thenReturn(expectedUrl);

    StepVerifier.create(service.generateDownloadUrl(id)).expectNext(expectedUrl).verifyComplete();
  }

  @Test
  void uploadDocumentUpdateExistingDocument() throws Exception {
    String metadataStringRequest =
        """
      {
        "user": "erick",
        "documentName": "cv.pdf",
        "tags": ["java"]
      }
  """;

    FilePart filePart = mock(FilePart.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    when(filePart.headers()).thenReturn(headers);

    File tempFile = File.createTempFile("upload-", ".pdf");
    when(filePart.transferTo(any(File.class))).thenReturn(Mono.empty());

    Document existingDoc =
        Document.builder()
            .id(UUID.randomUUID())
            .user("erick")
            .documentName("cv.pdf")
            .tags(List.of("java"))
            .minioPath("erick/cv.pdf")
            .fileSize(1000L)
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();

    when(minioConfig.getBucket()).thenReturn("documents-bucket");
    when(repository.findByUserAndDocumentName("erick", "cv.pdf"))
        .thenReturn(Mono.just(existingDoc));
    when(objectMapper.readValue(any(String.class), eq(DocumentRequest.class)))
        .thenReturn(documentRequest);

    Document updatedDoc =
        existingDoc.toBuilder().fileSize(tempFile.length()).createdAt(LocalDateTime.now()).build();

    when(repository.save(any(Document.class))).thenReturn(Mono.just(updatedDoc));
    when(mapper.toDto(any(Document.class))).thenReturn(documentResponse);

    Mono<DocumentResponse> result = service.uploadDocument(metadataStringRequest, filePart);

    StepVerifier.create(result).expectNext(documentResponse).verifyComplete();
  }

  @Test
  void uploadDocumentCreateNewDocumentWhenNotExists() throws Exception {
    String metadataStringRequest =
        """
      {
        "user": "erick",
        "documentName": "nuevo.pdf",
        "tags": ["dev"]
      }
  """;

    DocumentRequest newRequest =
        DocumentRequest.builder()
            .user("erick")
            .documentName("nuevo.pdf")
            .tags(List.of("dev"))
            .build();

    DocumentResponse newResponse =
        DocumentResponse.builder()
            .id(UUID.randomUUID())
            .user("erick")
            .documentName("nuevo.pdf")
            .tags(List.of("dev"))
            .fileSize(12345L)
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();

    FilePart filePart = mock(FilePart.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    when(filePart.headers()).thenReturn(headers);

    File tempFile = File.createTempFile("upload-", ".pdf");
    when(filePart.transferTo(any(File.class))).thenReturn(Mono.empty());

    when(minioConfig.getBucket()).thenReturn("documents-bucket");
    when(repository.findByUserAndDocumentName("erick", "nuevo.pdf")).thenReturn(Mono.empty());
    when(objectMapper.readValue(any(String.class), eq(DocumentRequest.class)))
        .thenReturn(newRequest);

    Document newDoc =
        Document.builder()
            .id(UUID.randomUUID())
            .user("erick")
            .documentName("nuevo.pdf")
            .tags(List.of("dev"))
            .minioPath("erick/nuevo.pdf")
            .fileSize(tempFile.length())
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();

    when(repository.save(any(Document.class))).thenReturn(Mono.just(newDoc));
    when(mapper.toDto(any(Document.class))).thenReturn(newResponse);

    Mono<DocumentResponse> result = service.uploadDocument(metadataStringRequest, filePart);

    StepVerifier.create(result).expectNext(newResponse).verifyComplete();
  }

  @Test
  void searchDocuments() {
    String user = "erick";
    String documentName = "cv";
    List<String> tags = List.of("java");
    int page = 0;
    int size = 10;

    when(repository.searchDocuments(any(), any(), any(), eq(size), eq(0)))
        .thenReturn(Flux.just(document));
    when(mapper.toDto(document)).thenReturn(documentResponse);
    when(repository.countDocuments(any(), any(), any())).thenReturn(Mono.just(1L));

    Mono<CustomPage<DocumentResponse>> result =
        service.searchDocuments(user, documentName, tags, page, size);

    StepVerifier.create(result)
        .assertNext(
            pageResult -> {
              assert pageResult.getTotalElements() == 1;
              assert pageResult.getContent().size() == 1;
              assert pageResult.getContent().get(0).getUser().equals("erick");
            })
        .verifyComplete();
  }
}
