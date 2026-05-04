package dev.jordanvoss.rugbylive.admin.externaldata.rules;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
/**
 * Maps to competition.yml — canonical definition for one competition from one provider.
 * externalId links this rule to the provider's numeric/string ID.
 */
@Data
public class CompetitionProviderRule {
    private String externalId;
    private String canonicalKey;
    private String currentName;
    private String shortName;
    private String displayRegion;
    private boolean international;
    private String gender = "men";
    private List<String> participantCountryKeys = new ArrayList<>();
    /** All known names/aliases registered as competition_aliases on ingest. */
    private List<String> aliases = new ArrayList<>();
}
