package dev.jordanvoss.rugbylive.provider.apisports.dto;

import java.util.List;

public record ApiSportsLeagueResponse(
        List<ApiSportsLeagueDto> response
) {
}

