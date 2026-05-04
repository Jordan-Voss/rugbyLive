package dev.jordanvoss.rugbylive.provider.apisports.dto;

import java.util.List;

public record ApiSportsCountryResponse(
        List<ApiSportsCountryDto> response
) {
}

