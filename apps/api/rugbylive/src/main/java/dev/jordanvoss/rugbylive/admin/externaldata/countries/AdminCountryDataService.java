package dev.jordanvoss.rugbylive.admin.externaldata.countries;
import dev.jordanvoss.rugbylive.admin.externaldata.resolution.*;
import dev.jordanvoss.rugbylive.admin.externaldata.rules.*;
import dev.jordanvoss.rugbylive.country.Country;
import dev.jordanvoss.rugbylive.country.CountryRepository;
import dev.jordanvoss.rugbylive.mapping.ExternalMapping;
import dev.jordanvoss.rugbylive.mapping.ExternalMappingRepository;
import dev.jordanvoss.rugbylive.provider.ExternalCountryRecord;
import dev.jordanvoss.rugbylive.provider.ExternalRugbyDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
public class AdminCountryDataService {
    private final Map<String, ExternalRugbyDataProvider> providers;
    private final ExternalDataRuleService rules;
    private final CountryRepository countryRepository;
    private final ExternalMappingRepository mappingRepository;
    public AdminCountryDataService(List<ExternalRugbyDataProvider> providers,
            ExternalDataRuleService rules, CountryRepository countryRepository,
            ExternalMappingRepository mappingRepository) {
        this.providers = providers.stream().collect(
                Collectors.toMap(ExternalRugbyDataProvider::providerKey, p -> p));
        this.rules = rules;
        this.countryRepository = countryRepository;
        this.mappingRepository = mappingRepository;
    }
    @Transactional
    public Map<String, ExternalDataResult> refresh(List<String> providerKeys, boolean commit) {
        Map<String, ExternalDataResult> results = new LinkedHashMap<>();
        for (ExternalRugbyDataProvider provider : selectProviders(providerKeys)) {
            results.put(provider.providerKey(), refreshFromProvider(provider, commit));
        }
        return results;
    }
    private ExternalDataResult refreshFromProvider(ExternalRugbyDataProvider provider, boolean commit) {
        List<ExternalCountryRecord> records = provider.fetchCountries();
        if (records.isEmpty()) return ExternalDataResult.empty(provider.providerKey(), "COUNTRY", commit);
        List<ExternalDataItem> items = records.stream().map(r -> resolveCountry(r, commit)).toList();
        return new ExternalDataResult(provider.providerKey(), "COUNTRY", commit, ExternalDataSummary.from(items), items);
    }
    private ExternalDataItem resolveCountry(ExternalCountryRecord record, boolean commit) {
        Optional<CountryProviderRule> yamlRule = yamlRule(record);
        String proposedKey = yamlRule.map(CountryProviderRule::getCanonicalKey).orElse(slug(record.name()));
        String resolvedName = yamlRule.map(CountryProviderRule::getName)
                .filter(n -> n != null && !n.isBlank())
                .orElse(record.name());
        // code: prefer YAML rugby code (e.g. ENG, SCO), fall back to provider code (e.g. GB, IE)
        String resolvedCode = yamlRule.map(CountryProviderRule::getCode)
                .filter(c -> c != null && !c.isBlank())
                .orElse(record.code());
        ExternalDataDecision decision;

        Optional<ExternalMapping> existing = mappingRepository
                .findByProviderAndEntityTypeAndExternalId(record.provider(), "COUNTRY", record.externalId());
        if (existing.isPresent()) {
            Country country = countryRepository.findById(Long.parseLong(existing.get().getInternalId())).orElseThrow();
            if (commit) { applyFields(country, record, proposedKey, resolvedName, resolvedCode); countryRepository.save(country); upsertMapping("COUNTRY", country.getId().toString(), record, existing.get().getConfidence()); }
            decision = yamlRule.isPresent() ? ExternalDataDecision.APPLIED_PROVIDER_RULE : ExternalDataDecision.EXISTING_PROVIDER_MAPPING;
            return new ExternalDataItem("COUNTRY", record.externalId(), record.name(), resolvedName, proposedKey,
                    null, resolvedCode, decision, country.getId(), country.getCanonicalKey(), commit, List.of());
        }
        Optional<Country> byKey = countryRepository.findByCanonicalKey(proposedKey);
        if (byKey.isPresent()) {
            Country country = byKey.get();
            if (commit) { applyFields(country, record, proposedKey, resolvedName, resolvedCode); countryRepository.save(country); upsertMapping("COUNTRY", country.getId().toString(), record, "name_match"); }
            decision = yamlRule.isPresent() ? ExternalDataDecision.APPLIED_PROVIDER_RULE : ExternalDataDecision.MATCHED_CANONICAL_KEY;
            return new ExternalDataItem("COUNTRY", record.externalId(), record.name(), resolvedName, proposedKey,
                    null, resolvedCode, decision, country.getId(), country.getCanonicalKey(), commit, List.of());
        }
        if (commit) {
            Country country = new Country();
            applyFields(country, record, proposedKey, resolvedName, resolvedCode);
            country = countryRepository.save(country);
            upsertMapping("COUNTRY", country.getId().toString(), record, yamlRule.isPresent() ? "confirmed" : "provider");
            return new ExternalDataItem("COUNTRY", record.externalId(), record.name(), resolvedName, proposedKey,
                    null, resolvedCode, ExternalDataDecision.NEW_ENTITY, country.getId(), country.getCanonicalKey(), true, List.of());
        }
        ExternalDataDecision previewDecision = yamlRule.isPresent()
                ? ExternalDataDecision.APPLIED_PROVIDER_RULE : ExternalDataDecision.NEW_ENTITY;
        return new ExternalDataItem("COUNTRY", record.externalId(), record.name(), resolvedName, proposedKey,
                null, resolvedCode, previewDecision, null, null, false, List.of());
    }

