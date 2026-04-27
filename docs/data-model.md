# Data Model

## ID Strategy

The database uses numeric internal primary keys for most domain entities.

Supabase user IDs are UUIDs.

Public-facing URLs and API responses may use stable `canonical_key` values.

Example:

```text
teams.id = 123
teams.canonical_key = leinster
```

The canonical key is not the database primary key. It is a stable, readable identifier.

# Core Tables
## providers

Stores external data sources.

Examples:

rugby-api-provider-a
rugby-api-provider-b
scraped-site-a

## external_mappings

Maps external provider IDs to internal RugbyLive entities.

Used for:

teams
players
competitions
seasons
matches
venues

## countries

Stores country metadata.

## competitions

Stores long-lived competition identity.

Example:

Six Nations
United Rugby Championship
Super Rugby Pacific
Rugby Championship

## competition_names

Stores historical competition names.

Example:

Tri Nations
The Rugby Championship

## seasons

Generic season/date range entity.

## competition_seasons

Specific season instance for a competition.

This handles different season windows between competitions.

Examples:

URC 2025/26
Super Rugby 2026
Six Nations 2026
Autumn Nations Series 2026

## competition_season_teams

Links teams to a competition season.

This ensures a team only appears in the season where it actually participated.

## teams

Stores long-lived team identity.

## team_names

Stores historical team names.

## team_aliases

Stores alternative names from providers and search.

## team_assets

Stores logos and image references.

## team_colours

Stores home, away, alternate, and UI colours.

## players

Stores long-lived player identity.

## player_names

Stores player name variations.

## player_team_stints

Tracks when a player belonged to a team.

## venues

Stores stadium/venue data.

## matches

Stores scheduled and confirmed match data.

## live_match_state

Stores current live score/status/minute.

## match_events

Stores the match timeline.

## match_lineups

Stores starting XV and bench data.

## match_team_stats

Stores raw per-match team stats.

## match_player_stats

Stores raw per-match player stats.

## team_season_stats

Stores precomputed aggregate team stats per competition season.

## player_season_stats

Stores precomputed aggregate player stats per competition season.

## team_lifetime_stats

Stores precomputed all-time team stats.

## player_lifetime_stats

Stores precomputed all-time player stats.

## user_profiles

Stores user settings linked to Supabase user ID.

## user_favourite_teams

Stores user/team favourites.

## user_favourite_competitions

Stores user/competition favourites.

# Example Favourites Model
user_favourite_teams
- user_id UUID
- team_id BIGINT
- created_at TIMESTAMP
- unique(user_id, team_id)

This does not modify the team. It only records that a specific user follows a team.

# Example Stat Model

Raw match stat:

match_player_stats
- player_id
- match_id
- tries
- tackles
- metres_carried
- turnovers_won

Precomputed season stat:

player_season_stats
- player_id
- competition_season_id
- appearances
- tries
- tackles
- metres_carried
- turnovers_won

# Why Precomputed Aggregates Exist

The app needs fast player and team pages.

Without aggregates, the API would need to calculate totals from raw match stats every time a user opens a page.

With aggregate tables, the API can return season and lifetime statistics quickly.