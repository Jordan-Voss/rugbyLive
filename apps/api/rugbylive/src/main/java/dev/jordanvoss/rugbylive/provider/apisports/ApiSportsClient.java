package dev.jordanvoss.rugbylive.provider.apisports;

import dev.jordanvoss.rugbylive.provider.apisports.dto.ApiSportsCountryResponse;
import dev.jordanvoss.rugbylive.provider.apisports.dto.ApiSportsLeagueResponse;
import dev.jordanvoss.rugbylive.provider.apisports.dto.ApiSportsTeamResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ApiSportsClient {

    private final RestClient restClient;

    public ApiSportsClient(ApiSportsProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-apisports-key", properties.apiKey())
                .build();
    }

    public ApiSportsCountryResponse getCountries() {
        return restClient.get()
                .uri("/countries")
                .retrieve()
                .body(ApiSportsCountryResponse.class);
    }

    public ApiSportsLeagueResponse getLeague(Integer leagueId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leagues")
                        .queryParam("id", leagueId)
                        .build())
                .retrieve()
                .body(ApiSportsLeagueResponse.class);
    }

    public ApiSportsTeamResponse getTeams(Integer leagueId, Integer season) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .body(ApiSportsTeamResponse.class);
    }
}

