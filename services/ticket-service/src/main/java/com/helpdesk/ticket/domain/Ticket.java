package com.helpdesk.ticket.domain;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.common.enums.TicketStatus;
import com.helpdesk.ticket.domain.state.TicketState;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "tickets")
public class Ticket extends BaseEntity {

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "requester_id", nullable = false)
    private String requesterId;

    @ManyToOne
    @JoinColumn(name = "assigned_agent_id")
    private Agent assignedAgent;

    @Column(name = "routed_team")
    private String routedTeam;

    /**
     * Category-specific fields (e.g. browser/appVersion for BUG, invoiceId for BILLING),
     * validated and populated by the right TicketTypeHandler (Factory pattern) rather than
     * being fixed columns - each category needs a different shape here. EAGER because
     * TicketResponse always serializes it and open-in-view is disabled - the default LAZY
     * proxy can't be initialized once the request thread is past the repository call, since
     * the Hibernate session that loaded the ticket is already closed by then.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ticket_metadata", joinColumns = @JoinColumn(name = "ticket_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata = new HashMap<>();

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public TicketStatus getStatus() {
        return status;
    }

    /**
     * The only way to change a ticket's status. There is no plain setStatus - going
     * through the State pattern here means an illegal transition (e.g. OPEN straight to
     * RESOLVED) is rejected structurally, not by every caller remembering to check.
     *
     * @throws com.helpdesk.ticket.domain.state.IllegalTicketTransitionException if the
     *         transition is not legal from the ticket's current status
     */
    public void changeStatus(TicketStatus target) {
        TicketState current = TicketState.forStatus(this.status);
        TicketState next = current.transitionTo(target);
        this.status = next.status();
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public Agent getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(Agent assignedAgent) {
        this.assignedAgent = assignedAgent;
    }

    public String getRoutedTeam() {
        return routedTeam;
    }

    public void setRoutedTeam(String routedTeam) {
        this.routedTeam = routedTeam;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
