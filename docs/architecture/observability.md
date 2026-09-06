# Observability Architecture (Phase 11)

This project uses the **OpenTelemetry (OTel)** standard coupled with the **Grafana** ecosystem for full end-to-end observability across the event-driven microservices.

## 1. Tracing (Grafana Tempo)
All Spring Boot microservices utilize the `opentelemetry-spring-boot-starter`. This auto-instruments our application, automatically injecting a unique `trace_id` at the API Gateway which propagates down through `ticket-service`, across Kafka into the `notification-service`, and into the `ai-service`.
- **Custom Spans**: The AI pipeline (in `AiController`) is wrapped with `@WithSpan("rag-pipeline-execution")` so we can distinctly measure the latency of Postgres Vector Search vs OpenAI LLM generation.
- **Storage**: Traces are exported via OTLP directly to **Grafana Tempo** listening on port `4317` (gRPC).

## 2. Metrics (Prometheus)
Spring Boot Actuator exposes Micrometer metrics (including JVM stats, HTTP latencies, and Kafka consumer lags) at `/actuator/prometheus`.
- **Prometheus** scrapes these endpoints every 15 seconds.
- We measure critical SLAs including P99 Ticket Creation latency and AI Generation time.

## 3. Dashboards (Grafana)
Grafana sits at `http://localhost:3001` and connects to both Prometheus and Tempo. Trace-to-log correlation is enabled, allowing developers to jump directly from a spike in the latency graph (Prometheus), into the exact request waterfall (Tempo), down into the contextual error logs.
