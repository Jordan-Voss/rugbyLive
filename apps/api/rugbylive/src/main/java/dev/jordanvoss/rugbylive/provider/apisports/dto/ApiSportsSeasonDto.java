package dev.jordanvoss.rugbylive.provider.apisports.dto;

import java.time.LocalDate;

public record ApiSportsSeasonDto(
        Integer season,
        Boolean current,
        LocalDate start,
        LocalDate end
) {
}

