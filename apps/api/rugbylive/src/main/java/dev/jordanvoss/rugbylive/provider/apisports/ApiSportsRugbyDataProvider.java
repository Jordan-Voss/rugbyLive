package dev.jordanvoss.rugbylive.provider.apisports;

import dev.jordanvoss.rugbylive.provider.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Thin adapter that maps API-Sports HTTP responses to provider-agnostic records.
 *
 * This class knows:
 * - API-Sports endpoint paths and auth header
 * - The shape of API-Sports DTOs
 *
 * This class does NOT know about:
 * - RugbyLive entities, repositories, or canonical keys
 * - Resolution logic or DB writes
 * - Any other provider
 */
@Service
public class ApiSportsRugbyDataProvider implements ExternalRugbyDataProvider {

    private final ApiSportsClient client;

    public ApiSportsRugbyDataProvider(ApiSportsClient client) {
        this.client = client;
    }

    @Override
    public String providerKey() {
        return "api-sports";
    }

    @Override
    public List<ExternalCountryRecord> fetchCountries() {
        var response = client.getCountries();
        if (response == null || response.response() == null) return List.of();
        return response.response().stream()
                .map(dto -> new ExternalCountryRecord(
                        providerKey(),
                        dto.id() == null ? null : dto.id().toString(),
                        dto.name(),
                        dto.code(),
                        dto.flag()))
                .toList();
    }

    @Override
    public Optional<ExternalCompetitionRecord> fetchCompetition(String externalCompetitionId) {
        var response = client.getLeague(Integer.parseInt(externalCompetitionId));
        if (response == null || response.response() == null || response.response().isEmpty()) {
            return Optional.empty();
        }
        var dto = response.response().getFirst();
        List<ExternalSeasonRecord> seasons = dto.seasons() == null ? List.of()
                : dto.seasons().stream()
                        .map(s -> new ExternalSeasonRecord(s.season(), s.current(), s.start(), s.end()))
                        .toList();
        return Optional.of(new ExternalCompetitionRecord(
                providerKey(),
                externalCompetitionId,
                dto.name(),
                dto.type(),
                dto.logo(),
                dto.country() == null ? null : dto.country().name(),
                dto.country() == null ? null : dto.country().code(),
                seasons));
    }

    @Override
    public List<ExternalTeamRecord> fetchTeams(String externalCompetitionId, int seasonYear) {
        var response = client.getTeams(Integer.parseInt(externalCompetitionId), seasonYear);
        if (response == null || response.response() == null) return List.of();
        return response.response().stream()
                .map(dto -> new ExternalTeamRecord(
                        providerKey(),
                        dto.id() == null ? null : dto.id().toString(),
                        dto.name(),
                        dto.logo(),
                        dto.national(),
                        dto.founded(),
                        dto.country() != null && dto.country().id() != null
                                ? dto.country().id().toString() : null,
                        dto.country() == null ? null : dto.country().name(),
                        dto.country() == null ? null : dto.country().code()))
                .toList();
    }
}

