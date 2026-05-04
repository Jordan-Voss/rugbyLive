# AGENTS.md — RugbyLive API

Spring Boot 3 REST API backed by **Supabase PostgreSQL**. Uses Flyway for migrations, Spring Security OAuth2 resource server for JWT auth, and pulls live rugby data from **API-Sports**.

---

## Architecture

```
SecurityConfig → Controller → AdminDataService → (ExternalRugbyDataProvider + Repositories) → Supabase PostgreSQL
```

| Package | Role |
|---|---|
| `config/` | `SecurityConfig` — CORS, JWT, route auth rules |
| `user/` | User profile CRUD; `UserService` resolves/creates profile from JWT subject |
| `competition/`, `team/`, `country/` | Core rugby domain entities + repositories |
| `admin/externaldata/` | `AdminExternalDataController` + per-entity services (`AdminCountryDataService`, `AdminCompetitionDataService`, `AdminTeamDataService`) — own the RugbyLive refresh workflow |
| `admin/externaldata/resolution/` | `ExternalDataResult`, `ExternalDataItem`, `ExternalDataSummary`, `ExternalDataDecision`, `MultiProviderDataResult` — preview/commit result types |
| `admin/externaldata/rules/` | `ExternalDataRuleService` — loads YAML provider rules/overrides; `CompetitionProviderRule`, `TeamProviderRule`, `CountryProviderRule`, `SeasonProviderRule` |
| `provider/` | `ExternalRugbyDataProvider` interface + thin record types (`ExternalCountryRecord`, `ExternalCompetitionRecord`, `ExternalTeamRecord`, `ExternalSeasonRecord`) |
| `provider/apisports/` | `ApiSportsClient` (HTTP), `ApiSportsRugbyDataProvider` (thin adapter implements `ExternalRugbyDataProvider`), DTOs |
| `mapping/` | `ExternalMapping` — maps provider IDs to internal RugbyLive IDs |
| `health/` | Single `/api/v1/health` public ping endpoint |

### Layer responsibilities

```
admin/externaldata/*   = RugbyLive business process (what to refresh, how to resolve, what to write)
provider/apisports/    = API-Sports-specific HTTP adapter (thin, no domain logic)
admin/externaldata/rules/ = YAML corrections — provider names → RugbyLive canonical values
admin/externaldata/resolution/ = result/preview types shared across all entity services
```

Adding a new provider only requires:
1. `provider/{name}/{Name}Client.java` + DTOs
2. `provider/{name}/{Name}RugbyDataProvider.java` implements `ExternalRugbyDataProvider`
3. YAML rule files in `ingestion/{name}/competitions/*/`
No changes to the admin services or controller.

---

## Auth Model

Security is **profile-conditional** (`SecurityConfig`):

| Profile | Behaviour |
|---|---|
| `dev` | **All requests permitted, no JWT required** — `devSecurityFilterChain` bean |
| any other | JWT (ES256, Supabase JWKS) required for authenticated routes |

When auth is enforced (non-dev), route rules:

| Pattern | Auth required |
|---|---|
| `/api/v1/health`, `/api/v1/matches/**`, `/api/v1/competitions/**`, `/api/v1/teams/**`, `/actuator/health/**`, `/actuator/info` | Public |
| `/api/v1/me`, `/api/v1/favourites/**`, `/api/v1/admin/**` | Authenticated JWT |

> **Note:** `/api/v1/me` uses `@AuthenticationPrincipal Jwt jwt` and calls `jwt.getSubject()` — it will NPE in the `dev` profile if called without a Bearer token, even though auth is disabled globally. This is expected.

User identity is always: `UUID.fromString(jwt.getSubject())`

---

## Developer Workflows

```bash
# Run locally (dev profile — no JWT required)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
./mvnw test

# Build JAR
./mvnw clean package -DskipTests

# Docker Compose (requires .env or exported vars)
docker-compose up --build
```

