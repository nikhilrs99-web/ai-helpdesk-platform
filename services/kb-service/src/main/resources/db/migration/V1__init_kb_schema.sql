CREATE TABLE knowledge_article (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    search_vector tsvector
);

-- Create a function to update the tsvector column based on title and body
CREATE OR REPLACE FUNCTION knowledge_article_search_vector_trigger() RETURNS trigger AS $$
BEGIN
  new.search_vector :=
     setweight(to_tsvector('english', coalesce(new.title,'')), 'A') ||
     setweight(to_tsvector('english', coalesce(new.body,'')), 'B');
  return new;
END
$$ LANGUAGE plpgsql;

-- Trigger to automatically update search_vector before insert or update
CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
    ON knowledge_article FOR EACH ROW EXECUTE FUNCTION knowledge_article_search_vector_trigger();

-- Create a GIN index for fast full-text search
CREATE INDEX idx_kb_search_vector ON knowledge_article USING GIN(search_vector);
