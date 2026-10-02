package com.helpdesk.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits an article into ~400-token chunks so each embedding represents one focused passage
 * instead of a whole article averaged into a single vector. Every chunk keeps the article's
 * id/title and gets its position, so re-ingesting an article can replace all its old chunks.
 */
@Component
public class ArticleChunker {

    private final TokenTextSplitter splitter = new TokenTextSplitter(400, 100, 5, 10000, true);

    public List<Document> chunk(String id, String title, String content) {
        Document whole = new Document(content, new HashMap<>(Map.of("id", id, "title", title)));
        List<Document> chunks = new java.util.ArrayList<>(splitter.apply(List.of(whole)));
        if (chunks.isEmpty()) {
            chunks.add(whole);
        }
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put("chunk", i);
            chunks.get(i).getMetadata().put("chunks", chunks.size());
        }
        return chunks;
    }
}
