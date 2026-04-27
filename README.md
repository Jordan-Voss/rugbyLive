# RugbyLive API Documentation

RugbyLive is a rugby union live score and statistics platform 

The system is designed to support:

- live scores
- fixtures by date
- match events
- lineups
- team and player pages
- historical competition data
- provider aggregation
- user favourites
- future premium/statistical features

The project is intentionally API-first and backend-led to demonstrate production-grade software engineering, domain modelling, data integration, observability, testing, and deployment practices.

## Core Principles

- Rugby union only
- Spring Boot API between the app and database
- Supabase Postgres for persistence
- Supabase Auth for user authentication
- Provider-agnostic ingestion model
- External provider IDs mapped to internal domain entities
- Historical correctness for competitions, teams, names, seasons, and memberships
- Separate live match state from confirmed match data
- Store raw match stats and precomputed aggregates from the start
- OpenAPI contract-first API development
- Flyway-managed database schema

## High-Level Architecture

```text
Expo React Native App
        |
        v
Spring Boot API
        |
        v
Supabase Postgres

External Providers / Scrapers
        |
        v
Ingestion Jobs
        |
        v
Spring Boot API / DB
```
--- 

## Documentation Index
mvp-scope.md
architecture.md
api-contract.md
data-model.md
historical-data.md
provider-ingestion.md
live-match-model.md
stats-model.md
auth-security.md
admin-model.md
observability.md
testing-strategy.md
deployment.md
diagrams.md
