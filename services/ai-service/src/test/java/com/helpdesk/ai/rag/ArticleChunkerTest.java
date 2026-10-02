package com.helpdesk.ai.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleChunkerTest {

    private final ArticleChunker chunker = new ArticleChunker();

    @Test
    void longArticleIsSplitIntoSeveralChunksCarryingArticleMetadata() {
        String content = "Resetting your password requires access to your registered email address. ".repeat(200);

        List<Document> chunks = chunker.chunk("kb-1", "Password reset", content);

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> {
            assertThat(c.getMetadata()).containsEntry("id", "kb-1").containsEntry("title", "Password reset");
            assertThat(c.getMetadata()).containsKey("chunk");
        });
    }

    @Test
    void tinyArticleIsStillKeptAsOneChunk() {
        assertThat(chunker.chunk("kb-2", "t", "c")).hasSize(1);
    }
}
