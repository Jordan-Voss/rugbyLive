# ADR 0007: Historical Entity Identity

## Status
Accepted

## Decision

Entities (teams, competitions) are NOT duplicated over time.

## Context

Names and membership change over time.

## Approach

- stable entity IDs
- versioned names
- season relationships

## Implementation Notes

- store name history in separate tables with effective dates
- link participation to seasons rather than duplicating entities

## References

- docs/data-model.md

## Consequences

- accurate history
- simpler aggregation
