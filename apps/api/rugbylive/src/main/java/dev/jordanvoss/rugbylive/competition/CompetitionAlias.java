package dev.jordanvoss.rugbylive.competition;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "competition")
@Entity
@Table(name = "competition_aliases")
public class CompetitionAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    private Competition competition;

    private String alias;
    private String normalized;
    private String provider;
    private Integer validFromYear;
    private Integer validToYear;
}
