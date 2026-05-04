package dev.jordanvoss.rugbylive.admin.externaldata.rules;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Loads YAML provider rules from classpath on startup.
 *
 * Rules define RugbyLive canonical values and provider-specific corrections
 * for competitions, teams, seasons, and countries.
 *
 * File layout:
 *   ingestion/{provider}/countries.yml
 *   ingestion/{provider}/competitions/{slug}/competition.yml
 *   ingestion/{provider}/competitions/{slug}/teams.yml
 *   ingestion/{provider}/competitions/{slug}/seasons.yml
 *
 * Restart to pick up YAML changes during development.
 */
@Slf4j
@Component
public class ExternalDataRuleService {

    private static final List<String> KNOWN_PROVIDERS = List.of("api-sports");

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ResourcePatternResolver resourceResolver;

    /** provider → (externalId → CompetitionProviderRule) */
    private final Map<String, Map<String, CompetitionProviderRule>> competitions = new HashMap<>();

    /** provider → (externalTeamId → TeamProviderRule) — aggregated across all competition files */
    private final Map<String, Map<String, TeamProviderRule>> teams = new HashMap<>();

    /** provider → (externalCompetitionId → (year → SeasonProviderRule)) */
    private final Map<String, Map<String, Map<Integer, SeasonProviderRule>>> seasons = new HashMap<>();

    /** provider → (canonicalKey → CountryProviderRule) */
    private final Map<String, Map<String, CountryProviderRule>> countries = new HashMap<>();

