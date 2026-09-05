CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(1536) -- 1536 is default for OpenAI embeddings
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);
