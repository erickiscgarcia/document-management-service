package com.garbed.document_management_service.util;

/**
 * Constants class used for Constants in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/30/25
 */
public final class SystemConstants {

  private SystemConstants() {}

  public static final class Config {
    public static final String PROJECT_TITLE = "Document Management API";
    public static final String PROJECT_DESCRIPTION =
        "Upload, list and manage PDF documents using MinIO and PostgreSQL.";
    public static final String API_VERSION = "1.0.0";
  }

  public static final class Exception {
    public static final String DOCUMENT_NOT_FOUND = "Document not found.";
    public static final String MINIO_UPLOAD_EXCEPTION = "Error uploading file to MinIO.";
    public static final String FILE_UPLOAD_EXCEPTION = "Error reading file stream.";
    public static final String MAX_FILE_SIZE_EXCEPTION = "File size exceeds the allowed 500MB limit.";
  }

  public static final class Util {
    public static final String PERCENTAGE_CHAR = "%";
    public static final String SLASH_CHAR = "/";
    public static final long MAX_FILE_SIZE_BYTES = 500L * 1024 * 1024; // 500MB
  }

  public static final class File {
    public static final String UPLOAD_PREFIX = "upload-";
    public static final String PDF_EXTENSION = ".pdf";
    public static final String CONTENT_TYPE = "application/pdf";
  }

  public static final class Pg {
    public static final String ARRAY_START = "{";
    public static final String ARRAY_END = "}";
    public static final String ARRAY_SEPARATOR = ",";
    public static final String ARRAY_QUOTE_START = "\"";
    public static final String ARRAY_QUOTE_END = "\\\"";
  }
}
