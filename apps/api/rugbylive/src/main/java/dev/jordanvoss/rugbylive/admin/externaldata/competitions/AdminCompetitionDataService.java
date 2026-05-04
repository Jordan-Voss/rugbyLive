package dev.jordanvoss.rugbylive.admin.externaldata.competitions;
import dev.jordanvoss.rugbylive.admin.externaldata.resolution.*;
import dev.jordanvoss.rugbylive.admin.externaldata.rules.*;
import dev.jordanvoss.rugbylive.competition.*;
import dev.jordanvoss.rugbylive.country.Country;
import dev.jordanvoss.rugbylive.country.CountryRepository;
import dev.jordanvoss.rugbylive.mapping.ExternalMapping;
import dev.jordanvoss.rugbylive.mapping.ExternalMappingRepository;
import dev.jordanvoss.rugbylive.provider.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;
/**
 * RugbyLive competition + season refresh flow.
 *
 * Writes: competitions, competition_seasons, competition_aliases,
 *         competition_countries, external_mappings.
 * Does NOT write teams — see AdminTeamDataService.
 */
@Slf4j
@Service
public class AdminCompetitionDataService {
    private final Map<String, ExternalRugbyDataProvider> providers;
    private final ExternalDataRuleService rules;
    private final CompetitionRepository competitionRepository;
    private final CompetitionSeasonRepository competitionSeasonRepository;
    private final CompetitionAliasRepository competitionAliasRepository;
    private final CountryRepository countryRepository;
    private final ExternalMappingRepository mappingRepository;
    private final JdbcTemplate jdbcTemplate;
    public AdminCompetitionDataService(
            List<ExternalRugbyDataProvider> providers, ExternalDataRuleService rules,
            CompetitionRepository competitionRepository,
            CompetitionSeasonRepository competitionSeasonRepository,
            CompetitionAliasRepository competitionAliasRepository,
            CountryRepository countryRepository,
            ExternalMappingRepository mappingRepository,
            JdbcTemplate jdbcTemplate) {
        this.providers = providers.stream().collect(
                Collectors.toMap(ExternalRugbyDataProvider::providerKey, p -> p));
        this.rules = rules;
        this.competitionRepository = competitionRepository;
        this.competitionSeasonRepository = competitionSeasonRepository;
        this.competitionAliasRepository = competitionAliasRepository;
        this.countryRepository = countryRepository;
        this.mappingRepository = mappingRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
    /**
     * Refresh a specific competition season from one or more providers.
     *
     * @param competitionKey RugbyLive canonical key (e.g. "united-rugby-championship")
     * @param seasonYear     four-digit season year
     * @param providerKeys   empty = all applicable providers
     * @param commit         false = preview only
     * @return per-provider results
     */
    @Transactional
    public Map<String, ExternalDataResult> refreshSeason(
            String competitionKey, int seasonYear, List<String> providerKeys, boolean commit) {
        List<ExternalRugbyDataProvider> selected = selectProvidersForCompetition(competitionKey, providerKeys);
        if (selected.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No provider has a rule for competition '" + competitionKey + "'.");
        Map<String, ExternalDataResult> results = new LinkedHashMap<>();
        for (ExternalRugbyDataProvider provider : selected) {
            results.put(provider.providerKey(),
                    refreshSeasonFromProvider(competitionKey, seasonYear, provider, commit));
        }
        return results;
    }
    private ExternalDataResult refreshSeasonFromProvider(
            String competitionKey, int seasonYear, ExternalRugbyDataProvider provider, boolean commit) {
        String externalId = resolveExternalId(competitionKey, provider.providerKey());
        Optional<ExternalCompetitionRecord> record = provider.fetchCompetition(externalId);
        if (record.isEmpty()) return ExternalDataResult.empty(provider.providerKey(), "COMPETITION", commit);
        ExternalCompetitionRecord comp = record.get();
        Optional<SeasonProviderRule> seasonRule = rules.season(provider.providerKey(), externalId, seasonYear);
        Optional<CompetitionProviderRule> compRule = rules.competition(provider.providerKey(), externalId);
        List<ExternalDataItem> items = new ArrayList<>();
        // --- competition entity ---
        String proposedCompKey = compRule.map(CompetitionProviderRule::getCanonicalKey)
                .orElse(slug(comp.name()));
        String resolvedCompName = compRule.map(CompetitionProviderRule::getCurrentName).orElse(comp.name());
        ExternalDataDecision compDecision = compRule.isPresent()
                ? ExternalDataDecision.APPLIED_PROVIDER_RULE
                : resolveCompetitionDecision(provider.providerKey(), externalId, proposedCompKey);
        Long competitionId = null;
        Competition competition = null;
        boolean compWritten = false;
        if (commit && compDecision != ExternalDataDecision.NEEDS_REVIEW && compDecision != ExternalDataDecision.CONFLICT) {
            competition = resolveOrCreateCompetition(provider.providerKey(), externalId, proposedCompKey);
            applyCompetitionFields(competition, comp, compRule.orElse(null), proposedCompKey);
            competition = competitionRepository.save(competition);
            competitionId = competition.getId();
            upsertMapping("COMPETITION", competitionId.toString(), externalId, comp.name(), provider.providerKey(), "provider");
            if (compRule.isPresent()) {
                registerAliases(competition, compRule.get().getAliases(), provider.providerKey(), comp);
                applyCompetitionCountries(competition, compRule.get().getParticipantCountryKeys());
            }
            compWritten = true;
        } else {
            Optional<ExternalMapping> existing = mappingRepository
                    .findByProviderAndEntityTypeAndExternalId(provider.providerKey(), "COMPETITION", externalId);
            if (existing.isPresent()) competitionId = Long.parseLong(existing.get().getInternalId());
            else competitionRepository.findByCanonicalKey(proposedCompKey).ifPresent(c -> {});
        }
        items.add(ExternalDataItem.of("COMPETITION", externalId, comp.name(), proposedCompKey,
                resolvedCompName, compDecision, competitionId,
                competitionId != null ? proposedCompKey : null, compWritten));
        // --- season entity ---
        Optional<ExternalSeasonRecord> seasonRecord = comp.seasons().stream()
                .filter(s -> s.year() == seasonYear).findFirst();
        if (seasonRecord.isEmpty()) {
            items.add(ExternalDataItem.needsReview("COMPETITION_SEASON",
                    externalId + ":" + seasonYear, comp.name() + " " + seasonYear,
                    proposedCompKey + "-" + seasonYear,
                    List.of("Season " + seasonYear + " not found in provider response for league " + externalId)));
            return new ExternalDataResult(provider.providerKey(), "COMPETITION", commit,
                    ExternalDataSummary.from(items), items);
        }
        ExternalSeasonRecord season = seasonRecord.get();
        String proposedSeasonKey = proposedCompKey + "-" + seasonYear;
        String resolvedSeasonName = seasonRule.map(SeasonProviderRule::getDisplayName)
                .orElseGet(() -> compRule.map(CompetitionProviderRule::getCurrentName).orElse(comp.name()));
        Long seasonId = null;
        boolean seasonWritten = false;
        ExternalDataDecision seasonDecision;
        Optional<CompetitionSeason> existingSeason = competitionId != null
                ? competitionSeasonRepository.findByCompetitionIdAndSeasonYear(competitionId, seasonYear)
                : Optional.empty();
        seasonDecision = existingSeason.isPresent()
                ? ExternalDataDecision.MATCHED_CANONICAL_KEY : ExternalDataDecision.NEW_ENTITY;
        if (commit && competitionId != null
                && seasonDecision != ExternalDataDecision.NEEDS_REVIEW
                && seasonDecision != ExternalDataDecision.CONFLICT) {
            CompetitionSeason cs = existingSeason.orElseGet(CompetitionSeason::new);
            cs.setCompetition(competition != null ? competition : competitionRepository.findById(competitionId).orElseThrow());
            cs.setSeasonYear(seasonYear);
            cs.setDisplayName(seasonRule.map(SeasonProviderRule::getDisplayName).orElseGet(() -> compRule.map(CompetitionProviderRule::getCurrentName).orElse(comp.name())));
            cs.setShortName(seasonRule.map(SeasonProviderRule::getShortName).orElseGet(() -> compRule.map(CompetitionProviderRule::getShortName).orElse(comp.name())));
            cs.setLogoUrl(comp.logoUrl());
            cs.setStartDate(season.startDate());
            cs.setEndDate(season.endDate());
            cs.setCurrent(Boolean.TRUE.equals(season.current()));
            cs = competitionSeasonRepository.save(cs);
            seasonId = cs.getId();
            upsertMapping("COMPETITION_SEASON", seasonId.toString(),
                    externalId + ":" + seasonYear, comp.name() + " " + seasonYear, provider.providerKey(), "provider");
            seasonWritten = true;
        }
        items.add(ExternalDataItem.of("COMPETITION_SEASON", externalId + ":" + seasonYear,
                comp.name() + " " + seasonYear, proposedSeasonKey,
                resolvedSeasonName, seasonDecision, seasonId,
                seasonId != null ? proposedSeasonKey : null, seasonWritten));
        return new ExternalDataResult(provider.providerKey(), "COMPETITION", commit,
                ExternalDataSummary.from(items), items);
    }
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private ExternalDataDecision resolveCompetitionDecision(String provider, String externalId, String proposedKey) {
        if (mappingRepository.findByProviderAndEntityTypeAndExternalId(provider, "COMPETITION", externalId).isPresent())
            return ExternalDataDecision.EXISTING_PROVIDER_MAPPING;
        if (competitionRepository.findByCanonicalKey(proposedKey).isPresent())
            return ExternalDataDecision.MATCHED_CANONICAL_KEY;
        return ExternalDataDecision.NEW_ENTITY;
    }
    private Competition resolveOrCreateCompetition(String provider, String externalId, String proposedKey) {
        return mappingRepository.findByProviderAndEntityTypeAndExternalId(provider, "COMPETITION", externalId)
                .flatMap(m -> competitionRepository.findById(Long.parseLong(m.getInternalId())))
                .or(() -> competitionRepository.findByCanonicalKey(proposedKey))
                .orElseGet(Competition::new);
    }
    private void applyCompetitionFields(Competition c, ExternalCompetitionRecord r,
                                         CompetitionProviderRule rule, String canonicalKey) {
        c.setCanonicalKey(canonicalKey);
        c.setCurrentName(rule != null ? rule.getCurrentName() : r.name());
        c.setType(r.type());
        c.setLogoUrl(r.logoUrl());
        c.setLogoSource(r.provider());
        c.setDisplayRegion(rule != null ? rule.getDisplayRegion() : null);
        c.setInternational(rule != null && rule.isInternational());
        c.setGender(rule != null ? rule.getGender() : "men");
        if (r.countryCode() != null) {
            rules.countryByProviderCode(r.provider(), r.countryCode())
                    .map(cr -> countryRepository.findByCanonicalKey(cr.getCanonicalKey()).orElse(null))
                    .ifPresent(c::setProviderCountry);
        }
    }
    private void registerAliases(Competition competition, List<String> aliases, String provider, ExternalCompetitionRecord rec) {
        Set<String> toRegister = new LinkedHashSet<>(aliases);
        toRegister.add(rec.name());
        for (String alias : toRegister) {
            String normalized = slug(alias);
            CompetitionAlias ca = competitionAliasRepository
                    .findByCompetitionIdAndNormalizedAndProvider(competition.getId(), normalized, provider)
                    .orElseGet(CompetitionAlias::new);
            ca.setCompetition(competition);
            ca.setAlias(alias);
            ca.setNormalized(normalized);
            ca.setProvider(provider);
            competitionAliasRepository.save(ca);
        }
    }
    private void applyCompetitionCountries(Competition competition, List<String> countryKeys) {
        for (String key : countryKeys) {
            Country country = countryRepository.findByCanonicalKey(key).orElseGet(() -> {
                Country c = new Country();
                c.setCanonicalKey(key);
                rules.countryByCanonicalKey("api-sports", key).ifPresentOrElse(
                        r -> { c.setName(r.getName()); c.setCode(r.getCode()); },
                        () -> c.setName(key));
                c.setFlagSource("manual");
                return countryRepository.save(c);
            });
            jdbcTemplate.update("INSERT INTO competition_countries (competition_id, country_id, role) VALUES (?, ?, 'participant') ON CONFLICT DO NOTHING",
                    competition.getId(), country.getId());
        }
    }
    private String resolveExternalId(String competitionKey, String provider) {
        Optional<Competition> comp = competitionRepository.findByCanonicalKey(competitionKey);
        if (comp.isPresent()) {
            Optional<ExternalMapping> m = mappingRepository.findByProviderAndEntityTypeAndInternalId(
                    provider, "COMPETITION", comp.get().getId().toString());
            if (m.isPresent()) return m.get().getExternalId();
        }
        return rules.competitionByCanonicalKey(provider, competitionKey)
                .map(CompetitionProviderRule::getExternalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No external ID found for competition '" + competitionKey + "' / provider '" + provider + "'"));
    }
    private void upsertMapping(String entityType, String internalId, String externalId,
                                String externalName, String provider, String confidence) {
        ExternalMapping m = mappingRepository.findByProviderAndEntityTypeAndExternalId(provider, entityType, externalId)
                .orElseGet(ExternalMapping::new);
        m.setProvider(provider); m.setEntityType(entityType); m.setInternalId(internalId);
        m.setExternalId(externalId); m.setExternalName(externalName); m.setActive(true);
        if (m.getConfidence() == null || rank(confidence) > rank(m.getConfidence())) m.setConfidence(confidence);
        mappingRepository.save(m);
    }
    private List<ExternalRugbyDataProvider> selectProvidersForCompetition(String competitionKey, List<String> keys) {
        List<String> covering = rules.providersForCompetition(competitionKey);
        if (keys == null || keys.isEmpty() || keys.contains("all"))
            return covering.stream().map(providers::get).filter(Objects::nonNull).toList();
        return keys.stream().filter(covering::contains).map(k -> {
            ExternalRugbyDataProvider p = providers.get(k);
            if (p == null) throw new IllegalArgumentException("Unknown provider: " + k);
            return p;
        }).toList();
    }
    static String slug(String v) { return v == null ? "unknown" : v.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); }
    static int rank(String c) { return switch (c) { case "confirmed" -> 3; case "provider" -> 2; case "name_match" -> 1; default -> 0; }; }
}
