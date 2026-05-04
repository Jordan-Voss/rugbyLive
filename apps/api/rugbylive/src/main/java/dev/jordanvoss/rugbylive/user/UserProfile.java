package dev.jordanvoss.rugbylive.user;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    private UUID id;

    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "onboarding_complete")
    private boolean onboardingComplete;

    private String theme;
    private String timezone;
}