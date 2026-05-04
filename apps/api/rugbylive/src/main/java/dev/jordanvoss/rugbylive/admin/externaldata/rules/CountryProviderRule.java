package dev.jordanvoss.rugbylive.admin.externaldata.rules;
import lombok.Data;
/** Maps to one entry in countries.yml — provider-specific code override for a country. */
@Data
public class CountryProviderRule {
    private String canonicalKey;
    private String name;
    private String code;
}
