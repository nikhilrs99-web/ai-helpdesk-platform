package com.helpdesk.ai.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AiController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    @PostMapping("/rag/search")
    public String hybridRagSearch(@RequestBody Map<String, String> request) {
        String query = request.getOrDefault("query", "");

        // 1. Retrieve relevant context from pgvector
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.query(query).withTopK(3)
        );

        String context = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        // 2. Generate response using LLM (RAG pattern)
        String systemPrompt = "You are a helpful IT support assistant. Use the following context to answer the user's question.\nContext:\n" + context;
        
        return chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .call()
                .content();
    }

    @PostMapping("/ticket/analyze")
    public String analyzeTicket(@RequestBody Map<String, String> request) {
        String description = request.getOrDefault("description", "");
        
        String systemPrompt = "Analyze the following support ticket description. Return a JSON object with two fields: 'sentiment' (POSITIVE, NEUTRAL, NEGATIVE) and 'category' (BUG, BILLING, ACCESS, HOW_TO, FEATURE_REQUEST).";
        
        return chatClient.prompt()
                .system(systemPrompt)
                .user(description)
                .call()
                .content();
    }

    @PostMapping("/ingest")
    public void ingestArticle(@RequestBody Map<String, String> request) {
        String id = request.get("id");
        String title = request.get("title");
        String content = request.get("content");
        
        Document doc = new Document(content, Map.of("id", id, "title", title));
        // Note: Spring AI handles generating the embedding via the configured EmbeddingModel
        // and saving it to the VectorStore.
        vectorStore.accept(List.of(doc));
    }
}
