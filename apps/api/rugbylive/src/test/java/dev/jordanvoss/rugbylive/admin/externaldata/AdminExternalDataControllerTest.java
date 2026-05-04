package dev.jordanvoss.rugbylive.admin.externaldata;
import dev.jordanvoss.rugbylive.admin.externaldata.competitions.AdminCompetitionDataService;
import dev.jordanvoss.rugbylive.admin.externaldata.countries.AdminCountryDataService;
import dev.jordanvoss.rugbylive.admin.externaldata.resolution.ExternalDataDecision;
import dev.jordanvoss.rugbylive.admin.externaldata.resolution.ExternalDataItem;
import dev.jordanvoss.rugbylive.admin.externaldata.resolution.ExternalDataResult;
import dev.jordanvoss.rugbylive.admin.externaldata.resolution.ExternalDataSummary;
import dev.jordanvoss.rugbylive.admin.externaldata.teams.AdminTeamDataService;
import dev.jordanvoss.rugbylive.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(AdminExternalDataController.class)
@Import(SecurityConfig.class)
class AdminExternalDataControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AdminCountryDataService countryService;
    @MockitoBean AdminCompetitionDataService competitionService;
    @MockitoBean AdminTeamDataService teamService;
    @Test
    void refreshCountries_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/external-data/countries"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void refreshCountries_returnsExternalDataResultForSingleProvider() throws Exception {
        ExternalDataItem item = ExternalDataItem.of("COUNTRY", "42", "Ireland",
                "ireland", "Ireland", ExternalDataDecision.NEW_ENTITY, null, null, false);
        ExternalDataResult result = new ExternalDataResult("api-sports", "COUNTRY", false,
                ExternalDataSummary.from(List.of(item)), List.of(item));
        given(countryService.refresh(any(), eq(false))).willReturn(Map.of("api-sports", result));
        mockMvc.perform(post("/api/v1/admin/external-data/countries")
                        .param("providers", "api-sports")
                        .param("commit", "false")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("api-sports"))
                .andExpect(jsonPath("$.entityType").value("COUNTRY"))
                .andExpect(jsonPath("$.commit").value(false))
                .andExpect(jsonPath("$.summary.total").value(1));
    }
    @Test
    void refreshCountries_returnsMultiProviderResultForMultipleProviders() throws Exception {
        ExternalDataResult result = ExternalDataResult.empty("api-sports", "COUNTRY", false);
        given(countryService.refresh(any(), eq(false))).willReturn(
                Map.of("api-sports", result, "sportmonks", result));
        mockMvc.perform(post("/api/v1/admin/external-data/countries")
                        .param("commit", "false")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("COUNTRY"))
                .andExpect(jsonPath("$.byProvider").isMap());
    }
    @Test
    void refreshCompetitionSeason_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/external-data/competitions/united-rugby-championship/seasons/2024"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void refreshCompetitionSeason_delegatesToCompetitionService() throws Exception {
        ExternalDataResult result = ExternalDataResult.empty("api-sports", "COMPETITION", false);
        given(competitionService.refreshSeason(eq("united-rugby-championship"), eq(2024), any(), eq(false)))
                .willReturn(Map.of("api-sports", result));
        mockMvc.perform(post("/api/v1/admin/external-data/competitions/united-rugby-championship/seasons/2024")
                        .param("providers", "api-sports")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("COMPETITION"));
    }
    @Test
    void refreshTeams_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/external-data/competitions/united-rugby-championship/seasons/2024/teams"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void refreshTeams_delegatesToTeamService() throws Exception {
        ExternalDataResult result = ExternalDataResult.empty("api-sports", "TEAM", false);
        given(teamService.refreshTeams(eq("united-rugby-championship"), eq(2024), any(), eq(false)))
                .willReturn(Map.of("api-sports", result));
        mockMvc.perform(post("/api/v1/admin/external-data/competitions/united-rugby-championship/seasons/2024/teams")
                        .param("providers", "api-sports")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("TEAM"));
    }
}
