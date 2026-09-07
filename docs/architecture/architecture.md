# System Architecture

```mermaid
graph TD
    Client[React Frontend SPA] -->|HTTPS| IGW[AWS API Gateway / ALB]
    
    subgraph EKS Cluster
        IGW --> Gateway[Spring Cloud Gateway]
        Gateway --> TicketSvc[ticket-service]
        Gateway --> KBSvc[kb-service]
        Gateway --> AISvc[ai-service]
        Gateway --> Auth[Keycloak]
        
        TicketSvc -.->|Transactional Outbox| DB[(PostgreSQL)]
        KBSvc -.->|pgvector| DB
        AISvc -.->|Semantic Search| DB
        
        OutboxWorker[Outbox Worker] -->|Poll| DB
        OutboxWorker -->|Publish| Kafka[Kafka Cluster]
        
        Kafka -->|Consume| NotifSvc[notification-service]
        Kafka -->|Consume| AnalyticsSvc[analytics-service]
        
        TicketSvc -.->|Rate Limit/Presence| Redis[(ElastiCache Redis)]
        KBSvc -.->|Cache-Aside| Redis
    end
    
    AISvc -->|API| OpenAI[OpenAI API]
    
    subgraph Observability
        TicketSvc -.->|OTLP| OTel[OpenTelemetry Collector]
        KBSvc -.->|OTLP| OTel
        AISvc -.->|OTLP| OTel
        OTel --> Tempo[Grafana Tempo]
        OTel --> Prom[Prometheus]
        Tempo --> Grafana[Grafana Dashboards]
        Prom --> Grafana
    end
```

## Database Design
We avoided MongoDB in favor of PostgreSQL to reduce operational complexity. PostgreSQL acts as our relational store for Tickets and Outbox events, our full-text search engine (`tsvector`) for keyword KB lookups, and our Vector Database (`pgvector`) for Semantic Search.
