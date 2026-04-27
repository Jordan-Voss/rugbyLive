# Live Match Model

## Overview

Live match data is handled separately from confirmed match data to ensure:

- real-time updates do not corrupt historical data
- ingestion can safely retry updates
- UI can display live state independently

---

## Tables

### matches (authoritative)

Stores confirmed match data:

- final score
- official events
- status (FULL_TIME, etc)

---

### live_match_state

Stores real-time updates:

- current score
- current minute
- live status
- temporary events

---

## Flow

1. Provider ingestion updates `live_match_state`
2. API serves live data from `live_match_state`
3. When match becomes FULL_TIME:
   - data is copied to `matches`
   - `is_final = true`
4. Aggregates are updated

---

## Status Lifecycle


SCHEDULED → LIVE → HALF_TIME → LIVE → FULL_TIME



Other statuses:

- POSTPONED
- CANCELLED
- ABANDONED

---

## Important Rule

Aggregates are only updated when:


match.is_final = true


---

## API Behaviour

When requesting matches:

- if match is live → use `live_match_state`
- if match is finished → use `matches`

---

## Future Improvements

- WebSockets or SSE for real-time updates
- finer-grained event streaming