package com.garbed.document_management_service.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.garbed.document_management_service.entity.Document;
import com.garbed.document_management_service.repository.DocumentRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * DocumentServiceIntegrationTest class used for requests regarding DocumentServiceIntegrationTest
 * in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 7/7/25
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class DocumentServiceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @Container
  static GenericContainer<?> minio =
      new GenericContainer<>("minio/minio:latest")
          .withEnv("MINIO_ACCESS_KEY", "minio")
          .withEnv("MINIO_SECRET_KEY", "minio123")
          .withExposedPorts(9000)
          .withCommand("server", "/data");

  @Autowired private WebTestClient webTestClient;
  @Autowired private DocumentRepository repository;

  @BeforeAll
  static void setupMinioBucket() throws Exception {

    MinioClient client =
        MinioClient.builder()
            .endpoint(minio.getHost(), minio.getFirstMappedPort(), false)
            .credentials("minio", "minio123")
            .build();

    boolean bucketExists =
        client.bucketExists(BucketExistsArgs.builder().bucket("documents-bucket").build());
    if (!bucketExists) {
      client.makeBucket(MakeBucketArgs.builder().bucket("documents-bucket").build());
    }
  }

  @DynamicPropertySource
  static void dynamicProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.r2dbc.url",
        () ->
            String.format(
                "r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
    registry.add("spring.r2dbc.username", postgres::getUsername);
    registry.add("spring.r2dbc.password", postgres::getPassword);

    String minioEndpoint = "http://" + minio.getHost() + ":" + minio.getFirstMappedPort();
    registry.add("MINIO_ENDPOINT", () -> minioEndpoint);
    registry.add("MINIO_ACCESS_KEY", () -> "minio");
    registry.add("MINIO_SECRET_KEY", () -> "minio123");
  }

  @Test
  void fullUploadFlow() {
    String metadataJson =
        "{\"user\":\"tester\",\"documentName\":\"testFile.pdf\",\"tags\":[\"tag1\",\"tag2\"]}";

    byte[] fileBytes = "contenido PDF simulado".getBytes(StandardCharsets.UTF_8);

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("metadata", metadataJson);
    builder
        .part(
            "file",
            new ByteArrayResource(fileBytes) {
              @Override
              public String getFilename() {
                return "testFile.pdf";
              }
            })
        .header("Content-Type", MediaType.APPLICATION_PDF_VALUE);

    webTestClient
        .post()
        .uri("/documents/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(builder.build()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.user")
        .isEqualTo("tester")
        .jsonPath("$.documentName")
        .isEqualTo("testFile.pdf");

    // Validate in DB
    List<Document> docs = repository.findAll().collectList().block();
    assertNotNull(docs);
    Assertions.assertFalse(docs.isEmpty());
    Assertions.assertEquals("tester", docs.get(0).getUser());

    // URL download validation
    UUID docId = docs.get(0).getId();
    webTestClient
        .get()
        .uri("/documents/" + docId + "/download-url")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            res -> {
              String url = res.getResponseBody();
              assertNotNull(url);
              Assertions.assertTrue(url.startsWith("http"));
            });
  }

  /** Negative cases */
  @Test
  void uploadShouldFailFileTooLarge() {
    String metadataJson = "{\"user\":\"tester\",\"documentName\":\"large.pdf\",\"tags\":[\"tag\"]}";

    byte[] tooLargeFile = new byte[(int) (1024 * 1024 + 1)];

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("metadata", metadataJson);
    builder
        .part(
            "file",
            new ByteArrayResource(tooLargeFile) {
              @Override
              public String getFilename() {
                return "large.pdf";
              }
            })
        .header("Content-Type", MediaType.APPLICATION_PDF_VALUE);

    webTestClient
        .post()
        .uri("/documents/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(builder.build()))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(msg -> Assertions.assertTrue(((String) msg).toLowerCase().contains("file size")));
  }

  @Test
  void uploadShouldFailMetadataInvalidJson() {
    String invalidJson = "{\"user\":\"tester\",\"documentName\":\"file.pdf\"";

    byte[] fileBytes = "contenido".getBytes(StandardCharsets.UTF_8);

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("metadata", invalidJson);
    builder
        .part(
            "file",
            new ByteArrayResource(fileBytes) {
              @Override
              public String getFilename() {
                return "file.pdf";
              }
            })
        .header("Content-Type", MediaType.APPLICATION_PDF_VALUE);

    webTestClient
        .post()
        .uri("/documents/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(builder.build()))
        .exchange()
        .expectStatus()
        .isEqualTo(400)
        .expectBody()
        .jsonPath("$.message")
        .value(
            msg ->
                Assertions.assertTrue(((String) msg).toLowerCase().contains("invalid metadata")));
  }

  @Test
  void uploadShouldFailFileIsEmpty() {
    String metadataJson = "{\"user\":\"tester\",\"documentName\":\"empty.pdf\",\"tags\":[\"tag\"]}";

    byte[] emptyFile = new byte[0];

    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("metadata", metadataJson);
    builder
        .part(
            "file",
            new ByteArrayResource(emptyFile) {
              @Override
              public String getFilename() {
                return "empty.pdf";
              }
            })
        .header("Content-Type", MediaType.APPLICATION_PDF_VALUE);

    webTestClient
        .post()
        .uri("/documents/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(builder.build()))
        .exchange()
        .expectStatus()
        .isEqualTo(400)
        .expectBody()
        .jsonPath("$.message")
        .value(msg -> Assertions.assertTrue(((String) msg).toLowerCase().contains("file size")));
  }

  @Test
  void uploadShouldFailMissingMetadataOrFile() {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    byte[] fileBytes = "contenido".getBytes(StandardCharsets.UTF_8);
    builder
        .part(
            "file",
            new ByteArrayResource(fileBytes) {
              @Override
              public String getFilename() {
                return "file.pdf";
              }
            })
        .header("Content-Type", MediaType.APPLICATION_PDF_VALUE);

    webTestClient
        .post()
        .uri("/documents/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(builder.build()))
        .exchange()
        .expectStatus()
        .isEqualTo(400);

    MultipartBodyBuilder builder2 = new MultipartBodyBuilder();
    builder2.part("metadata", "{\"user\":\"tester\",\"documentName\":\"file.pdf\"}");

    webTestClient
        .post()
        .uri("/documents/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(builder2.build()))
        .exchange()
        .expectStatus()
        .isEqualTo(400);
  }

  @Test
  void getDownloadUrlShouldFailDocumentNotFound() {
    UUID fakeId = UUID.randomUUID();

    webTestClient
        .get()
        .uri("/documents/" + fakeId + "/download-url")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .value(msg -> Assertions.assertTrue(((String) msg).toLowerCase().contains("not found")));
  }
}
