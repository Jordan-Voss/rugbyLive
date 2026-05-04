package dev.jordanvoss.rugbylive.provider;
/** Raw team data as returned by an external provider. */
public record ExternalTeamRecord(
        String provider,
        String externalId,
        String name,
        String logoUrl,
        Boolean national,
        Integer founded,
        String countryExternalId,
        String countryName,
        String countryCode) {}
