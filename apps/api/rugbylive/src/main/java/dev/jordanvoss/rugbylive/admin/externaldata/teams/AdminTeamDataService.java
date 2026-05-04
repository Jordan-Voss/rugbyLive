package dev.jordanvoss.rugbylive.admin.externaldata.teams;
import dev.jordanvoss.rugbylive.admin.externaldata.resolution.*;
import dev.jordanvoss.rugbylive.admin.externaldata.rules.*;
import dev.jordanvoss.rugbylive.competition.*;
import dev.jordanvoss.rugbylive.country.Country;
import dev.jordanvoss.rugbylive.country.CountryRepository;
import dev.jordanvoss.rugbylive.mapping.ExternalMapping;
import dev.jordanvoss.rugbylive.mapping.ExternalMappingRepository;
import dev.jordanvoss.rugbylive.provider.*;
import dev.jordanvoss.rugbylive.team.Team;
import dev.jordanvoss.rugbylive.team.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;
/**
 * RugbyLive team refresh flow for a competition season.
 *
 * Precondition: the competition and season must already exist in DB
 * (run AdminCompetitionDataService.refreshSeason first).
 *
 * Writes: teams, external_mappings for TEAM, competition_season_teams.
 */
@Slf4j
@Service
public class AdminTeamDataService {
    private final Map<String, ExternalRugbyDataProvider> providers;
    private final ExternalDataRuleService rules;
    private final TeamRepository teamRepository;
    private final CountryRepository countryRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionSeasonRepository competitionSeasonRepository;
    private final ExternalMappingRepository mappingRepository;
    private final JdbcTemplate jdbcTemplate;
    public AdminTeamDataService(
            List<ExternalRugbyDataProvider> providers, ExternalDataRuleService rules,
            TeamRepository teamRepository, CountryRepository countryRepository,
            CompetitionRepository competitionRepository,
            CompetitionSeasonRepository competitionSeasonRepository,
            ExternalMappingRepository mappingRepository, JdbcTemplate jdbcTemplate) {
        this.providers = providers.stream().collect(
                Collectors.toMap(ExternalRugbyDataProvider::providerKey, p -> p));
        this.rules = rules;
        this.teamRepository = teamRepository;
        this.countryRepository = countryRepository;
        this.competitionRepository = competitionRepository;
        this.competitionSeasonRepository = competitionSeasonRepository;
        this.mappingRepository = mappingRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
    @Transactional
    public Map<String, ExternalDataResult> refreshTeams(
            String competitionKey, int seasonYear, List<String> providerKeys, boolean commit) {
        // Commit mode requires competition + season to exist in DB (data integrity).
        // Preview mode can proceed using YAML-only resolution — nothing is written anyway.
        Competition competition = competitionRepository.findByCanonicalKey(competitionKey).orElse(null);
        if (commit && competition == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Competition '" + competitionKey + "' not found. Run competition refresh with commit=true first.");
        CompetitionSeason season = competition == null ? null
                : competitionSeasonRepository.findByCompetitionIdAndSeasonYear(competition.getId(), seasonYear).orElse(null);
        if (commit && season == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Season " + seasonYear + " for '" + competitionKey + "' not found. Run competition season refresh with commit=true first.");
        List<ExternalRugbyDataProvider> selected = selectProviders(providerKeys);
        if (selected.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No providers available.");
        Map<String, ExternalDataResult> results = new LinkedHashMap<>();
        for (ExternalRugbyDataProvider provider : selected) {
            // Resolve external competition ID: prefer DB mapping, fall back to YAML rule.
            String externalCompId = null;
            if (competition != null) {
                externalCompId = mappingRepository.findByProviderAndEntityTypeAndInternalId(
                        provider.providerKey(), "COMPETITION", competition.getId().toString())
                        .map(ExternalMapping::getExternalId).orElse(null);
            }
            if (externalCompId == null) {
                externalCompId = rules.competitionByCanonicalKey(provider.providerKey(), competitionKey)
                        .map(CompetitionProviderRule::getExternalId).orElse(null);
            }
            if (externalCompId == null) {
                log.warn("No external ID found for competition '{}' / provider '{}' — skipping", competitionKey, provider.providerKey());
                results.put(provider.providerKey(), ExternalDataResult.empty(provider.providerKey(), "TEAM", commit));
                continue;
            }
            results.put(provider.providerKey(),
                    refreshTeamsFromProvider(provider, externalCompId, seasonYear, season, commit));
        }
        return results;
    }
    private ExternalDataResult refreshTeamsFromProvider(ExternalRugbyDataProvider provider,
            String externalCompId, int seasonYear, CompetitionSeason season, boolean commit) {
        List<ExternalTeamRecord> records = provider.fetchTeams(externalCompId, seasonYear);
        if (records.isEmpty()) return ExternalDataResult.empty(provider.providerKey(), "TEAM", commit);
        List<ExternalDataItem> items = records.stream()
                .map(r -> resolveTeam(r, season, provider.providerKey(), commit))
                .toList();
        return new ExternalDataResult(provider.providerKey(), "TEAM", commit,
                ExternalDataSummary.from(items), items);
    }
    private ExternalDataItem resolveTeam(ExternalTeamRecord record, CompetitionSeason season,
                                          String provider, boolean commit) {
        Optional<TeamProviderRule> rule = rules.team(provider, record.externalId());
        String resolvedName = rule.map(TeamProviderRule::getName).filter(n -> n != null && !n.isBlank())
                .orElse(record.name());
        String proposedKey = rule.map(TeamProviderRule::getCanonicalKey).filter(k -> k != null && !k.isBlank())
                .orElse(slug(resolvedName) + (record.countryCode() != null ? "-" + record.countryCode().toLowerCase() : ""));
        String proposedCountryKey = rule.map(TeamProviderRule::getCountryKey).orElse(
                record.countryCode() != null ? record.countryCode().toLowerCase() : null);
        // Step 1: existing external mapping
        Optional<ExternalMapping> existing = mappingRepository
                .findByProviderAndEntityTypeAndExternalId(provider, "TEAM", record.externalId());
        if (existing.isPresent()) {
            Team team = teamRepository.findById(Long.parseLong(existing.get().getInternalId())).orElseThrow();
            if (commit) { applyTeamFields(team, record, rule.orElse(null), proposedKey, proposedCountryKey); teamRepository.save(team); upsertTeamMapping(record, team.getId().toString(), provider, "provider"); linkToSeason(team.getId(), season.getId()); }
            return teamItem(record, resolvedName, proposedKey, proposedCountryKey,
                    ExternalDataDecision.EXISTING_PROVIDER_MAPPING, team.getId(), team.getCanonicalKey(), commit);
        }
        // Step 2: canonical key match
        Optional<Team> byKey = teamRepository.findByCanonicalKey(proposedKey);
        if (byKey.isPresent()) {
            Team team = byKey.get();
            if (commit) { applyTeamFields(team, record, rule.orElse(null), proposedKey, proposedCountryKey); teamRepository.save(team); upsertTeamMapping(record, team.getId().toString(), provider, "name_match"); linkToSeason(team.getId(), season.getId()); }
            return teamItem(record, resolvedName, proposedKey, proposedCountryKey,
                    ExternalDataDecision.MATCHED_CANONICAL_KEY, team.getId(), team.getCanonicalKey(), commit);
        }
        // Step 3: new entity
        if (commit) {
            Team team = new Team();
            applyTeamFields(team, record, rule.orElse(null), proposedKey, proposedCountryKey);
            team = teamRepository.save(team);
            upsertTeamMapping(record, team.getId().toString(), provider, rule.isPresent() ? "confirmed" : "provider");
            linkToSeason(team.getId(), season.getId());
            return teamItem(record, resolvedName, proposedKey, proposedCountryKey,
                    ExternalDataDecision.NEW_ENTITY, team.getId(), team.getCanonicalKey(), true);
        }
        return teamItem(record, resolvedName, proposedKey, proposedCountryKey,
                ExternalDataDecision.NEW_ENTITY, null, null, false);
    }
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private void applyTeamFields(Team team, ExternalTeamRecord r, TeamProviderRule rule,
                                   String canonicalKey, String countryKey) {
        team.setCanonicalKey(canonicalKey);
        team.setName(rule != null && rule.getName() != null ? rule.getName() : r.name());
        team.setLogoUrl(r.logoUrl());
        team.setLogoSource(r.provider());
        team.setNational(Boolean.TRUE.equals(r.national()));
        team.setFounded(r.founded());
        String resolvedCountryKey = countryKey != null ? countryKey
                : (r.countryCode() != null ? r.countryCode().toLowerCase() : null);
        if (resolvedCountryKey != null) {
            countryRepository.findByCanonicalKey(resolvedCountryKey)
                    .ifPresent(team::setCountry);
        }
    }
    private void upsertTeamMapping(ExternalTeamRecord r, String internalId, String provider, String confidence) {
        ExternalMapping m = mappingRepository.findByProviderAndEntityTypeAndExternalId(provider, "TEAM", r.externalId())
                .orElseGet(ExternalMapping::new);
        m.setProvider(provider); m.setEntityType("TEAM"); m.setInternalId(internalId);
        m.setExternalId(r.externalId()); m.setExternalName(r.name()); m.setActive(true);
        if (m.getConfidence() == null || rank(confidence) > rank(m.getConfidence())) m.setConfidence(confidence);
        mappingRepository.save(m);
    }
    private void linkToSeason(Long teamId, Long seasonId) {
        jdbcTemplate.update("INSERT INTO competition_season_teams (competition_season_id, team_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                seasonId, teamId);
    }
    private static ExternalDataItem teamItem(ExternalTeamRecord r, String resolvedName,
            String canonicalKey, String countryKey, ExternalDataDecision decision,
            Long internalId, String existingCanonicalKey, boolean written) {
        return new ExternalDataItem("TEAM", r.externalId(), r.name(), resolvedName,
                canonicalKey, countryKey, null, decision, internalId, existingCanonicalKey, written, List.of());
    }
    private List<ExternalRugbyDataProvider> selectProviders(List<String> keys) {
        if (keys == null || keys.isEmpty() || keys.contains("all")) return new ArrayList<>(providers.values());
        return keys.stream().map(k -> { ExternalRugbyDataProvider p = providers.get(k); if (p == null) throw new IllegalArgumentException("Unknown provider: " + k); return p; }).toList();
    }
    static String slug(String v) { return v == null ? "unknown" : v.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); }
    static int rank(String c) { return switch (c) { case "confirmed" -> 3; case "provider" -> 2; case "name_match" -> 1; default -> 0; }; }
}
