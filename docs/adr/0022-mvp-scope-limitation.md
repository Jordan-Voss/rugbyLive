# ADR 0022: MVP Scope Limitation

## Status
Accepted

## Decision

Limit data ingestion to:

- current season
- previous season

## Reason

- faster delivery
- manageable scope

## Implementation Notes

- ingestion jobs filter seasons by configured date windows
- allow the window to expand post-MVP without schema changes

## References

- docs/mvp-scope.md

## Consequences

- limits historical data coverage in MVP
