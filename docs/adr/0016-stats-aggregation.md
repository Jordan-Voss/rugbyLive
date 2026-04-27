# ADR 0016: Stats Aggregation Strategy

## Status
Accepted

## Decision

Store both raw stats and precomputed aggregates.

## Context

Aggregations are expensive at scale.

## Approach

- raw data in match tables
- aggregates in season tables

## Implementation Notes

- store raw stats as immutable match records
- compute aggregates in a scheduled job or finalization step

## References

- docs/data-model.md
- docs/historical-data.md

## Consequences

- faster reads
- additional complexity
