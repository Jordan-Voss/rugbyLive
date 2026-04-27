# ADR 0005: External ID Mapping

## Status
Accepted

## Decision

Use external_mappings table to map provider IDs.

## Reason

Providers use different identifiers.

## Implementation Notes

- enforce uniqueness on (provider, external_id)
- upsert mappings during ingestion to avoid duplicates

## References

- docs/data-model.md
- docs/provider-ingestion.md

## Consequences

- decouples system from providers
- enables multi-provider ingestion
