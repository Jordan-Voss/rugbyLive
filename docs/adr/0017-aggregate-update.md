# ADR 0017: Aggregate Update Timing

## Status
Accepted

## Decision

Update aggregates when match becomes final.

## Reason

- prevents incorrect stats

## Implementation Notes

- trigger aggregation when match status becomes FINAL
- re-run safely after corrections

## References

- docs/live-match-model.md

## Consequences

- slight delay in stats availability
