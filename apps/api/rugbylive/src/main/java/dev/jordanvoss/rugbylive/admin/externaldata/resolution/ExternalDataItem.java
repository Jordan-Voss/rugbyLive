package dev.jordanvoss.rugbylive.admin.externaldata.resolution;
import java.util.List;
/** One resolved item from a provider data refresh — shows what did or would happen. */
public record ExternalDataItem(
        String entityType,
        String externalId,
        String providerName,
        /** Curated/normalised display name that would be written. */
        String resolvedName,
        /** The canonical key that will be written to RugbyLive DB. */
        String canonicalKey,
        /** Canonical country key (teams only). */
        String countryKey,
        /** Rugby/sport code that would be written (countries only — e.g. ENG, SCO, IRE). */
        String code,
        ExternalDataDecision decision,
        /** Internal RugbyLive ID, if an existing entity was matched. Null for NEW_ENTITY. */
        Long internalId,
        /** The canonical key of the existing entity that was matched. Null for NEW_ENTITY. */
        String existingCanonicalKey,
        boolean written,
        List<String> notes
) {
    /**
     * Convenience factory — no code (competitions, seasons).
     * existingCanonicalKey should be null when decision is NEW_ENTITY — pass null explicitly.
     */
    public static ExternalDataItem of(
            String entityType, String externalId, String providerName,
            String canonicalKey, String resolvedName, ExternalDataDecision decision,
            Long internalId, String existingCanonicalKey, boolean written) {
        return new ExternalDataItem(entityType, externalId, providerName,
                resolvedName, canonicalKey, null, null, decision, internalId, existingCanonicalKey,
                written, List.of());
    }
    /** Factory for NEEDS_REVIEW items — never written regardless of commit flag. */
    public static ExternalDataItem needsReview(
            String entityType, String externalId, String providerName,
            String canonicalKey, List<String> notes) {
        return new ExternalDataItem(entityType, externalId, providerName,
                null, canonicalKey, null, null, ExternalDataDecision.NEEDS_REVIEW,
                null, null, false, notes);
    }
}
