import { api } from './client';
import type { ArticleResponse, Page } from './types';

// Matches services/kb-service/.../web/KnowledgeArticleController.java (routed via
// api-gateway's /api/articles/** predicate).
export const articlesApi = {
  search: (q: string, page: number, size = 10) =>
    api.get<Page<ArticleResponse>>(`/articles/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`),
};
