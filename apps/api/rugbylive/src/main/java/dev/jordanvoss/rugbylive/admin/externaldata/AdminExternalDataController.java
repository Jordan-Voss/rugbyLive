package dev.jordanvoss.rugbylive.admin.externaldata;
import dev.jordanvoss.rugbylive.admin.externaldata.competitions.AdminCompetitionDataService;
import dev.jordanvoss.rugbylive.admin.externaldata.countries.AdminCountryDataService;
import dev.jordanvoss.rugbylive.admin.externaldata.resolution.ExternalDataResult;
import dev.jordanvoss.rugbylive.admin.externaldata.resolution.MultiProviderDataResult;
import dev.jordanvoss.rugbylive.admin.externaldata.teams.AdminTeamDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
/**
 * Admin endpoints for pulling external rugby data into RugbyLive.
 *
 * All endpoints accept:
 *   ?providers=api-sports            → single provider
 *   ?providers=api-sports,sportmonks → multiple providers (comma-separated or repeated param)
 *   (omit providers)                 → all applicable providers
 *   ?commit=false                    → preview only (default)
 *   ?commit=true                     → write to DB
 *
 * Response is always either ExternalDataResult (single provider) or
 * MultiProviderDataResult (multiple providers).
 *
 * Flow:
 *   1. POST /countries
 *   2. POST /competitions/{key}/seasons/{year}
 *   3. POST /competitions/{key}/seasons/{year}/teams
 */
@RestController
@RequestMapping("/api/v1/admin/external-data")
public class AdminExternalDataController {
    private final AdminCountryDataService countryService;
    private final AdminCompetitionDataService competitionService;
    private final AdminTeamDataService teamService;
    public AdminExternalDataController(
            AdminCountryDataService countryService,
            AdminCompetitionDataService competitionService,
            AdminTeamDataService teamService) {
        this.countryService = countryService;
        this.competitionService = competitionService;
        this.teamService = teamService;
    }
    /**
     * Refresh countries from one or more providers.
     *
     * POST /api/v1/admin/external-data/countries?providers=api-sports&commit=false
     */
    @PostMapping("/countries")
    public ResponseEntity<?> refreshCountries(
            @RequestParam(required = false) List<String> providers,
            @RequestParam(defaultValue = "false") boolean commit) {
        Map<String, ExternalDataResult> results = countryService.refresh(providers, commit);
        return singleOrMulti("COUNTRY", commit, results);
    }
    /**
     * Refresh one competition + a specific season.
     *
     * POST /api/v1/admin/external-data/competitions/united-rugby-championship/seasons/2024?providers=api-sports&commit=false
     */
    @PostMapping("/competitions/{competitionKey}/seasons/{seasonYear}")
    public ResponseEntity<?> refreshCompetitionSeason(
            @PathVariable String competitionKey,
            @PathVariable int seasonYear,
            @RequestParam(required = false) List<String> providers,
            @RequestParam(defaultValue = "false") boolean commit) {
        Map<String, ExternalDataResult> results = competitionService.refreshSeason(
                competitionKey, seasonYear, providers, commit);
        return singleOrMulti("COMPETITION", commit, results);
    }
    /**
     * Refresh teams for a competition season.
     * Precondition: competition + season must already exist (run season endpoint first).
     *
     * POST /api/v1/admin/external-data/competitions/united-rugby-championship/seasons/2024/teams?providers=api-sports&commit=false
     */
    @PostMapping("/competitions/{competitionKey}/seasons/{seasonYear}/teams")
    public ResponseEntity<?> refreshTeams(
            @PathVariable String competitionKey,
            @PathVariable int seasonYear,
            @RequestParam(required = false) List<String> providers,
            @RequestParam(defaultValue = "false") boolean commit) {
        Map<String, ExternalDataResult> results = teamService.refreshTeams(
                competitionKey, seasonYear, providers, commit);
        return singleOrMulti("TEAM", commit, results);
    }
    // -------------------------------------------------------------------------
    /** Returns ExternalDataResult directly for a single provider, MultiProviderDataResult otherwise. */
    private ResponseEntity<?> singleOrMulti(String entityType, boolean commit,
                                             Map<String, ExternalDataResult> byProvider) {
        if (byProvider.size() == 1) {
            return ResponseEntity.ok(byProvider.values().iterator().next());
        }
        return ResponseEntity.ok(new MultiProviderDataResult(entityType, commit, byProvider));
    }
}
