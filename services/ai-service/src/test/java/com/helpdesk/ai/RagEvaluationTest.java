package com.helpdesk.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A real evaluation harness against the golden dataset in eval-dataset.json - still
 * @Disabled by default because it needs a real OpenAI key and a VectorStore actually
 * populated with the kb-10x articles the dataset's expectedArticleId values reference
 * (via POST /api/ai/ingest), neither of which exist in a default local run. Enable and run
 * manually (`mvn test -Dtest=RagEvaluationTest -Dsurefire.failIfNoSpecifiedTests=false` with
 * @Disabled removed, or your IDE's "run test" with the annotation commented out) after
 * ingesting the matching articles.
 */
@SpringBootTest
@Disabled("Manual evaluation harness - requires a real OpenAI key and a VectorStore already populated with the golden dataset's articles")
class RagEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(RagEvaluationTest.class);
    private static final int TOP_K = 3;
    // Below this, retrieval is not doing its job well enough to trust the rest of the
    // pipeline - tune based on real runs against a real populated store, not guessed.
    private static final double MIN_ACCEPTABLE_PRECISION = 0.6;

    private record GoldenQuery(String query, String expectedArticleId, String expectedArticleTitle,
                                List<String> expectedAnswerKeywords) {}

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private List<GoldenQuery> loadDataset() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return List.of(mapper.readValue(
                new ClassPathResource("eval-dataset.json").getInputStream(), GoldenQuery[].class));
    }

    @Test
    void testRetrievalPrecisionAtK() throws Exception {
        List<GoldenQuery> dataset = loadDataset();
        int hits = 0;

        for (GoldenQuery golden : dataset) {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder().query(golden.query()).topK(TOP_K).build());

            boolean found = results.stream()
                    .anyMatch(doc -> golden.expectedArticleId().equals(doc.getMetadata().get("id")));

            log.info("Precision@{} check for \"{}\": expected {} -> {}",
                    TOP_K, golden.query(), golden.expectedArticleId(), found ? "FOUND" : "MISSED");
            if (found) hits++;
        }

        double precision = (double) hits / dataset.size();
        log.info("Overall Precision@{}: {}/{} = {}", TOP_K, hits, dataset.size(), precision);
        assertThat(precision)
                .withFailMessage("Precision@%d was %.2f (%d/%d), below the %.2f floor - retrieval quality regressed",
                        TOP_K, precision, hits, dataset.size(), MIN_ACCEPTABLE_PRECISION)
                .isGreaterThanOrEqualTo(MIN_ACCEPTABLE_PRECISION);
    }

    @Test
    void testAnswerFaithfulness() throws Exception {
        List<GoldenQuery> dataset = loadDataset();
        ChatClient chatClient = chatClientBuilder.build();
        int faithful = 0;

        for (GoldenQuery golden : dataset) {
            List<Document> retrieved = vectorStore.similaritySearch(
                    SearchRequest.builder().query(golden.query()).topK(TOP_K).build());
            String context = retrieved.stream().map(Document::getText).reduce("", (a, b) -> a + "\n\n" + b);

            String answer = chatClient.prompt()
                    .system("You are a helpful IT support assistant. Use the following context to answer the user's question.\nContext:\n" + context)
                    .user(golden.query())
                    .call()
                    .content();

            String judgePrompt = "You are an evaluator. Determine if the ANSWER is strictly derived from the CONTEXT "
                    + "(no claims that aren't supported by it). Answer with only 'YES' or 'NO'.\n\nCONTEXT:\n" + context;
            String verdict = chatClient.prompt()
                    .system(judgePrompt)
                    .user("ANSWER: " + answer)
                    .call()
                    .content();

            boolean isFaithful = verdict != null && verdict.trim().toUpperCase().startsWith("YES");
            log.info("Faithfulness check for \"{}\": {} (answer: {})", golden.query(), verdict, answer);
            if (isFaithful) faithful++;
        }

        double faithfulnessRate = (double) faithful / dataset.size();
        log.info("Overall faithfulness rate: {}/{} = {}", faithful, dataset.size(), faithfulnessRate);
        assertThat(faithfulnessRate)
                .withFailMessage("Faithfulness rate was %.2f (%d/%d) - the LLM is generating unsupported claims",
                        faithfulnessRate, faithful, dataset.size())
                .isGreaterThanOrEqualTo(MIN_ACCEPTABLE_PRECISION);
    }
}
