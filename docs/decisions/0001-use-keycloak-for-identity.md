# ADR 0001: Use Keycloak instead of a custom auth-service

**Status**: Accepted

## Context
The platform needs authentication/authorization across multiple services (ticket-service, kb-service, ai-service, etc.). The simplest option is a hand-rolled `auth-service` that issues and validates JWTs itself.

## Decision
Use **Keycloak** as an external OAuth2/OIDC identity provider. Services are configured as OAuth2 resource servers that validate Keycloak-issued JWTs, and roles (customer, agent, admin) are managed as Keycloak realm roles mapped to method-level `@PreAuthorize` checks.

## Consequences
- One more container to run locally (Keycloak), but this mirrors how real production systems delegate identity to a dedicated IdP rather than reinventing it.
- No custom password-hashing, token-issuance, or refresh-token logic to build or secure ourselves.
- Centralizes user/role management outside of any single service — no service owns "the" user table.
