# ADR 0009: Public vs Authenticated Data

## Status
Accepted

## Decision

Separate public and authenticated features.

## Public
- matches
- teams

## Authenticated
- favourites
- preferences

## Future
- premium data

## Implementation Notes

- public endpoints remain unauthenticated and cacheable
- authenticated endpoints require JWT and user context

## References

- docs/auth-security.md
- docs/api-contract.md

## Consequences

- flexible monetisation
