import keycloak from '../auth/keycloak';

export class ApiRequestError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
    this.name = 'ApiRequestError';
  }
}

const FALLBACK_MESSAGES: Record<number, string> = {
  401: 'Your session has expired. Please sign in again.',
  403: 'You do not have permission to perform this action.',
  404: 'Not found.',
  429: 'Too many requests - please slow down and try again shortly.',
};

// All backend routes live behind the gateway's /api/** prefix (see
// services/api-gateway/src/main/resources/application.yml). Vite's dev-server proxy and
// nginx.conf both forward this same relative path, so callers never need an absolute URL.
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (keycloak.token) {
    headers.set('Authorization', `Bearer ${keycloak.token}`);
  }
  if (options.body) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`/api${path}`, { ...options, headers });

  if (!response.ok) {
    let message = FALLBACK_MESSAGES[response.status] ?? `Request failed (${response.status})`;
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
    } catch {
      // No JSON body (e.g. the 429 rate-limit response) - fall back message stands.
    }
    throw new ApiRequestError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
};
