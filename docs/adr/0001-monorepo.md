# ADR 0001: Monorepo Structure

## Status
Accepted

## Decision

Use a monorepo containing:

- API (Spring Boot)
- Mobile app (React Native / Expo)
- Infrastructure

## Context

The system consists of multiple tightly coupled components that evolve together.

## Options Considered

### Multi-repo
- harder to coordinate changes
- duplicated setup

### Monorepo (chosen)
- single source of truth
- easier refactoring across layers
- shared tooling

## Implementation Notes

- keep apps/ as the primary boundary for services and clients
- share linting, formatting, and CI configuration at the repo root

## References

- docs/architecture.md
- README.md

## Consequences

### Positive
- simpler CI/CD
- easier local development

### Negative
- larger repository
