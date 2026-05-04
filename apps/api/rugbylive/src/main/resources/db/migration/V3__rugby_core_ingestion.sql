CREATE TABLE countries (
                           id BIGSERIAL PRIMARY KEY,
                           canonical_key TEXT NOT NULL UNIQUE,
                           name TEXT NOT NULL,
                           code TEXT UNIQUE,
                           flag_url TEXT,
                           flag_source TEXT,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE competitions (
                              id BIGSERIAL PRIMARY KEY,
                              canonical_key TEXT NOT NULL UNIQUE,
                              current_name TEXT NOT NULL,
                              type TEXT,
                              tier INTEGER,
                              format TEXT,
                              international BOOLEAN NOT NULL DEFAULT FALSE,
                              gender TEXT,
                              provider_country_id BIGINT REFERENCES countries(id),
                              display_region TEXT,
                              primary_colour TEXT,
                              secondary_colour TEXT,
                              logo_url TEXT,
                              logo_source TEXT,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE competition_seasons (
                                     id BIGSERIAL PRIMARY KEY,
                                     competition_id BIGINT NOT NULL REFERENCES competitions(id),
                                     season_year INTEGER NOT NULL,
                                     display_name TEXT NOT NULL,
                                     short_name TEXT,
                                     logo_url TEXT,
                                     start_date DATE,
                                     end_date DATE,
                                     current BOOLEAN NOT NULL DEFAULT FALSE,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     UNIQUE (competition_id, season_year)
);

CREATE TABLE competition_aliases (
                                     id BIGSERIAL PRIMARY KEY,
                                     competition_id BIGINT NOT NULL REFERENCES competitions(id),
                                     alias TEXT NOT NULL,
                                     normalized TEXT NOT NULL,
                                     provider TEXT,
                                     valid_from_year INTEGER,
                                     valid_to_year INTEGER,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     UNIQUE (competition_id, normalized, provider)
);

CREATE TABLE competition_countries (
                                       competition_id BIGINT NOT NULL REFERENCES competitions(id),
                                       country_id BIGINT NOT NULL REFERENCES countries(id),
                                       role TEXT NOT NULL DEFAULT 'participant',
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       PRIMARY KEY (competition_id, country_id, role)
);

CREATE TABLE teams (
                       id BIGSERIAL PRIMARY KEY,
                       canonical_key TEXT NOT NULL UNIQUE,
                       name TEXT NOT NULL,
                       short_name TEXT,
                       logo_url TEXT,
                       logo_source TEXT,
                       national BOOLEAN NOT NULL DEFAULT FALSE,
                       founded INTEGER,
                       country_id BIGINT REFERENCES countries(id),
                       alternate_names TEXT[],
                       colours JSONB,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE stadiums (
                          id BIGSERIAL PRIMARY KEY,
                          canonical_key TEXT NOT NULL UNIQUE,
                          name TEXT NOT NULL,
                          capacity INTEGER,
                          location TEXT,
                          country_id BIGINT REFERENCES countries(id),
                          created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE team_stadiums (
                               team_id BIGINT NOT NULL REFERENCES teams(id),
                               stadium_id BIGINT NOT NULL REFERENCES stadiums(id),
                               is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                               start_date DATE,
                               end_date DATE,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                               PRIMARY KEY (team_id, stadium_id)
);

CREATE TABLE competition_season_teams (
                                          competition_id BIGINT NOT NULL REFERENCES competitions(id),
                                          competition_season_id BIGINT NOT NULL REFERENCES competition_seasons(id),
                                          team_id BIGINT NOT NULL REFERENCES teams(id),
                                          created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                          PRIMARY KEY (competition_id, competition_season_id, team_id)
);

CREATE TABLE external_mappings (
                                   id BIGSERIAL PRIMARY KEY,
                                   provider TEXT NOT NULL,
                                   entity_type TEXT NOT NULL,
                                   internal_id TEXT NOT NULL,
                                   external_id TEXT NOT NULL,
                                   external_name TEXT,
                                   is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                   confidence TEXT NOT NULL DEFAULT 'provider',
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   UNIQUE (provider, entity_type, external_id)
);

CREATE TABLE image_hashes (
                              id BIGSERIAL PRIMARY KEY,
                              entity_id TEXT NOT NULL,
                              entity_type TEXT NOT NULL,
                              hash_image TEXT NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                              updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_competitions_provider_country_id ON competitions(provider_country_id);
CREATE INDEX idx_competition_seasons_competition_id ON competition_seasons(competition_id);
CREATE INDEX idx_competition_countries_country_id ON competition_countries(country_id);
CREATE INDEX idx_teams_country_id ON teams(country_id);
CREATE INDEX idx_external_mappings_lookup ON external_mappings(provider, entity_type, external_id);
CREATE INDEX idx_external_mappings_internal ON external_mappings(entity_type, internal_id);