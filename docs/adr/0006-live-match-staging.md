# ADR 0006: Live Match Staging Table

## Status
Accepted

## Decision

Use a separate live_match_state table.

## Context

Live data changes frequently and may be incorrect.

## Implementation Notes

- write live updates to live_match_state
- promote to confirmed match tables only on validation/finalization

## References

- docs/live-match-model.md

## Consequences

### Positive
- protects confirmed data
- supports retries

### Negative
- additional complexity
