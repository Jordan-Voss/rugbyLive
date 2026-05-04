package dev.jordanvoss.rugbylive.provider;
/** Raw country data as returned by an external provider — no RugbyLive IDs. */
public record ExternalCountryRecord(
        String provider,
        String externalId,
        String name,
        String code,
        String flagUrl) {}
