package dev.jordanvoss.rugbylive.competition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionSeasonRepository extends JpaRepository<CompetitionSeason, Long> {
    Optional<CompetitionSeason> findByCompetitionIdAndSeasonYear(Long competitionId, Integer seasonYear);
}

