// Mirrors common/src/main/java/com/helpdesk/common/enums - keep in sync with those.
export type TicketCategory = 'BUG' | 'BILLING' | 'ACCESS' | 'HOW_TO' | 'FEATURE_REQUEST';

export type TicketStatus =
  | 'OPEN'
  | 'AI_TRIAGED'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'WAITING_FOR_CUSTOMER'
  | 'RESOLVED'
  | 'CLOSED';

export const TICKET_CATEGORIES: TicketCategory[] = [
  'BUG',
  'BILLING',
  'ACCESS',
  'HOW_TO',
  'FEATURE_REQUEST',
];

export const TICKET_STATUSES: TicketStatus[] = [
  'OPEN',
  'AI_TRIAGED',
  'ASSIGNED',
  'IN_PROGRESS',
  'WAITING_FOR_CUSTOMER',
  'RESOLVED',
  'CLOSED',
];

// Matches services/ticket-service/.../web/dto/TicketResponse.java
export interface TicketResponse {
  id: string;
  subject: string;
  description: string;
  category: TicketCategory;
  status: TicketStatus;
  requesterId: string;
  assignedAgentId: string | null;
  routedTeam: string;
  metadata: Record<string, string>;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTicketRequest {
  subject: string;
  description: string;
  category: TicketCategory;
  metadata: Record<string, string>;
}

export interface UpdateTicketRequest {
  subject: string;
  description: string;
}

// The fields of Spring Data's Page<T> JSON this app actually reads.
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// Matches services/analytics-service/.../web/AnalyticsController.java
export interface DashboardMetrics {
  totalTickets: number;
  slaCompliancePercentage: number;
  aiResolutionPercentage: number;
}

// Matches services/kb-service/.../web/dto/ArticleResponse.java
export interface ArticleResponse {
  id: string;
  title: string;
  body: string;
  category: TicketCategory;
  createdAt: string;
  updatedAt: string;
}

// Matches common/src/main/java/com/helpdesk/common/dto/ApiError.java
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
