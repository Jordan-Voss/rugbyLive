package dev.jordanvoss.rugbylive.user;

import dev.jordanvoss.rugbylive.user.dto.FavouritesDto;
import dev.jordanvoss.rugbylive.user.dto.MeResponse;
import dev.jordanvoss.rugbylive.user.dto.PreferencesDto;
import dev.jordanvoss.rugbylive.user.dto.UserDto;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserProfileRepository userProfileRepository;

    public UserService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public MeResponse getOrCreateMe(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setId(userId);
                    newProfile.setEmail(email);
                    newProfile.setOnboardingComplete(false);
                    newProfile.setTheme("system");
                    newProfile.setTimezone("Europe/Dublin");
                    return newProfile;
                });

        if (profile.getEmail() == null && email != null) {
            profile.setEmail(email);
        }

        UserProfile savedProfile = userProfileRepository.save(profile);

        return new MeResponse(
                new UserDto(
                        savedProfile.getId(),
                        savedProfile.getEmail(),
                        savedProfile.getDisplayName(),
                        savedProfile.isOnboardingComplete()
                ),
                new PreferencesDto(
                        savedProfile.getTheme(),
                        savedProfile.getTimezone()
                ),
                new FavouritesDto(
                        List.of(),
                        List.of()
                )
        );
    }
}