Required env vars for non-dev: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SUPABASE_JWK_SET_URI`, `API_SPORTS_API_KEY`, `CORS_ALLOWED_ORIGINS`.

---

## Database / Migrations

- Flyway auto-runs on startup; files in `src/main/resources/db/migration/`
- Naming convention: `V{n}__{description}.sql`
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate **never** alters schema; all changes must be a new migration file
- Supabase RLS policies are applied in `V2__secure_user_profiles.sql` — new tables with user data need corresponding RLS policies

---

## Test Patterns

- Controller tests: `@WebMvcTest(XController.class)` + `@Import(SecurityConfig.class)`
- Use `@MockitoBean` (not `@MockBean`) for service dependencies
- Authenticated requests: `SecurityMockMvcRequestPostProcessors.jwt()`
- `application-test.properties` disables the real DB and Flyway entirely; use Testcontainers when DB access is needed
- See `UserControllerTest` and `HealthControllerTest` as reference examples

---

## External Integrations

- **Supabase** — PostgreSQL (via HikariCP) + JWKS endpoint for JWT verification
- **API-Sports** (`https://v1.rugby.api-sports.io`) — authenticated via `X-RapidAPI-Key` header; client in `provider/apisports/ApiSportsClient.java`; ingestion triggered via admin endpoints

---

## Key Files

- `SecurityConfig.java` — read before adding any endpoint
- `ExternalRugbyDataProvider.java` — implement this interface to add a new data provider
- `ApiSportsRugbyDataProvider.java` — reference thin-adapter implementation
- `ExternalDataRuleService.java` — YAML rule loading; `ingestion/api-sports/` resource files
- `AdminExternalDataController.java` — `/api/v1/admin/external-data/*` endpoints
- `AdminCountryDataService.java` — country refresh flow
- `AdminCompetitionDataService.java` — competition + season refresh flow
- `AdminTeamDataService.java` — team refresh flow
- `V3__rugby_core_ingestion.sql` — full domain schema
- `UserController.java` + `UserService.java` — canonical controller/service pattern




# RugbyLive Project Blueprint

## 1. Executive Summary

RugbyLive is a modern rugby union live-score and fixtures platform designed for fans who want fast access to rugby scores, fixtures, competitions, teams, and eventually richer match/player statistics.

The project is both a real product and a senior-engineering portfolio project. It should demonstrate strong backend architecture, clean mobile/web UX, secure authentication, production-grade CI/CD, infrastructure-as-code, data ingestion, and long-term scalability.

### Target Audience

- Rugby fans who want quick access to fixtures, results, and live scores.
- Users who want to follow favourite teams and competitions.
- Future premium users who may want deeper stats, historical insights, notifications, and analytics.

### North Star Goal

Build a polished rugby scores platform where users can open the app and instantly see relevant rugby fixtures, scores, and team/competition information, with optional login for personalisation.

---

## 2. Architecture & Monorepo Map

### Monorepo Structure

```text
rugbylive/
  apps/
    api/
      rugbylive/
        src/
          main/
            java/dev/jordanvoss/rugbylive/
            resources/
              db/migration/
              application.properties
          test/
        Dockerfile
        pom.xml

    app/
      rugbylive/
        app/
        src/
        assets/
        package.json
        package-lock.json
        app.json

  infra/
    terraform/
      main.tf
      variables.tf
      outputs.tf

  .github/
    workflows/
      ci.yml
      api-deploy.yml
      web-deploy.yml
````

---

## 3. Backend: `/apps/api/rugbylive`

### Technology

* Java 25
* Spring Boot 4
* Spring Web MVC
* Spring Security
* Spring OAuth2 Resource Server
* Spring Data JPA
* Hibernate
* PostgreSQL
* Flyway
* Supabase Postgres
* Docker
* API-SPORTS Rugby API for ingestion

### Package Direction

Use domain-first packages:

```text
dev.jordanvoss.rugbylive/
  config/
  user/
  country/
  competition/
  team/
  mapping/
  provider/
    apisports/
  ingestion/
```

### Backend Responsibilities

The API owns:

* User profile lifecycle
* Supabase JWT validation
* Canonical rugby domain data
* External provider ingestion
* Mapping provider IDs to internal IDs
* Public read endpoints
* Authenticated user endpoints
* Admin ingestion endpoints

The API should not rely on the frontend for business rules.

---

## 4. Frontend: `/apps/app/rugbylive`

### Technology

* Expo SDK 55
* React Native
* Expo Router
* TypeScript
* Supabase JS client
* Web export via GitHub Pages
* iOS development build via EAS

### Routing

Expo Router is used with root `app/`, not `src/app`.

```text
apps/app/rugbylive/
  app/
    _layout.tsx
    index.tsx
    (auth)/
    (tabs)/
  src/
    components/
    features/
    lib/
    theme/
  assets/
    brand/
    images/