    public ExternalDataRuleService(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @PostConstruct
    public void load() {
        for (String provider : KNOWN_PROVIDERS) {
            loadCountries(provider);
            loadCompetitions(provider);
        }
    }

    // -------------------------------------------------------------------------
    // Lookups
    // -------------------------------------------------------------------------

    /** Competition rule by provider externalId. */
    public Optional<CompetitionProviderRule> competition(String provider, String externalId) {
        return Optional.ofNullable(competitions.getOrDefault(provider, Map.of()).get(externalId));
    }

    /** Competition rule for a canonical key — used to reverse-resolve the provider externalId. */
    public Optional<CompetitionProviderRule> competitionByCanonicalKey(String provider, String canonicalKey) {
        return competitions.getOrDefault(provider, Map.of()).values().stream()
                .filter(r -> canonicalKey.equals(r.getCanonicalKey()))
                .findFirst();
    }

    /**
     * All providers that have a rule for the given canonical competition key.
     * Used to fan-out ingestion across all providers at once.
     */
    public List<String> providersForCompetition(String canonicalKey) {
        return KNOWN_PROVIDERS.stream()
                .filter(p -> competitions.getOrDefault(p, Map.of()).values().stream()
                        .anyMatch(r -> canonicalKey.equals(r.getCanonicalKey())))
                .toList();
    }

    /** Team rule by provider externalId — aggregated across all competition team files. */
    public Optional<TeamProviderRule> team(String provider, String externalId) {
        return Optional.ofNullable(teams.getOrDefault(provider, Map.of()).get(externalId));
    }

    /** Season rule for a specific competition-year. */
    public Optional<SeasonProviderRule> season(String provider, String externalCompetitionId, int year) {
        return Optional.ofNullable(
                seasons.getOrDefault(provider, Map.of())
                       .getOrDefault(externalCompetitionId, Map.of())
                       .get(year));
    }

    /** Country rule by RugbyLive canonical key (e.g. "ireland"). */
    public Optional<CountryProviderRule> countryByCanonicalKey(String provider, String canonicalKey) {
        return Optional.ofNullable(countries.getOrDefault(provider, Map.of()).get(canonicalKey));
    }

    /** Country rule by provider code — case-insensitive (e.g. "IRE" → ireland). */
    public Optional<CountryProviderRule> countryByProviderCode(String provider, String code) {
        if (code == null) return Optional.empty();
        return countries.getOrDefault(provider, Map.of()).values().stream()
                .filter(r -> code.equalsIgnoreCase(r.getCode()))
                .findFirst();
    }

    // -------------------------------------------------------------------------
    // Loaders
    // -------------------------------------------------------------------------

    private void loadCountries(String provider) {
        String path = "classpath:ingestion/" + provider + "/countries.yml";
        try {
            Resource resource = resourceResolver.getResource(path);
            if (!resource.exists()) {
                log.debug("No country rules at {} — skipping", path);
                return;
            }
            CountryRuleFile file = yaml.readValue(resource.getInputStream(), CountryRuleFile.class);
            countries.put(provider, file.getItems());
            log.info("Loaded {} country rules for provider '{}'", file.getItems().size(), provider);
        } catch (IOException e) {
            log.error("Failed to load country rules for provider '{}': {}", provider, e.getMessage());
        }
    }

    private void loadCompetitions(String provider) {
        String pattern = "classpath:ingestion/" + provider + "/competitions/*/competition.yml";
        Resource[] files;
        try {
            files = resourceResolver.getResources(pattern);
        } catch (IOException e) {
            log.warn("Could not scan for competition rules ({}): {}", pattern, e.getMessage());
            return;
        }
        if (files.length == 0) {
            log.debug("No competition rules at {}", pattern);
            return;
        }
        competitions.putIfAbsent(provider, new LinkedHashMap<>());
        teams.putIfAbsent(provider, new LinkedHashMap<>());
        seasons.putIfAbsent(provider, new LinkedHashMap<>());
        for (Resource f : files) {
            loadCompetitionBundle(provider, f);
        }
    }

    private void loadCompetitionBundle(String provider, Resource competitionFile) {
        try {
            CompetitionProviderRule rule = yaml.readValue(
                    competitionFile.getInputStream(), CompetitionProviderRule.class);
            if (rule.getExternalId() == null || rule.getExternalId().isBlank()) {
                log.warn("competition.yml at {} has no externalId — skipping", competitionFile.getDescription());
                return;
            }
            competitions.get(provider).put(rule.getExternalId(), rule);
            log.info("Loaded competition rule '{}' (externalId={}) for provider '{}'",
                    rule.getCanonicalKey(), rule.getExternalId(), provider);

            Resource teamsFile = sibling(competitionFile, "teams.yml");
            if (teamsFile != null && teamsFile.exists()) {
                TeamRuleFile teamsData = yaml.readValue(teamsFile.getInputStream(), TeamRuleFile.class);
                teams.get(provider).putAll(teamsData.getItems());
                log.info("Loaded {} team rules for competition '{}' (provider '{}')",
                        teamsData.getItems().size(), rule.getCanonicalKey(), provider);
            }

            Resource seasonsFile = sibling(competitionFile, "seasons.yml");
            if (seasonsFile != null && seasonsFile.exists()) {
                SeasonRuleFile seasonsData = yaml.readValue(seasonsFile.getInputStream(), SeasonRuleFile.class);
                Map<Integer, SeasonProviderRule> byYear = new LinkedHashMap<>();
                for (Map.Entry<String, SeasonProviderRule> entry : seasonsData.getItems().entrySet()) {
                    try {
                        byYear.put(Integer.parseInt(entry.getKey()), entry.getValue());
                    } catch (NumberFormatException ignored) {
                        log.warn("Non-integer season key '{}' in seasons.yml for '{}' — skipping",
                                entry.getKey(), rule.getCanonicalKey());
                    }
                }
                seasons.get(provider).put(rule.getExternalId(), byYear);
                log.info("Loaded {} season rules for competition '{}' (provider '{}')",
                        byYear.size(), rule.getCanonicalKey(), provider);
            }
        } catch (IOException e) {
            log.error("Failed to load competition bundle from {}: {}", competitionFile.getDescription(), e.getMessage());
        }
    }

    private Resource sibling(Resource base, String filename) {
        try {
            URL baseUrl = base.getURL();
            String baseStr = baseUrl.toString();
            String siblingUrl = baseStr.substring(0, baseStr.lastIndexOf('/') + 1) + filename;
            return resourceResolver.getResource(siblingUrl);
        } catch (IOException e) {
            log.warn("Could not resolve sibling '{}' relative to {}: {}",
                    filename, base.getDescription(), e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // YAML file wrappers (package-private — not part of the public API)
    // -------------------------------------------------------------------------

    @Data
    static class TeamRuleFile {
        private String provider;
        private String entityType;
        private String competitionCanonicalKey;
        private Map<String, TeamProviderRule> items = new LinkedHashMap<>();
    }

    @Data
    static class SeasonRuleFile {
        private String provider;
        private String entityType;
        private String competitionCanonicalKey;
        private Map<String, SeasonProviderRule> items = new LinkedHashMap<>();
    }

    @Data
    static class CountryRuleFile {
        private String provider;
        private String entityType;
        private Map<String, CountryProviderRule> items = new LinkedHashMap<>();
    }
}

