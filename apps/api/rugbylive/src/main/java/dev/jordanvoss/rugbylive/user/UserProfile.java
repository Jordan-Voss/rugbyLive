package dev.jordanvoss.rugbylive.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isOnboardingComplete() {
        return onboardingComplete;
    }

    public void setOnboardingComplete(boolean onboardingComplete) {
        this.onboardingComplete = onboardingComplete;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}