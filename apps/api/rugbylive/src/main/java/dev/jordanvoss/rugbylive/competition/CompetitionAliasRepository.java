package dev.jordanvoss.rugbylive.competition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionAliasRepository extends JpaRepository<CompetitionAlias, Long> {
    Optional<CompetitionAlias> findByCompetitionIdAndNormalizedAndProvider(
            Long competitionId,
            String normalized,
            String provider
    );
}

