# ADR 0021: Favourites Model

## Status
Accepted

## Decision

Store favourites as user relationships.

## Implementation Notes

- store favourites in a join table keyed by user_id and entity_id
- enforce uniqueness to prevent duplicates

## References

- docs/data-model.md

## Consequences

- scalable
- flexible
