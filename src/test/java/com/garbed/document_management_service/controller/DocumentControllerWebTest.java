package com.garbed.document_management_service.controller;

import static org.mockito.ArgumentMatchers.*;

import com.garbed.document_management_service.service.DocumentService;
import com.garbed.document_management_service.util.CustomPage;
import com.garbed.document_management_service.vo.DocumentResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = DocumentController.class)
class DocumentControllerWebTest {

  @Autowired private WebTestClient webTestClient;

  @MockBean private DocumentService documentService;

  private DocumentResponse mockDoc;

  @BeforeEach
  void setup() {
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
  void getAllDocuments() {
    Mockito.when(documentService.getAllDocuments()).thenReturn(Flux.just(mockDoc));

    webTestClient
        .get()
        .uri("/documents")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(DocumentResponse.class)
        .hasSize(1)
        .contains(mockDoc);
  }

  @Test
  void getDownloadUrl() {
    String dummyUrl = "http://localhost/download/cv.pdf";

    Mockito.when(documentService.generateDownloadUrl(any(UUID.class)))
        .thenReturn(Mono.just(dummyUrl));

    webTestClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/documents/{id}/download-url").build(mockDoc.getId()))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo(dummyUrl);
  }

  @Test
  void searchDocuments() {
    CustomPage<DocumentResponse> page = new CustomPage<>(List.of(mockDoc), 0, 1, 1);

    Mockito.when(
            documentService.searchDocuments(
                anyString(), anyString(), anyList(), anyInt(), anyInt()))
        .thenReturn(Mono.just(page));

    webTestClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/documents/search")
                    .queryParam("user", "erick")
                    .queryParam("documentName", "cv.pdf")
                    .queryParam("tags", "java")
                    .queryParam("page", "0")
                    .queryParam("size", "10")
                    .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(CustomPage.class);
  }
}
