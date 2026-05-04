package dev.jordanvoss.rugbylive.admin.externaldata.resolution;
import java.util.List;
/**
 * Aggregated counts across all items in a data refresh run.
 *
 * When commit=false: written=0, wouldCreate shows what would be created.
 * When commit=true:  wouldCreate=0, written shows what was actually persisted.
 */
public record ExternalDataSummary(
        int total,
        int existingMappings,
        int matchedCanonicalKey,
        int matchedAlias,
        int appliedProviderRule,
        int wouldCreate,
        int written,
        int needsReview,
        int conflicts
) {
    public static ExternalDataSummary from(List<ExternalDataItem> items) {
        return new ExternalDataSummary(
                items.size(),
                count(items, ExternalDataDecision.EXISTING_PROVIDER_MAPPING),
                count(items, ExternalDataDecision.MATCHED_CANONICAL_KEY),
                count(items, ExternalDataDecision.MATCHED_ALIAS),
                count(items, ExternalDataDecision.APPLIED_PROVIDER_RULE),
                wouldCreate(items),
                (int) items.stream().filter(ExternalDataItem::written).count(),
                count(items, ExternalDataDecision.NEEDS_REVIEW),
                count(items, ExternalDataDecision.CONFLICT)
        );
    }
    public static ExternalDataSummary empty() {
        return new ExternalDataSummary(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    private static int count(List<ExternalDataItem> items, ExternalDataDecision decision) {
        return (int) items.stream().filter(i -> i.decision() == decision).count();
    }
    private static int wouldCreate(List<ExternalDataItem> items) {
        return (int) items.stream()
                .filter(i -> i.internalId() == null
                        && i.decision() != ExternalDataDecision.NEEDS_REVIEW
                        && i.decision() != ExternalDataDecision.CONFLICT)
                .count();
    }
}
