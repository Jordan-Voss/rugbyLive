package dev.jordanvoss.rugbylive.competition.dto;

public record CompetitionResponse(
        Long id,
        String canonicalKey,
        String currentName,
        String type,
        String logoUrl,
        String displayRegion,
        String providerCountry
) {
}

