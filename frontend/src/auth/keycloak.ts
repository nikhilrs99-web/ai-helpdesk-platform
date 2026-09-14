import Keycloak from 'keycloak-js';

// Matches the "helpdesk-frontend" public client in
// infrastructure/docker/keycloak/realm-export/helpdesk-realm.json - its redirect URIs
// already cover both the Vite dev server (5173) and the Docker/nginx build (3000).
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8080',
  realm: 'helpdesk',
  clientId: 'helpdesk-frontend',
});

export default keycloak;
