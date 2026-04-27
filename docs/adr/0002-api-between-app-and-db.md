# ADR 0002: API Between App and Database

## Status
Accepted

## Decision

All client communication goes through the API.

## Context

Direct DB access (e.g. via Supabase) was considered.

## Options

### Direct DB access
Rejected:
- leaks schema
- limits flexibility
- poor for CV/system design

### API layer (chosen)
- abstraction
- security
- control over responses

## Implementation Notes

- all clients use the API, even for reads
- enforce auth, validation, and rate limiting at the API boundary
- keep contract-first OpenAPI as the source of truth

## References

- docs/api-contract.md
- docs/auth-security.md

## Consequences

- stronger architecture
- easier future scaling
