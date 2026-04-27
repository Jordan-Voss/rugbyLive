# Architecture

## Overview

RugbyLive uses a layered architecture where the mobile app does not access the database directly.

```text
Client App
   |
   v
Spring Boot API
   |
   v
Postgres

External Providers / Scrapers
   |
   v
Ingestion Layer
   |
   v
Domain Tables
```

## Client

The client is an Expo React Native app.


today screen
date swipe
matches grouped by competition
team pages
player pages
league pages
news/settings/favourites tabs

## API Layer

The Spring Boot API is the only supported access path for the app.

The API is responsible for:

exposing public rugby data
exposing authenticated user data
validating JWTs
enforcing permissions
shaping responses for the UI
hiding provider complexity
serving precomputed stat aggregates
exposing admin ingestion endpoints
Database

Supabase Postgres stores:

domain data
provider mappings
match data
user preferences
favourites
raw stats
aggregate stats

Supabase Auth provides user identity and JWTs.

## Ingestion

The ingestion layer pulls data from:

external APIs
scraping sources

Ingested data is normalized into RugbyLive domain entities.

Provider-specific IDs are never exposed directly to the client.

## Live Data

Live data is stored separately from confirmed match data.

matches stores the core authoritative match record
live_match_state stores current live state
match_events stores event timeline data

This prevents incomplete or fast-changing live data from corrupting confirmed historical records.

## Statistics

The system stores both:

raw match-level stats
precomputed aggregate stats

Aggregate tables are included from the start to support fast team/player pages.

## Deployment

MVP deployment uses:

Spring Boot 4
Java 25
Docker
AWS free tier
Supabase Postgres
GitHub Actions
Expo React Native