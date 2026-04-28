# ADR 0023: No Admin UI Initially

## Status
Accepted

## Decision

Use API tools (e.g. Insomnia) instead of building admin UI.

## Reason

- reduces scope
- faster MVP

## Future

- build internal admin UI

## Implementation Notes

- use authenticated API clients for admin actions
- log all admin changes for auditability

## References

- docs/auth-security.md

## Consequences

- slower manual workflows during MVP
