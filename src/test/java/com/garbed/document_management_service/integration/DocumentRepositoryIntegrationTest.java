package com.garbed.document_management_service.integration;

import com.garbed.document_management_service.entity.Document;
import com.garbed.document_management_service.repository.DocumentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

/**
 * DocumentRepositoryIntegrationTest class used for requests regarding
 * DocumentRepositoryIntegrationTest in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 7/7/25
 */
@ActiveProfiles("test")
@Testcontainers
@DataR2dbcTest
public class DocumentRepositoryIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @Autowired private DocumentRepository documentRepository;
  private Document testDoc;

  @DynamicPropertySource
  static void setDatasourceProps(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.r2dbc.url",
        () ->
            "r2dbc:postgresql://"
                + postgres.getHost()
                + ":"
                + postgres.getFirstMappedPort()
                + "/testdb");
    registry.add("spring.r2dbc.username", postgres::getUsername);
    registry.add("spring.r2dbc.password", postgres::getPassword);
  }

  @BeforeEach
  void setup() {
    testDoc =
        Document.builder()
            .user("erick")
            .documentName("cv.pdf")
            .tags(List.of("java", "reactive"))
            .minioPath("erick/cv.pdf")
            .fileSize(123456L)
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  void shouldSaveAndFindDocument() {
    StepVerifier.create(
            documentRepository
                .save(testDoc)
                .flatMap(saved -> documentRepository.findById(saved.getId())))
        .expectNextMatches(
            found -> found.getUser().equals("erick") && found.getDocumentName().equals("cv.pdf"))
        .verifyComplete();
  }

  @Test
  void searchDocuments_shouldReturnFilteredResults() {
    Document doc1 =
        Document.builder()
            .user("user1")
            .documentName("invoice_2024")
            .tags(List.of("finance", "july"))
            .minioPath("path1")
            .fileSize(1000L)
            .fileType("pdf")
            .build();

    Document doc2 =
        Document.builder()
            .user("user1")
            .documentName("report_q2")
            .tags(List.of("finance", "report"))
            .minioPath("path2")
            .fileSize(1200L)
            .fileType("docx")
            .build();

    StepVerifier.create(documentRepository.saveAll(List.of(doc1, doc2)).then()).verifyComplete();

    StepVerifier.create(
            documentRepository.searchDocuments("user1", "%invoice%", "{finance}", 10, 0))
        .assertNext(
            result -> {
              Assertions.assertEquals("invoice_2024", result.getDocumentName());
              Assertions.assertEquals("user1", result.getUser());
            })
        .verifyComplete();
  }

  @Test
  void countDocuments_shouldReturnCorrectCount() {
    Document doc1 =
        Document.builder()
            .user("user-e")
            .documentName("invoice_2024")
            .tags(List.of("finance", "july"))
            .minioPath("path1")
            .fileSize(1000L)
            .fileType("pdf")
            .build();

    Document doc2 =
        Document.builder()
            .user("user-e")
            .documentName("report_q2")
            .tags(List.of("finance", "report"))
            .minioPath("path2")
            .fileSize(1200L)
            .fileType("docx")
            .build();

    StepVerifier.create(documentRepository.saveAll(List.of(doc1, doc2)).then()).verifyComplete();
    StepVerifier.create(documentRepository.countDocuments("user-e", "%invoice%", "{\"finance\"}"))
        .expectNext(1L)
        .verifyComplete();
  }

  @Test
  void findByUserAndDocumentName_shouldReturnCorrectDocument() {
    Document doc1 =
        Document.builder()
            .user("user1-r")
            .documentName("invoice_2024")
            .tags(List.of("finance", "july"))
            .minioPath("path1")
            .fileSize(1000L)
            .fileType("pdf")
            .build();

    Document doc2 =
        Document.builder()
            .user("user-r")
            .documentName("report_q2")
            .tags(List.of("finance", "report"))
            .minioPath("path2")
            .fileSize(1200L)
            .fileType("docx")
            .build();

    StepVerifier.create(documentRepository.saveAll(List.of(doc1, doc2)).then()).verifyComplete();
    StepVerifier.create(documentRepository.findByUserAndDocumentName("user-r", "report_q2"))
        .assertNext(doc -> Assertions.assertEquals("docx", doc.getFileType()))
        .verifyComplete();
  }
}
