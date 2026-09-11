# AI Helpdesk Platform — Helm Chart

Umbrella chart deploying the full platform (6 microservices + frontend) plus its
infrastructure dependencies (PostgreSQL, Kafka, Redis, Keycloak via Bitnami subcharts).

## Deploying

```bash
helm dependency update infrastructure/helm/helpdesk
helm install helpdesk-platform infrastructure/helm/helpdesk
```

The release name matters: several values (`postgresql.image.repository` references,
Keycloak's `externalDatabase.host`, the Ingress rules) are hardcoded against the release
name `helpdesk-platform` because `values.yaml` isn't Helm-templated and so can't reference
`.Release.Name` itself. Installing under a different release name needs those values
updated to match.

## Known gaps

- **No pgvector.** The stock Bitnami `postgresql` image doesn't include the pgvector
  extension, so `ai-service`'s own Flyway migration (`CREATE EXTENSION vector`) fails
  against it — unlike docker-compose, which uses `pgvector/pgvector:pg16` specifically for
  this. Every other service works fine against this database. Fixing this needs a custom
  image (Bitnami postgresql base with pgvector compiled in — swapping to
  `pgvector/pgvector` directly breaks the chart's Bitnami-specific entrypoint scripts)
  pushed to a registry the cluster can pull from.
- **No observability stack.** docker-compose's Tempo/Prometheus/Grafana have no K8s
  equivalent here. Services will log (harmless) warnings trying to export traces to a
  nonexistent OTLP endpoint.
- **Secrets are plaintext values, not a real secrets manager.** See the `secrets:` block
  in `values.yaml` — fine for local/dev, not for anything real. A Kubernetes Secret is
  base64, not encrypted at rest by default.

## What was actually verified, and how

This chart was deployed to a real cluster (Docker Desktop's single-node Kubernetes) and
debugged until it worked, not just reviewed as YAML. Confirmed stable for 40+ minutes with
zero restarts: PostgreSQL, Redis (all 4 pods), Kafka, Keycloak, frontend, api-gateway, and
notification-service. Keycloak's Postgres authentication was independently verified with a
direct `psql` connection from a throwaway pod.

Bugs found only by actually deploying (not visible from reading the chart):
- Every backend service's Deployment hardcoded container port 8080 for health checks and
  Service routing — none of the six actually listens on 8080 (each has its own port, see
  `values.yaml`). Every one of these pods would have failed every health check forever.
- `postgresql` (our own top-level dependency) and Keycloak's own bundled `postgresql`
  sub-dependency both rendered a StatefulSet/Service/Secret named `<release>-postgresql` —
  an exact, silent collision. Fixed by disabling Keycloak's bundled copy
  (`keycloak.postgresql.enabled: false`) and pointing it at the shared instance via
  `externalDatabase`.
- Bitnami removed most pinned/versioned image tags from the free `docker.io/bitnami`
  namespace in 2025 (kafka and keycloak don't even have a free `:latest` there anymore).
  Every infra pod sat in `ImagePullBackOff` until repointed at `docker.io/bitnamilegacy`,
  Bitnami's own free mirror of the exact same pinned tags.
- The default `livenessProbe.initialDelaySeconds: 30` (our services) and Kafka's own
  `10` were both too short for real JVM/KRaft startup times observed directly in logs
  (60–150+ seconds) — pods were being killed mid-startup, which looks exactly like an app
  crash but isn't. Fixed with generous `startupProbe`s (our services: 5 min budget; Kafka:
  enabled its own, disabled by default in the chart).
- The Ingress's `rewrite-target: /` annotation stripped the `/api` prefix before
  forwarding to api-gateway, whose routes match on `Path=/api/tickets/**` including that
  prefix — every request would have 404'd at the gateway. Removed.
- No `KEYCLOAK_ISSUER_URI` / `KEYCLOAK_JWK_SET_URI` wiring existed at all — the same class
  of JWT validation bug fixed in docker-compose (issuer must match what the browser sees;
  the actual signing-key fetch should use in-cluster DNS). Added to `config.yaml`, kept in
  sync with `keycloak.extraEnvVars.KC_HOSTNAME` in `values.yaml`.

## Known remaining instability (not fixed — needs a better cluster to isolate)

`ai-service`, `analytics-service`, `kb-service`, and `ticket-service` — the four services
that connect to Postgres directly — intermittently fail with a raw
`java.net.SocketTimeoutException: Connect timed out` at the TCP layer (not "connection
refused", not a Postgres auth error) when starting up. Ruled out as the cause:

- Postgres itself: a direct `psql` connection from a throwaway pod succeeded instantly,
  repeatedly, including at moments when the app pods were failing.
- Image/config/secrets: all four pods have the correct image, env vars, and credentials.
- HikariCP connection-pool contention: reduced `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE`
  to 3 (from the default 10) to cut simultaneous connection attempts from ~40 to ~12 - no
  change.
- Restart-storm timing: force-restarting all four together vs. leaving them to their own
  independent `CrashLoopBackOff` timers - no difference either way.

A raw TCP connect timeout (packets not reaching a listener at all, rather than being
actively rejected) pattern-matches known networking flakiness in Docker Desktop's
Windows-hosted single-node `kubeadm` cluster under load (kube-proxy/CNI), not a defect in
this chart. **Recommended next step: re-run this same chart against a cluster with proper
networking** (`kind` on Linux, or a real cloud cluster - EKS/GKE/AKS) to confirm whether
the instability disappears, which would confirm the environment theory rather than leaving
it as an assumption.
