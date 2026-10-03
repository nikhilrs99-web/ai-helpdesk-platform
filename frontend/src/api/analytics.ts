import { api } from './client';
import type { DashboardMetrics, VolumePoint } from './types';

// Matches services/analytics-service/.../web/AnalyticsController.java
export const analyticsApi = {
  getDashboardMetrics: () => api.get<DashboardMetrics>('/analytics/dashboard'),
  getTicketVolume: (days = 7) => api.get<VolumePoint[]>(`/analytics/volume?days=${days}`),
};
