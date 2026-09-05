package com.helpdesk.ai;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Disabled("Manual evaluation harness - requires actual OpenAI key and populated VectorStore to run")
class RagEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(RagEvaluationTest.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void testRetrievalPrecisionAtK() {
        // Golden Query: "How do I reset my password?"
        // Expected Article ID: "kb-101"
        
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.query("How do I reset my password?").withTopK(3)
        );

        // Check if the expected document is in the top 3 results (Precision@3)
        boolean found = results.stream()
                .anyMatch(doc -> "kb-101".equals(doc.getMetadata().get("id")));
                
        // In a real automated run, we'd loop over the dataset and calculate average precision
        log.info("Retrieval Precision check. Found expected document: {}", found);
        // assertThat(found).isTrue(); 
    }

    @Test
    void testAnswerFaithfulness() {
        // Golden Query: "How do I reset my password?"
        String retrievedContext = "To reset your password, click the 'forgot password' link on the login page.";
        String systemPrompt = "You are an evaluator. Determine if the ANSWER is strictly derived from the CONTEXT. " +
                              "Answer with only 'YES' or 'NO'.\n\nCONTEXT:\n" + retrievedContext;
                              
        String generatedAnswer = "You can reset your password by clicking 'forgot password' on the login page.";
        
        String evalResult = chatClientBuilder.build().prompt()
                .system(systemPrompt)
                .user("ANSWER: " + generatedAnswer)
                .call()
                .content();
                
        log.info("Faithfulness evaluation result: {}", evalResult);
        // assertThat(evalResult.trim().toUpperCase()).isEqualTo("YES");
    }
}
