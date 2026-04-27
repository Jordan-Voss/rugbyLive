# ADR 0010: Admin Override Strategy

## Status
Accepted

## Decision

Allow manual overrides of provider data.

## Context

Providers may be incorrect or delayed.

## Approach

- admin endpoints
- override flags

## Implementation Notes

- store overrides with source=admin and audit metadata
- preserve original provider values for traceability

## References

- docs/provider-ingestion.md
- docs/data-model.md

## Consequences

- improved data accuracy
