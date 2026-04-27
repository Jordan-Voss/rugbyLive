# ADR 0004: Provider Aggregation Strategy

## Status
Accepted

## Decision

Support multiple data providers and merge their data.

## Context

No single provider has complete, reliable data.

## Approach

- normalize provider data
- map via external_mappings
- prioritise fields per provider

## Implementation Notes

- convert provider payloads into a common internal schema
- use a field-priority map to select the most trusted source

## References

- docs/provider-ingestion.md
- docs/live-match-model.md

## Consequences

### Positive
- higher data quality
- resilience

### Negative
- increased complexity
