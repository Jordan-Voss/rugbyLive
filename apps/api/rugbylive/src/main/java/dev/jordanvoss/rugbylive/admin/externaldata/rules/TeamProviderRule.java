package dev.jordanvoss.rugbylive.admin.externaldata.rules;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
/**
 * Maps to one entry in teams.yml — stable team identity and provider name corrections.
 * Keyed by provider external team ID.
 */
@Data
public class TeamProviderRule {
    private String canonicalKey;
    private String name;
    private String countryKey;
    /** Explains why the curated name differs from what the provider returns. */
    private String providerNameOverrideReason;
    private List<String> aliases = new ArrayList<>();
}
