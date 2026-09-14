import { api } from './client';
import type { DashboardMetrics } from './types';

// Matches services/analytics-service/.../web/AnalyticsController.java
export const analyticsApi = {
  getDashboardMetrics: () => api.get<DashboardMetrics>('/analytics/dashboard'),
};
