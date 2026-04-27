# Provider Ingestion

## Overview

RugbyLive aggregates data from multiple external providers and scraping sources.

Different providers may be better for different data types.

Examples:

- one provider may have better live scores
- another may have better lineups
- another may have better player stats
- manually maintained data may be better for colours/logos

## Provider-Agnostic Domain Model

Provider data is never exposed directly to the client.

External data is normalized into RugbyLive domain tables.

## external_mappings

The `external_mappings` table maps external provider IDs to internal entities.

Example:

```text
provider: provider_a
entity_type: TEAM
external_id: 8472
internal_entity_id: 123
```

## Provider Priority

Provider trust can vary by data type.

Example:

live score: provider_a
lineups: provider_b
player stats: provider_c
logos/colours: manual
Merge Strategy

The ingestion process should:

fetch provider data
identify the entity using external_mappings
normalize fields
apply provider priority rules
update domain tables
store ingestion metadata

## Conflict Handling

Conflicts may occur when providers disagree.

Examples:

different score
different kickoff time
different team name
duplicate players
missing events

The system should prefer provider rules per field and allow manual correction where needed.

## Manual Overrides

Manual overrides are allowed where provider data is missing or clearly wrong.

Manual data should not be overwritten accidentally by ingestion jobs.

## Initial Admin Approach

No admin UI is required for MVP.

Admin operations are performed through secured API endpoints using Insomnia.