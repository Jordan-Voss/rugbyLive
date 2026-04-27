# MVP Scope

## Product Goal

The MVP should prove that RugbyLive can support a real-world rugby live score experience with clean architecture and extensible data modelling.

The first version should be small enough to build, but structured so that adding more competitions, providers, statistics, and features does not require a rewrite.

## Sport

Rugby union only.

Rugby league is out of scope.

## Competitions

The MVP will include:

- 2 club competitions
- 2 international competitions

The goal of including multiple competitions is to increase the likelihood of having live matches available during development and testing.

## Historical Scope

The schema supports historical seasons, team membership changes, competition name changes, team name changes, and historical stats.

The MVP will ingest:

- current season
- previous season

Older historical data is out of scope for the first release but supported by the model.

## Match Features

The MVP includes:

- fixtures by date
- matches grouped by competition
- match status
- live scores
- half-time and full-time scores
- match events
- lineups
- team match stats
- player match stats

## Match Events

Supported event types include:

- try
- conversion
- penalty goal
- drop goal
- yellow card
- red card
- substitution
- penalty try
- missed conversion
- missed penalty

## Team Features

The MVP includes:

- team profile
- logo
- colours
- aliases
- current competition memberships
- fixtures
- recent results
- squad where provider data exists
- team match stats
- team season stat aggregates

## Player Features

The MVP includes:

- player profile
- player image where available
- team association
- match stats
- season stat aggregates

Full lifetime/career statistics are supported by the schema but may be incomplete depending on available provider data.

## User Features

The MVP may include authentication from the start, using Supabase Auth.

Authenticated users can:

- favourite teams
- favourite competitions
- store preferences

Unauthenticated users can view public match and score data.

## Admin Features

The MVP does not require an admin UI.

Admin operations will initially be performed through secured API endpoints using Insomnia.

Admin features include:

- triggering ingestion runs
- viewing unmatched provider mappings
- fixing mappings
- applying manual overrides where needed

## Out of Scope for MVP

- Rugby league
- WebSockets
- push notifications
- betting odds
- full premium subscription system
- complete historical archive
- polished admin dashboard
- advanced predictive analytics