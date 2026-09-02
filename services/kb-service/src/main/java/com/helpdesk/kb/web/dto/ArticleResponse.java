package com.helpdesk.kb.web.dto;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.kb.domain.KnowledgeArticle;

import java.time.Instant;
import java.util.UUID;

public class ArticleResponse {
    private UUID id;
    private String title;
    private String body;
    private TicketCategory category;
    private Instant createdAt;
    private Instant updatedAt;

    public ArticleResponse() {}

    public ArticleResponse(KnowledgeArticle article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.body = article.getBody();
        this.category = article.getCategory();
        this.createdAt = article.getCreatedAt();
        this.updatedAt = article.getUpdatedAt();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public TicketCategory getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
