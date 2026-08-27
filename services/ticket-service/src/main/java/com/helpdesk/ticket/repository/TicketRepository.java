package com.helpdesk.ticket.repository;

import com.helpdesk.ticket.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    boolean existsByIdAndRequesterId(UUID id, String requesterId);

    Page<Ticket> findByRequesterId(String requesterId, Pageable pageable);
}
