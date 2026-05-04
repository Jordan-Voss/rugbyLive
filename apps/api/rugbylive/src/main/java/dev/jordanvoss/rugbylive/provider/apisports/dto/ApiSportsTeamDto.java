package dev.jordanvoss.rugbylive.provider.apisports.dto;

public record ApiSportsTeamDto(
        Integer id,
        String name,
        String logo,
        Boolean national,
        Integer founded,
        ApiSportsArenaDto arena,
        ApiSportsCountryDto country
) {
}

