# ADR 0008: Competition Name Changes

## Status
Accepted

## Decision

Handle renames via competition_names table.

## Example

Tri Nations → Rugby Championship

## Implementation Notes

- competition_names includes start/end dates per name
- API resolves the active name by match date

## References

- docs/data-model.md

## Consequences

- preserves history
- avoids duplication
