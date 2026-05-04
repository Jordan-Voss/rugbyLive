package dev.jordanvoss.rugbylive.provider;
import java.util.List;
import java.util.Optional;
/**
 * Thin adapter contract for an external rugby data source.
 *
 * Implementations only know how to call their API and convert responses to
 * provider-agnostic records. No RugbyLive business logic or DB writes here.
 */
public interface ExternalRugbyDataProvider {
    /** Unique key matching the provider's curation folder name (e.g. "api-sports"). */
    String providerKey();
    List<ExternalCountryRecord> fetchCountries();
    Optional<ExternalCompetitionRecord> fetchCompetition(String externalCompetitionId);
    /**
     * Fetches all teams participating in a given external competition/league for a season.
     *
     * @param externalCompetitionId provider-specific competition ID
     * @param seasonYear            four-digit season year (e.g. 2024)
     */
    List<ExternalTeamRecord> fetchTeams(String externalCompetitionId, int seasonYear);
}
