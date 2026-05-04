package dev.jordanvoss.rugbylive.competition;

import dev.jordanvoss.rugbylive.country.Country;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "providerCountry")
@Entity
@Table(name = "competitions")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String canonicalKey;
    private String currentName;
    private String type;
    private Integer tier;
    private String format;
    private boolean international;
    private String gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_country_id")
    private Country providerCountry;

    private String displayRegion;
    private String primaryColour;
    private String secondaryColour;
    private String logoUrl;
    private String logoSource;
}
