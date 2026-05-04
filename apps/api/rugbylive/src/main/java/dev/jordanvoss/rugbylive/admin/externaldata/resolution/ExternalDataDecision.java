package dev.jordanvoss.rugbylive.admin.externaldata.resolution;
/** The outcome of resolving one external record against RugbyLive canonical data. */
public enum ExternalDataDecision {
    /** Provider external ID already in external_mappings — entity is known. */
    EXISTING_PROVIDER_MAPPING,
    /** No mapping found, but canonical_key matched an existing entity. */
    MATCHED_CANONICAL_KEY,
    /** Provider name matched a known competition_alias or alternate_name. */
    MATCHED_ALIAS,
    /** Canonical key and display data were determined by a YAML provider rule, not raw provider data. */
    APPLIED_PROVIDER_RULE,
    /** No existing match — safe to create a new canonical entity. */
    NEW_ENTITY,
    /** Not enough information to safely create or link — requires manual review. */
    NEEDS_REVIEW,
    /** Multiple possible matches found — requires manual resolution. */
    CONFLICT
}
