# Keycloak Setup

Realm, client, roles, and test users are defined as code in
[`infrastructure/docker/keycloak/realm-export/helpdesk-realm.json`](../../infrastructure/docker/keycloak/realm-export/helpdesk-realm.json)
and imported automatically every time the container starts (`start-dev --import-realm`). Keycloak's
storage here is ephemeral (no data volume mounted), so a container restart always re-imports this exact
file — there is no "clicking through the admin console and hoping you remember what you set" involved.

## What's defined
- **Realm**: `helpdesk`
- **Client**: `helpdesk-frontend` — public client, both the standard (browser) and direct-access-grant
  flows enabled, so it can issue tokens for the real frontend later and for quick local testing via
  password grant now.
- **Realm roles**: `customer`, `agent`, `admin`
- **Test users**: `test-customer` (role `customer`) and `test-agent` (role `agent`), passwords in the
  realm-export file. These are local-only fixtures with no relation to any real account — never reuse
  them anywhere real.

## Getting a test token locally
```bash
curl -X POST http://localhost:8080/realms/helpdesk/protocol/openid-connect/token \
  -d "client_id=helpdesk-frontend" \
  -d "grant_type=password" \
  -d "username=test-customer" \
  -d "password=customer-local-dev-123"
```
Returns a JSON body with an `access_token` — a JWT whose payload includes
`"realm_access": {"roles": ["customer"]}`, which is what `ticket-service`'s OAuth2 resource server
config (Day 7) will validate and read roles from.

## Admin console
`http://localhost:8080` &rarr; Administration Console, using the credentials in `.env`
(`KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD`), realm `master`. Useful for poking around, but the
realm-export file is the source of truth — changes made only through the console are lost on restart.

## A real gotcha hit while setting this up
Keycloak 26's declarative **User Profile** feature enables the `VERIFY_PROFILE` required action by
default, which silently requires `firstName` and `lastName` on every user. Test users created without
those two fields fail password-grant login with a generic `invalid_grant` / "Account is not fully set
up" error — nothing about the error message points at the missing name fields. Both test users here
include `firstName`/`lastName` specifically to avoid this.
