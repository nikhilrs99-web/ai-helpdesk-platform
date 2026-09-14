import { api } from './client';
import type { CreateTicketRequest, Page, TicketResponse, TicketStatus, UpdateTicketRequest } from './types';

// Matches services/ticket-service/.../web/TicketController.java (routed via
// api-gateway's /api/tickets/** predicate).
export const ticketsApi = {
  list: (page: number, size = 10) =>
    api.get<Page<TicketResponse>>(`/tickets?page=${page}&size=${size}&sort=createdAt,desc`),

  get: (id: string) => api.get<TicketResponse>(`/tickets/${id}`),

  create: (request: CreateTicketRequest) => api.post<TicketResponse>('/tickets', request),

  update: (id: string, request: UpdateTicketRequest) =>
    api.put<TicketResponse>(`/tickets/${id}`, request),

  changeStatus: (id: string, status: TicketStatus) =>
    api.patch<TicketResponse>(`/tickets/${id}/status`, { status }),
};