    /**
     * Resolves the YAML rule for a country record.
     * First tries matching by provider code (e.g. 3-letter rugby codes like IRE, ENG).
     * Falls back to matching by slugified name (e.g. "England" → "england").
     */
    private Optional<CountryProviderRule> yamlRule(ExternalCountryRecord r) {
        if (r.code() != null && !r.code().isBlank()) {
            Optional<CountryProviderRule> byCode = rules.countryByProviderCode(r.provider(), r.code());
            if (byCode.isPresent()) return byCode;
        }
        return rules.countryByCanonicalKey(r.provider(), slug(r.name()));
    }

    private void applyFields(Country c, ExternalCountryRecord r, String canonicalKey, String resolvedName, String resolvedCode) {
        c.setCanonicalKey(canonicalKey); c.setName(resolvedName); c.setCode(resolvedCode);
        c.setFlagUrl(r.flagUrl()); c.setFlagSource(r.provider());
    }
    private void upsertMapping(String entityType, String internalId, ExternalCountryRecord r, String confidence) {
        ExternalMapping m = mappingRepository.findByProviderAndEntityTypeAndExternalId(r.provider(), entityType, r.externalId()).orElseGet(ExternalMapping::new);
        m.setProvider(r.provider()); m.setEntityType(entityType); m.setInternalId(internalId);
        m.setExternalId(r.externalId()); m.setExternalName(r.name()); m.setActive(true);
        if (m.getConfidence() == null || rank(confidence) > rank(m.getConfidence())) m.setConfidence(confidence);
        mappingRepository.save(m);
    }
    private List<ExternalRugbyDataProvider> selectProviders(List<String> keys) {
        if (keys == null || keys.isEmpty() || keys.contains("all")) return new ArrayList<>(providers.values());
        return keys.stream().map(k -> { ExternalRugbyDataProvider p = providers.get(k); if (p == null) throw new IllegalArgumentException("Unknown provider: " + k); return p; }).toList();
    }
    static String slug(String v) { return v == null ? "unknown" : v.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); }
    static int rank(String c) { return switch (c) { case "confirmed" -> 3; case "provider" -> 2; case "name_match" -> 1; default -> 0; }; }
}
