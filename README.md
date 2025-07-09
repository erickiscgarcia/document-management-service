![Java](https://img.shields.io/badge/Java-17-blue)
![Spring WebFlux](https://img.shields.io/badge/Spring-WebFlux-green)
![Dockerized](https://img.shields.io/badge/Docker-Ready-blue)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

# Document Management Service

A reactive, containerized microservice for uploading, storing, and managing PDF documents using Spring WebFlux, PostgreSQL (R2DBC), MinIO (S3-compatible), and Docker.

---

## Contents

- [Description](#description)
- [Requirements](#requirements)
- [Configuration](#configuration)
- [Environment Variables](#environment-variables)
- [Running with Docker](#running-with-docker)
- [Main Endpoints](#main-endpoints)
- [Unit and Integration Tests](#unit-and-integration-tests)
- [Coverage with Jacoco](#coverage-with-jacoco)
- [Code Formatting with Spotless](#code-formatting-with-spotless)
- [Notes](#notes)

---

## 🚀 Features

- ✅ Reactive stack with Spring WebFlux
- 🐳 Fully dockerized infrastructure (PostgreSQL, MinIO, backend)
- 📄 PDF document upload, validation, and storage
- 🎯 Pre-signed download URL generation
- 🔐 Environment variable externalization via .env (no hardcoded credentials)
- 🧪 Integration testing with Docker containers (PostgreSQL, MinIO)
- 🧼 Code formatting with Spotless
- 📖 Swagger/OpenAPI documentation
- 🔧 Multiple configuration profiles: local, test, docker

## Description

This microservice is designed to support applications that require secure and efficient handling of PDF documents — ideal for digital contracts, signed forms, or compliance storage systems.

---

## 📂 Project Structure

```plaintext
document-management-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── garbed/
│   │   │           └── document_management_service/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-docker.yml
│   │       ├── application-test.yml
│   │       └── db/
│   │           └── init/
│   │               └── schema.sql
│   └── test/
│       └── java/
│           └── com/
│               └── garbed/
│                   └── document_management_service/
├── .env
├── Dockerfile
├── docker-compose.yml
├── mvnw / mvnw.cmd
├── pom.xml
└── README.md
```

## Requirements

- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- MinIO (container or local install)
- PostgreSQL (container or local install)

---

## Configuration

The project uses externalized configuration with environment variables and YAML files to separate profiles: `local`, `docker`, and `test`.

### Main Configuration Files

- `application.yml`: Local development configuration with default credentials and environment variables.
- `application-docker.yml`: Docker configuration using container service names.
- `application-test.yml`: Test profile for unit and integration testing with a test database.

---

## Environment Variables

```env
# Spring Profile
SPRING_PROFILES_ACTIVE=docker

# PostgreSQL
DB_NAME=documents
DB_USER=user
DB_PASSWORD=password

# MinIO
MINIO_ACCESS_KEY=minio
MINIO_SECRET_KEY=minio123
MINIO_BUCKET=documents-bucket
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
```

## Running with Docker
Start all services with:

```bash
docker-compose --env-file .env up --build
```

This will launch:

- PostgreSQL on port 5432
- MinIO on ports 9000 (API) and 9001 (web console)
- Document Management Service on port 8080

Access MinIO Console at: http://localhost:9001

Swagger API Docs: http://localhost:8080/webjars/swagger-ui/index.html

Use credentials from the .env file.

##  MinIO URLs for Download Links

- In local development, the app uses http://localhost:9000 as MinIO endpoint.
- In docker environment, it uses http://minio:9000 (container hostname).
- To allow browser access to signed URLs generated inside Docker, the app replaces the internal endpoint with the public endpoint configured in minio.public-url.
- This ensures signed URLs are accessible externally without code changes.

## Main Endpoints
- POST /upload
  - Upload PDF documents with metadata. Validates max file size.
- GET /download/{documentId}
  - Generates a signed URL for downloading the document by UUID.
- GET /swagger-ui.html
  - Interactive API documentation.

## Unit and Integration Tests
- Tests run under the test profile.
- The test database is initialized with schema scripts located in src/main/resources/db/init/.
- Run all tests with:

```
mvn clean install -Dspring.profiles.active=test
```
- Tests use application-test.yml profile, connecting to the test database and MinIO.

###  Coverage with Jacoco
Jacoco is configured for code coverage reports.

Generate the coverage report with:

```
mvn jacoco:report  
```
-  Open the report at target/site/jacoco/index.html to review coverage.

### Code Formatting with Spotless
Spotless is used to automatically format the codebase.

To apply formatting:
```
./mvnw spotless:apply
```
- To check formatting without applying:

```
./mvnw spotless:check
```
- Make sure to run Spotless before committing code to maintain style consistency.

## 📖 API Documentation

Auto-generated with SpringDoc OpenAPI and available at:

http://localhost:8080/webjars/swagger-ui/index.html

## 🔐 Security & Best Practices

- 🔒 Credentials and secrets are never hardcoded — all config values are in .env and injected via ${}

- 🔧 Config profiles separate dev, test, and production environments

- 🧪 Tests use isolated containers via Testcontainers for PostgreSQL and MinIO


## Notes
- Max file size is configurable via app.max-file-size-bytes (default 500MB).
- Signed URL generation correctly handles MinIO credentials and expiry.
- Use the correct minio.public-url for external access to signed URLs.
- Profiles and environment variables enable seamless switching between local and Docker environments without code changes.


## ✍️ Author

### Erick Garcia
Built with ☕ and ☁️

## 🧾 License

MIT – Feel free to use, modify, and improve.
