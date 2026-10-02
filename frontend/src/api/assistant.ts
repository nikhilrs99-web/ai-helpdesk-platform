import { api } from './client';

// Matches services/ai-service/.../web/AiController.java (routed via api-gateway's /api/ai/**).
// agent/chat returns the model's reply as plain text, not JSON.
export const assistantApi = {
  chat: (message: string) => api.post<string>('/ai/agent/chat', { message }),
};
