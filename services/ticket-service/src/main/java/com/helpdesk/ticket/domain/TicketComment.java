package com.helpdesk.ticket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_comments")
public class TicketComment extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "ai_drafted", nullable = false)
    private boolean aiDrafted = false;

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isAiDrafted() {
        return aiDrafted;
    }

    public void setAiDrafted(boolean aiDrafted) {
        this.aiDrafted = aiDrafted;
    }
}
