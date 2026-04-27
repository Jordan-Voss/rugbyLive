# ADR 0011: OpenAPI Contract-First

## Status
Accepted

## Decision

Define API via OpenAPI spec.

## Reason

- consistent API
- enables client generation

## Implementation Notes

- treat OpenAPI as the source of truth
- generate client types from the spec in CI

## References

- docs/api-contract.md

## Consequences

- improved reliability
