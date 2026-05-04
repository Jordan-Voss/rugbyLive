package dev.jordanvoss.rugbylive.team;

import dev.jordanvoss.rugbylive.country.Country;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "country")
@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String canonicalKey;
    private String name;
    private String shortName;
    private String logoUrl;
    private String logoSource;
    private boolean national;
    private Integer founded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;
}
