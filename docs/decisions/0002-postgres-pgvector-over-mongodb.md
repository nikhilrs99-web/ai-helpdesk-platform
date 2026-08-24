# ADR 0002: Use PostgreSQL + pgvector instead of adding MongoDB

**Status**: Accepted

## Context
The original tech list included MongoDB as an optional document store for knowledge-base articles. Knowledge-base articles (title, body, category) have a fixed, simple shape and don't need MongoDB's schema flexibility. Adding a second database purely to demonstrate "using MongoDB" risks looking like technology collecting rather than a deliberate architectural choice.

## Decision
Store all data — tickets, knowledge-base articles, and vector embeddings — in a single **PostgreSQL** instance. Use the **pgvector** extension for semantic (embedding) search and Postgres's native full-text search (`tsvector`/`ts_rank`) for keyword search, so the same database serves both legs of hybrid retrieval.

## Consequences
- One database to run, back up, and reason about instead of two.
- Hybrid retrieval (semantic + keyword) becomes simpler: both queries run against the same store, no cross-database joins.
- Loses MongoDB's flexible schema — acceptable, since knowledge-base articles have a fixed shape.
- If a genuinely document-shaped use case appears later, MongoDB can still be added deliberately at that point.
