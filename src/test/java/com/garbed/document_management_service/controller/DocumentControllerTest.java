package com.garbed.document_management_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.garbed.document_management_service.service.DocumentService;
import com.garbed.document_management_service.util.CustomPage;
import com.garbed.document_management_service.util.PageRequest;
import com.garbed.document_management_service.vo.DocumentResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

  @Mock private DocumentService documentService;

  @InjectMocks private DocumentController documentController;

  private DocumentResponse mockDoc;

  @BeforeEach
  public void setUp() {
    mockDoc =
        DocumentResponse.builder()
            .id(UUID.randomUUID())
            .user("erick")
            .documentName("cv.pdf")
            .tags(List.of("java", "developer"))
            .minioPath("erick/cv.pdf")
            .fileSize(12345L)
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  void uploadDocument() {
    String metadataStringRequest =
        """
          {
            "user": "erick",
            "documentName": "cv.pdf",
            "tags": ["java","developer"]
          }
      """;
    FilePart mockFilePart = Mockito.mock(FilePart.class);

    DocumentResponse mockResponse =
        DocumentResponse.builder()
            .id(UUID.randomUUID())
            .user("erick")
            .documentName("cv.pdf")
            .tags(List.of("java", "developer"))
            .minioPath("erick/cv.pdf")
            .fileSize(12345L)
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();

    Mockito.when(documentService.uploadDocument(Mockito.anyString(), Mockito.any(FilePart.class)))
        .thenReturn(Mono.just(mockResponse));

    Mono<DocumentResponse> result = documentController.upload(metadataStringRequest, mockFilePart);

    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(mockResponse.getId(), response.getId());
              assertEquals(mockResponse.getUser(), response.getUser());
              assertEquals(mockResponse.getDocumentName(), response.getDocumentName());
            })
        .verifyComplete();
  }

  @Test
  void getAllDocuments_ReturnsFlux() {
    Mockito.when(documentService.getAllDocuments()).thenReturn(Flux.just(mockDoc));

    Flux<DocumentResponse> result = documentController.getAllDocuments();

    StepVerifier.create(result).expectNext(mockDoc).verifyComplete();
  }

  @Test
  void getDownloadUrl_ReturnsMono() {
    String dummyUrl = "http://localhost/download/cv.pdf";
    UUID id = mockDoc.getId();

    Mockito.when(documentService.generateDownloadUrl(id)).thenReturn(Mono.just(dummyUrl));

    Mono<String> result = documentController.getDownloadUrl(id);

    StepVerifier.create(result).expectNext(dummyUrl).verifyComplete();
  }

  @Test
  void searchDocuments_ReturnsMono() {
    CustomPage<DocumentResponse> page = new CustomPage<>(List.of(mockDoc), 0, 1, 1);

    Mockito.when(documentService.searchDocuments("erick", "cv.pdf", List.of("java"), 0, 1))
        .thenReturn(Mono.just(page));

    Mono<CustomPage<DocumentResponse>> result =
        documentController.searchDocuments(
            "erick", "cv.pdf", List.of("java"), new PageRequest(0, 1));

    StepVerifier.create(result)
        .expectNextMatches(p -> p.getContent().contains(mockDoc))
        .verifyComplete();
  }
}
