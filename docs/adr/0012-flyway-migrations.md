# ADR 0012: Flyway Migrations

## Status
Accepted

## Decision

Use Flyway for schema migrations.

## Context

Database evolves over time.

## Implementation Notes

- keep migrations in versioned SQL files
- run migrations automatically on deploy

## References

- docs/deployment.md

## Consequences

- reproducible schema
- version control
