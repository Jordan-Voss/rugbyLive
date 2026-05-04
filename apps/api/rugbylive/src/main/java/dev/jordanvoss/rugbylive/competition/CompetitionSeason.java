package dev.jordanvoss.rugbylive.competition;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;

@Data
@ToString(exclude = "competition")
@Entity
@Table(name = "competition_seasons")
public class CompetitionSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    private Competition competition;

    private Integer seasonYear;
    private String displayName;
    private String shortName;
    private String logoUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean current;
}
