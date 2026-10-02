package com.helpdesk.ai.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HybridRetrieverTest {

    private Document doc(String id) {
        return new Document(id, "text " + id, Map.of());
    }

    @Test
    void chunkFoundByBothLegsOutranksChunksFoundByOne() {
        List<Document> vector = List.of(doc("a"), doc("b"), doc("c"));
        List<Document> keyword = List.of(doc("c"), doc("d"));

        List<Document> fused = HybridRetriever.fuse(vector, keyword, 4);

        assertThat(fused).extracting(Document::getId).startsWith("c");
        assertThat(fused).extracting(Document::getId).containsExactlyInAnyOrder("a", "b", "c", "d");
    }

    @Test
    void keywordOnlyHitStillSurfacesAndTopKIsRespected() {
        List<Document> fused = HybridRetriever.fuse(List.of(doc("a"), doc("b")), List.of(doc("z")), 2);

        assertThat(fused).hasSize(2);
        assertThat(fused).extracting(Document::getId).contains("a", "z");
    }

    @Test
    void emptyLegsGiveEmptyResult() {
        assertThat(HybridRetriever.fuse(List.of(), List.of(), 5)).isEmpty();
    }
}
