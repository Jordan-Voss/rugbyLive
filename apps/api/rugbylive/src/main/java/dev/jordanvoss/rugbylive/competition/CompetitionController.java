package dev.jordanvoss.rugbylive.competition;

import dev.jordanvoss.rugbylive.competition.dto.CompetitionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CompetitionController {

    private final CompetitionRepository competitionRepository;

    public CompetitionController(CompetitionRepository competitionRepository) {
        this.competitionRepository = competitionRepository;
    }

    @GetMapping("/api/v1/competitions")
    public List<CompetitionResponse> competitions() {
        return competitionRepository.findAll().stream()
                .map(competition -> new CompetitionResponse(
                        competition.getId(),
                        competition.getCanonicalKey(),
                        competition.getCurrentName(),
                        competition.getType(),
                        competition.getLogoUrl(),
                        competition.getDisplayRegion(),
                        competition.getProviderCountry() == null
                                ? null
                                : competition.getProviderCountry().getName()
                ))
                .toList();
    }
}

