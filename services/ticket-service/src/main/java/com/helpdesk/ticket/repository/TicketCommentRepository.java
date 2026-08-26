package com.helpdesk.ticket.repository;

import com.helpdesk.ticket.domain.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {
}
