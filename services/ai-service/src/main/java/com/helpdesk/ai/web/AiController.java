package com.helpdesk.ai.web;

import tools.jackson.databind.ObjectMapper;
import com.helpdesk.ai.llm.LlmGuard;
import com.helpdesk.ai.rag.ArticleChunker;
import com.helpdesk.ai.rag.HybridRetriever;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final HybridRetriever retriever;
    private final ArticleChunker chunker;
    private final LlmGuard llmGuard;
    private final ObjectMapper objectMapper;
    private final ToolCallbackResolver toolResolver;

    public AiController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
                        HybridRetriever retriever, ArticleChunker chunker, LlmGuard llmGuard,
                        ObjectMapper objectMapper, ToolCallbackResolver toolResolver) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.retriever = retriever;
        this.chunker = chunker;
        this.llmGuard = llmGuard;
        this.objectMapper = objectMapper;
        this.toolResolver = toolResolver;
    }

    @PostMapping("/rag/search")
    @WithSpan("rag-pipeline-execution")
    public String hybridRagSearch(@RequestBody Map<String, String> request) {
        String query = request.getOrDefault("query", "");

        // Vector + full-text legs fused with Reciprocal Rank Fusion (see HybridRetriever).
        List<Document> documents = retriever.retrieve(query, 5);

        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = "You are a helpful IT support assistant. Use the following context to answer the user's question.\nContext:\n" + context;

        return llmGuard.call(() -> chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .call()
                .content());
    }

    @PostMapping("/ticket/analyze")
    public TicketAnalysis analyzeTicket(@RequestBody Map<String, String> request) {
        String description = request.getOrDefault("description", "");

        String systemPrompt = "Analyze the following support ticket description. Respond with ONLY a JSON object with two fields: 'sentiment' (POSITIVE, NEUTRAL, NEGATIVE) and 'category' (BUG, BILLING, ACCESS, HOW_TO, FEATURE_REQUEST).";

        String raw = llmGuard.call(() -> chatClient.prompt()
                .system(systemPrompt)
                .user(description)
                .call()
                .content());
        try {
            return TicketAnalysis.parse(raw, objectMapper);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The model returned an unusable analysis.", e);
        }
    }

    @PostMapping("/ingest")
    @PreAuthorize("hasAnyRole('agent','admin')")
    public Map<String, Object> ingestArticle(@RequestBody Map<String, String> request) {
        String id = request.get("id");
        String title = request.get("title");
        String content = request.get("content");
        if (id == null || title == null || content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id, title and content are required");
        }

        // Re-ingesting an article replaces all of its previous chunks instead of piling up.
        vectorStore.delete(new FilterExpressionBuilder().eq("id", id).build());

        List<Document> chunks = chunker.chunk(id, title, content);
        vectorStore.add(chunks);
        return Map.of("id", id, "chunks", chunks.size());
    }

    @PostMapping("/agent/chat")
    public String agentChat(@RequestBody Map<String, String> request) {
        String userMessage = request.getOrDefault("message", "Hello");

        // Agent orchestration picks the right tool automatically
        return llmGuard.call(() -> chatClient.prompt()
                .system("You are an autonomous support agent. Use the provided tools to fetch ticket details, SLA status, customer history, or search the KB. If a user asks to escalate, you MUST use the createEscalation tool and inform them it is pending human approval. Only call resolveTicket when your answer fully handled the request and the user confirms nothing else is needed.")
                .user(userMessage)
                .toolCallbacks(agentTools())
                .call()
                .content());
    }

    // Spring AI 2 dropped by-name tool selection on the request; resolve the Function-bean
    // tools declared in AgentToolsConfig to callbacks explicitly.
    private List<ToolCallback> agentTools() {
        return List.of(
                "getTicketStatus", "searchKnowledgeBase", "getSLAStatus",
                "getCustomerTickets", "createEscalation", "resolveTicket")
                .stream().map(toolResolver::resolve).toList();
    }
}
