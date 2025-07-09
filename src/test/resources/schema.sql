-- UUID extension

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create table

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(255) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    tags TEXT[] NOT NULL,
    minio_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index

CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_documents_document_name ON documents(document_name);
CREATE INDEX idx_documents_tags ON documents USING GIN (tags);
CREATE UNIQUE INDEX ux_user_document_name ON documents (user_id, document_name);