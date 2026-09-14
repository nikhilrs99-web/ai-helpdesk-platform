import { api } from './client';

// Matches services/ticket-service/.../web/AgentController.java - agent/admin only.
// There is no separate "get status" endpoint: a successful ping *is* the presence
// signal, backed by a Redis TTL key (see AgentPresenceService) that expires if the
// agent stops pinging.
export const agentsApi = {
  ping: () => api.post<void>('/agents/ping'),
};
