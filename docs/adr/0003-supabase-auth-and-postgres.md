# ADR 0003: Supabase for Auth and Database

## Status
Accepted

## Decision

Use Supabase for:

- PostgreSQL database
- Authentication (JWT)

## Context

Need managed DB + auth without overhead.

## Implementation Notes

- store Supabase connection and JWT settings as environment variables
- validate JWTs in the API and map to internal user IDs

## References

- docs/auth-security.md
- docs/deployment.md

## Consequences

### Positive
- fast setup
- managed infrastructure

### Negative
- dependency on external platform
