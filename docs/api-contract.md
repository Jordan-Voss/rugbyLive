# API Contract

## Base URL

/api/v1

---

## GET /me

Returns authenticated user context

```json
{
  "user": {
    "id": "uuid",
    "displayName": "Jordan"
  },
  "preferences": {
    "theme": "system",
    "timezone": "Europe/Dublin"
  },
  "favourites": {
    "teams": [
      {
        "id": 123,
        "canonicalKey": "leinster",
        "name": "Leinster"
      }
    ]
  }
}
```

GET /matches?date=YYYY-MM-DD
```json
{
  "date": "2026-04-26",
  "competitions": [
    {
      "id": 1,
      "name": "URC",
      "matches": [
        {
          "id": 999,
          "status": "LIVE",
          "minute": 52,
          "homeTeam": { "name": "Leinster", "score": 21 },
          "awayTeam": { "name": "Munster", "score": 17 }
        }
      ]
    }
  ]
}
```
GET /matches/{id}

Returns full match detail

GET /matches/{id}/events
```json
[
  {
    "minute": 23,
    "type": "TRY",
    "team": "Leinster",
    "player": "James Lowe"
  }
]
```
GET /teams/{key}
```json
{
  "id": 123,
  "canonicalKey": "leinster",
  "name": "Leinster",
  "stadium": "RDS",
  "colours": {
    "primary": "#003A8F"
  }
}
```
POST /favourites/teams/{key}

Adds favourite

DELETE /favourites/teams/{key}

Removes favourite

GET /players/{id}/stats
```json
{
  "season": {
    "competition": "URC",
    "appearances": 8,
    "tries": 5
  }
}
```
---

# 📁 `docs/auth-security.md`

```markdown
# Auth & Security

## Authentication

- Supabase JWT
- validated by API

---

## Roles

### Public
- can view matches and teams

### Authenticated
- favourites
- preferences

### Admin
- ingestion
- mappings

---

## Endpoint Protection

- public endpoints → no auth
- user endpoints → JWT required
- admin endpoints → role required

---

## Future

- premium data tiers
- rate limiting