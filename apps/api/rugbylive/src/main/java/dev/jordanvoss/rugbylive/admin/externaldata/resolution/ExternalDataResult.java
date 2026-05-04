package dev.jordanvoss.rugbylive.admin.externaldata.resolution;
import java.util.List;
/** Result of refreshing one entity type from one provider. */
public record ExternalDataResult(
        String provider,
        String entityType,
        boolean commit,
        ExternalDataSummary summary,
        List<ExternalDataItem> items
) {
    public static ExternalDataResult empty(String provider, String entityType, boolean commit) {
        return new ExternalDataResult(provider, entityType, commit, ExternalDataSummary.empty(), List.of());
    }
}
