package dev.jordanvoss.rugbylive.provider;
import java.util.List;
/** Raw competition/league data as returned by an external provider. */
public record ExternalCompetitionRecord(
        String provider,
        String externalId,
        String name,
        String type,
        String logoUrl,
        String countryName,
        String countryCode,
        List<ExternalSeasonRecord> seasons) {}
