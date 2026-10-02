package com.helpdesk.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real hybrid retrieval (ADR-0002): a vector-similarity leg (pgvector) and a keyword leg
 * (Postgres full-text search over the same chunks), merged with Reciprocal Rank Fusion.
 * RRF needs no score normalisation between the two legs - each only contributes by rank -
 * so a chunk that is strong on exact terms (error codes, product names) but semantically
 * distant still surfaces, and vice versa.
 */
@Component
public class HybridRetriever {

    static final int RRF_K = 60;
    private static final int CANDIDATES = 10;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public HybridRetriever(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Document> retrieve(String query, int topK) {
        List<Document> vectorHits = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(CANDIDATES).similarityThreshold(0.5).build());
        return fuse(vectorHits, keywordSearch(query), topK);
    }

    List<Document> keywordSearch(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT id::text AS id, content FROM vector_store "
                        + "WHERE to_tsvector('english', content) @@ plainto_tsquery('english', ?) "
                        + "ORDER BY ts_rank(to_tsvector('english', content), plainto_tsquery('english', ?)) DESC "
                        + "LIMIT " + CANDIDATES,
                (rs, i) -> new Document(rs.getString("id"), rs.getString("content"), Map.of()),
                query, query);
    }

    static List<Document> fuse(List<Document> vectorHits, List<Document> keywordHits, int topK) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, Document> docs = new LinkedHashMap<>();
        accumulate(vectorHits, scores, docs);
        accumulate(keywordHits, scores, docs);

        List<Map.Entry<String, Double>> ranked = new ArrayList<>(scores.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return ranked.stream().limit(topK).map(e -> docs.get(e.getKey())).toList();
    }

    private static void accumulate(List<Document> hits, Map<String, Double> scores, Map<String, Document> docs) {
        for (int rank = 0; rank < hits.size(); rank++) {
            Document d = hits.get(rank);
            scores.merge(d.getId(), 1.0 / (RRF_K + rank + 1), Double::sum);
            docs.putIfAbsent(d.getId(), d);
        }
    }
}
