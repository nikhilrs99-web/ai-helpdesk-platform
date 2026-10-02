import { api } from './client';
import type { EscalationResponse, SlaStatusResponse } from './types';

// Matches services/ticket-service/.../web/EscalationController.java and
// TicketController's GET /{id}/sla-status.
export const escalationsApi = {
  list: (ticketId: string) => api.get<EscalationResponse[]>(`/tickets/${ticketId}/escalations`),

  create: (ticketId: string, reason: string) =>
    api.post<EscalationResponse>(`/tickets/${ticketId}/escalations`, { reason }),

  approve: (ticketId: string, id: string) =>
    api.patch<EscalationResponse>(`/tickets/${ticketId}/escalations/${id}/approve`),

  reject: (ticketId: string, id: string) =>
    api.patch<EscalationResponse>(`/tickets/${ticketId}/escalations/${id}/reject`),
};

export const slaApi = {
  get: (ticketId: string) => api.get<SlaStatusResponse>(`/tickets/${ticketId}/sla-status`),
};
