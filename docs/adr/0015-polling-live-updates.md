# ADR 0015: Polling for Live Updates

## Status
Accepted

## Decision

Use polling (30–60s) for MVP.

## Reason

- simple
- low cost

## Future

- WebSockets or SSE

## Implementation Notes

- polling interval is configurable per environment
- cache short-lived responses for repeated requests

## References

- docs/live-match-model.md

## Consequences

- slight delay in updates
