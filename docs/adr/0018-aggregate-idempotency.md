# ADR 0018: Aggregate Idempotency

## Status
Accepted

## Decision

Aggregate updates must be idempotent.

## Reason

Ingestion may run multiple times.

## Implementation Notes

- use deterministic keys for aggregate rows
- prefer upserts over inserts

## References

- docs/provider-ingestion.md

## Consequences

- prevents duplicate counting