```

### Frontend Responsibilities

The app owns:

* User authentication UI
* Supabase login/signup
* Sending JWT access token to Spring API
* Public scores/fixtures browsing
* Optional personalised experience when logged in
* Light/dark themed UI using defined theme tokens

---

## 5. Inter-Service Communication

### Auth Flow

```text
User → Expo app → Supabase Auth
Supabase returns access token
Expo app calls Spring API with Authorization: Bearer <token>
Spring validates JWT via Supabase JWKS
Spring returns user/app data
```

### API Communication

* REST over HTTPS
* Base production API URL:

```text
https://api.rugbylive.jordanvoss.dev/api/v1
```

### Current Important Endpoints

```text
GET  /api/v1/health
GET  /api/v1/me
GET  /api/v1/competitions

# preview (commit=false, default=true)
POST /api/v1/admin/external-data/countries?providers=api-sports&commit=false
POST /api/v1/admin/external-data/competitions/{competitionKey}/seasons/{seasonYear}?providers=api-sports&commit=false
POST /api/v1/admin/external-data/competitions/{competitionKey}/seasons/{seasonYear}/teams?providers=api-sports&commit=false

# commit
POST /api/v1/admin/external-data/countries?providers=api-sports&commit=true
POST /api/v1/admin/external-data/competitions/united-rugby-championship/seasons/2024?providers=api-sports&commit=true
POST /api/v1/admin/external-data/competitions/united-rugby-championship/seasons/2024/teams?providers=api-sports&commit=true
```

### Refresh flow order (for a new competition)

```
1. POST /countries?commit=true            — seed canonical countries + external_mappings
2. POST /competitions/{key}/seasons/{year}?commit=true   — seed competition + season
3. POST /competitions/{key}/seasons/{year}/teams?commit=true — seed teams + competition_season_teams
```

### Ingestion preview/commit mode

Every `/admin/external-data/*` endpoint accepts `?commit=false` (preview) or `?commit=true` (write to DB).

- `commit=false` — fetches from provider, resolves identity, returns `ExternalDataResult` showing what *would* happen. **Nothing is written to DB.**
- `commit=true` — same resolve path, then writes canonical entities + `external_mappings`. Default to `false` when testing a new league/provider.

Response shape (`ExternalDataResult` — single provider):
```json
{
  "provider": "api-sports",
  "entityType": "TEAM",
  "commit": false,
  "summary": { "total": 16, "existingMappings": 0, "matchedByCanonicalKey": 0, "wouldCreate": 16, "written": 0, "unresolved": 0 },
  "items": [
    { "entityType": "TEAM", "externalId": "123", "providerName": "Leinster Rugby",
      "resolvedName": "Leinster Rugby", "proposedCanonicalKey": "leinster-rugby-ie",
      "proposedCountryKey": "ie", "decision": "NEW", "matchedInternalId": null, "written": false }
  ]
}
```

`decision` values: `EXISTING_EXTERNAL_MAPPING` · `MATCHED_BY_CANONICAL_KEY` · `MATCHED_BY_ALIAS` · `OVERRIDDEN_PROVIDER_DATA` · `NEW` · `UNRESOLVED` · `CONFLICT`

Response shape (`MultiProviderDataResult` — multiple providers):
```json
{
  "entityType": "COUNTRY",
  "commit": false,
  "byProvider": {
    "api-sports": { ... ExternalDataResult ... },
    "sportmonks": { ... ExternalDataResult ... }
  }
}
```

Single provider → returns `ExternalDataResult` directly.
Multiple providers (or no `providers` param) → returns `MultiProviderDataResult`.

### Providers parameter

```
?providers=api-sports            → single provider
?providers=api-sports,sportmonks → multiple
(omit)                           → all applicable providers
?providers=all                   → all applicable providers
```

---

## 6. Infrastructure & DevOps Ledger

### Production API

Hosted on AWS EC2.

```text
api.rugbylive.jordanvoss.dev
  → DNS A record
  → EC2 Elastic IP
  → Caddy reverse proxy
  → Docker container: rugbylive-api
```

### Reverse Proxy

Caddy is used for:

* HTTPS
* Let’s Encrypt certificates
* HTTP → HTTPS redirects
* Reverse proxy to Spring API

Caddyfile:

```caddy
api.rugbylive.jordanvoss.dev {
    reverse_proxy rugbylive-api:8080
}
```

### Docker Compose

Generated by GitHub Actions during deployment.

Services:

```text
rugbylive-api
rugbylive-caddy
```

The API image is pushed to AWS ECR.

### Terraform

Terraform manages:

* EC2 instance
* Security group
* IAM role
* IAM instance profile
* EC2 key pair
* Elastic IP
* ECR repository

Terraform state is stored in S3:

```text
s3://rugbylive-terraform-state/api/terraform.tfstate
```

### GitHub Actions

#### `ci.yml`

Runs on pull requests.

Responsibilities:

* API tests
* App install
* App typecheck
* App lint

#### `api-deploy.yml`

Runs on push to main/master when API or infra changes.

Responsibilities:

* Test API
* Terraform apply
* Build Docker image
* Push image to ECR
* SSH deploy to EC2
* Recreate Docker containers
* Verify production health endpoint

#### `web-deploy.yml`

Runs on push to main/master when app changes.

Responsibilities:

* Install app dependencies
* Typecheck
* Lint
* Export Expo web app
* Deploy to GitHub Pages

### Production Domains

```text
rugbylive.jordanvoss.dev        → web app
api.rugbylive.jordanvoss.dev    → API
```

---

## 7. Environment Variables

### API

Required in production:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
SUPABASE_PROJECT_URL
CORS_ALLOWED_ORIGINS
API_SPORTS_KEY
```

Spring properties:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

supabase.jwk-set-uri=${SUPABASE_PROJECT_URL}/auth/v1/.well-known/jwks.json

cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:8081,http://192.168.0.161:8081}

api-sports.base-url=${API_SPORTS_BASE_URL:https://v1.rugby.api-sports.io}
api-sports.api-key=${API_SPORTS_KEY:}
```

### App

Required:

```text
EXPO_PUBLIC_SUPABASE_URL
EXPO_PUBLIC_SUPABASE_ANON_KEY
EXPO_PUBLIC_API_BASE_URL
```

Production API value:

```text
EXPO_PUBLIC_API_BASE_URL=https://api.rugbylive.jordanvoss.dev/api/v1
```

### Rules

* Never commit `.env`
* Never commit `application-local.properties`
* Never hardcode API keys
* GitHub secrets are the production source of truth
* Local development uses ignored local config files

---

## 8. Security Protocol

### Authentication

Supabase handles identity/authentication.

The frontend logs in directly with Supabase and receives a JWT.

The Spring API validates Supabase JWTs using JWKS:

```text
https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
```

JWT algorithm is ES256.

### API Security

Current intended access model:

```text
Public:
  GET /api/v1/health
  GET /api/v1/competitions
  GET /api/v1/matches/**
  GET /api/v1/teams/**

Authenticated:
  GET /api/v1/me
  /api/v1/favourites/**

Admin:
  /api/v1/admin/**
```

Admin endpoints are temporarily allowed for authenticated users during ingestion development. Long term they must require admin claims.

### CORS

CORS must be driven by `CORS_ALLOWED_ORIGINS`.

Never hardcode only local origins in production code.

Required production origin:

```text
https://rugbylive.jordanvoss.dev
```

### Supabase RLS

RLS should be enabled on user-owned tables.

User profile data should be linked to Supabase Auth user ID.

The Spring API remains the primary access boundary for domain tables.

### Data Privacy

* Do not store unnecessary personal data.
* Do not request DOB unless there is a clear legal/product reason.
* Store only app-specific user profile fields in `user_profiles`.
* Supabase manages passwords and OAuth.
* Secrets must be rotated if pasted into logs/chats.

---

## 9. Decision Log

### Decision: Use Spring Boot for backend

Why:

* Strong CV/senior-engineering signal
* Aligns with professional Java experience
* Good for REST APIs, auth, data ingestion, transactions, and architecture

### Decision: Use Expo React Native for app

Why:

* Single codebase for iOS and web
* Fast iteration
* Existing user preference and experience
* Supports future App Store deployment

### Decision: Use Supabase for auth

Why:

* Handles email/password and OAuth
* Avoids building identity management manually
* JWTs integrate cleanly with Spring Security

### Decision: App talks directly to Supabase only for auth

Why:

* Supabase is the identity provider
* Spring API owns domain/business data
* Avoids exposing domain tables directly to frontend

### Decision: `/me` is authenticated but app is not login-only

Why:

* Scores and basic details should be public
* Logged-in users get favourites/preferences later
* Guest mode is important for a sports app

### Decision: Use EC2 + Docker + Caddy initially

Why:

* Simple, understandable infra
* Strong portfolio signal
* Cheaper and easier than ECS/Kubernetes for MVP
* Caddy gives automatic HTTPS

### Decision: Use Terraform

Why:

* Infrastructure should be reproducible
* Avoid manual AWS drift
* Demonstrates IaC competency

### Decision: Use ECR image deploy instead of copying source to EC2

Why:

* Builds are reproducible
* EC2 should pull a release artifact
* Rollback is easier
* More professional than building source on the server

### Decision: Use GitHub Pages for web

Why:

* Free and simple static hosting
* Good enough for Expo web MVP
* Custom domain supported

### Decision: Use API-SPORTS first

Why:

* Provides rugby countries, leagues, teams, seasons, games
* Good enough to bootstrap data
* Start with one provider before adding more

### Decision: Do manual ingestion before scheduled jobs

Why:

* Scheduled jobs hide failures
* Manual ingestion proves provider → canonical DB → API → UI flow
* Scheduling is easy after ingestion is reliable

### Decision: Competitions use identity + season branding model

Why:

* Competitions change names over time
* Same historical lineage should remain grouped
* Season-specific display names avoid awkward transition tables

Example:

```text
competition:
  current_name = United Rugby Championship

competition_seasons:
  2020 display_name = Pro14
  2024 display_name = United Rugby Championship
```

### Decision: Store provider mappings separately

Why:

* Internal IDs must remain stable
* Multiple APIs can map to the same canonical entity
* Provider IDs should not leak into domain design

---

## 10. Current Data Model Direction

### Core Tables

```text
countries
competitions
competition_seasons
competition_aliases
competition_countries
teams
stadiums
team_stadiums
competition_season_teams
external_mappings
image_hashes
```

### Key Concepts

`competitions` is the long-lived concept.

`competition_seasons` stores season-specific:

* display name
* short name
* logo
* start/end dates
* current flag

`external_mappings` maps provider IDs to internal IDs.

`competition_countries` allows competitions like URC to appear under Ireland, Scotland, Wales, Italy, and South Africa even if provider country is “World”.

---

## 11. Master Plan

## Phase 1: Immediate

Goal: Finish the first useful data and UI slice.

### Backend

* Finalise V3 rugby core ingestion migration
* Implement API-SPORTS countries ingestion
* Implement API-SPORTS competition ingestion for URC
* Implement API-SPORTS teams ingestion for URC
* Add `GET /api/v1/competitions`
* Add `GET /api/v1/teams?competitionId=&season=`
* Add `GET /api/v1/matches?date=` once fixtures are added
* Add simple admin protection strategy

### Frontend

* Replace temporary signed-in `/me` debug screen
* Add tabs structure
* Add Matches tab shell
* Add Competitions tab
* Add loading/error/empty states
* Ensure web and iPhone dev build both call production API correctly

### UX Polish

* Use RugbyLive brand assets from `assets/brand`
* Use defined theme tokens
* Avoid raw JSON UI except temporary debug screens
* Build clean mobile-first cards

---

## Phase 2: Functionality

Goal: Make the app useful for real rugby browsing.

### Backend

* Ingest fixtures/games from API-SPORTS
* Model matches
* Model teams in seasons
* Model match status
* Add fixture/date queries
* Add team detail endpoint
* Add competition detail endpoint
* Add favourites tables
* Add onboarding profile update endpoint

### Frontend

* Home screen grouped by country/region/competition
* Match list by date
* Competition page
* Team page
* Basic favourite team/competition flow
* Logged-in user preferences
* Better auth/session handling

---

## Phase 3: Scaling & Optimization

Goal: Improve reliability and data quality.

### Backend

* Add scheduled ingestion jobs
* Add provider request logging
* Add retry strategy
* Add ingestion run history table
* Add provider error table
* Add stale-data detection
* Add rate-limit protection
* Add caching where useful
* Add indexes for match/date queries
* Add DTO-only API responses
* Add structured logging

### Frontend

* Add better skeleton loading states
* Add offline/error fallback states
* Add performance optimisations for long lists
* Add analytics events
* Add crash/error reporting

---

## Phase 4: Launch & Maintenance

Goal: Prepare for public release.

### Deployment

* Harden SSH access
* Consider SSM or self-hosted runner instead of public SSH
* Add uptime monitoring
* Add alerting
* Add database backup strategy
* Add production/staging separation
* Add preview web deployments

### Security

* Add admin custom claims
* Restrict admin endpoints
* Review RLS policies
* Rotate secrets
* Remove local origins from production CORS
* Add security headers if needed

### Product

* Add privacy policy
* Add terms page if needed
* Prepare App Store assets
* Configure production Supabase email confirmation redirects
* Add password reset flow

---

## 12. Coding Standards for AI Agents

### General

* Do not introduce new frameworks without explicit approval.
* Prefer simple, maintainable code over clever abstractions.
* Keep domain concepts explicit.
* Do not hardcode secrets.
* Do not remove existing CI/CD without explanation.
* Do not regress routing back to `src/app`; Expo routes live in root `app/`.

### Backend Standards

* Use DTOs for all API responses.
* Do not expose JPA entities directly from controllers long term.
* Use services for business logic.
* Use repositories only for persistence.
* Use Flyway for all schema changes.
* Never edit old migrations that have already run in shared/prod DB.
* Add a new migration for schema changes.
* Keep provider DTOs separate from domain entities.
* Provider DTOs should match external API shape.
* Domain tables should represent RugbyLive concepts, not provider concepts.
* Use `external_mappings` for provider IDs.
* Use transactions around ingestion writes.
* Public endpoints should be explicitly permitted in `SecurityConfig`.
* Authenticated endpoints must use JWT context.
* Admin endpoints must eventually require admin claims.

### Frontend Standards

* Use functional components with hooks.
* Use TypeScript.
* Use Expo Router file-based routing.
* Use the existing theme tokens.
* Do not hardcode colours directly in screens unless adding a token first.
* Put reusable UI in `src/components`.
* Put feature-specific logic in `src/features`.
* Put API/Supabase clients in `src/lib`.
* Avoid dumping raw JSON in final UI.
* Always handle loading, error, and empty states.
* Keep web compatibility in mind.

### Styling Standards

* Use clean, modern, minimal design.
* Respect light/dark theme structure.
* Keep rugby branding consistent.
* Brand assets go in:

```text
assets/brand
```

General images go in:

```text
assets/images
```

### CI/CD Standards

* `ci.yml` is for pull request checks.
* `api-deploy.yml` owns API deployment.
* `web-deploy.yml` owns web deployment.
* API deploy must verify `/api/v1/health`.
* Web deploy must typecheck and lint before export.
* Do not run duplicate tests unnecessarily on the same push.
* Deployment workflows should be split into logical jobs/steps.

### Database Standards

* Use snake_case table/column names.
* Use `BIGSERIAL` for internal numeric IDs unless UUID is needed.
* Use stable `canonical_key` fields for public/domain identity.
* Store provider IDs only in `external_mappings`.
* Use join tables for many-to-many relationships.
* Prefer flexible metadata columns only where useful, not everywhere.
* Add indexes when adding query patterns.

---

## 13. Known Current State

Working:

* Expo app runs on iPhone dev build
* Expo web deploys
* Supabase auth works
* Spring API validates Supabase JWT
* `/me` returns user profile context
* API deploys to AWS EC2
* Caddy HTTPS works
* API health endpoint is public
* GitHub Actions API deployment works
* GitHub Actions web deployment works
* CORS is environment-driven

In progress:

* First rugby data ingestion
* API-SPORTS integration
* Competitions/teams/countries canonical model

Not started:

* Fixtures/matches ingestion
* Live scores
* Favourite teams/competitions
* Onboarding
* Admin custom claims
* Scheduled jobs
* Monitoring/alerts

---

## 14. Important Warnings for AI Agents

Do not:

* Replace Supabase auth with custom auth.
* Put domain reads directly against Supabase from the app.
* Use provider IDs as primary domain IDs.
* Revert API deployment to copying source to EC2.
* Revert Expo Router back to `src/app`.
* Hardcode production CORS origins in Java.
* Skip Flyway for DB changes.
* Add scheduled ingestion before manual ingestion works.
* Model competition name changes as separate competitions unless the sporting lineage truly changed.
* Ask for DOB during signup without a clear reason.
* Commit secrets, `.env`, or `application-local.properties`.

Always preserve:

* Spring API as domain boundary.
* Supabase as auth provider.
* Terraform + ECR + EC2 + Caddy production deployment.
* GitHub Pages web deployment.
* Competition identity + season display-name model.
* External mappings for API providers.
