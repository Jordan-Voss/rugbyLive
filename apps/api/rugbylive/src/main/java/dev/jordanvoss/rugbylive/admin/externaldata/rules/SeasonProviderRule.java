package dev.jordanvoss.rugbylive.admin.externaldata.rules;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
/**
 * One entry in seasons.yml — season-specific display metadata for a competition.
 * Only seasons that differ or have expected team validation need entries.
 */
@Data
public class SeasonProviderRule {
    private String displayName;
    private String shortName;
    private List<String> expectedTeamCanonicalKeys = new ArrayList<>();
}
