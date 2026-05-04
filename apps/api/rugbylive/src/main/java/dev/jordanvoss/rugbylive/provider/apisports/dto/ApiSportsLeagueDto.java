package dev.jordanvoss.rugbylive.provider.apisports.dto;

import java.util.List;

public record ApiSportsLeagueDto(
        Integer id,
        String name,
        String type,
        String logo,
        ApiSportsCountryDto country,
        List<ApiSportsSeasonDto> seasons
) {
}

