package dev.jordanvoss.rugbylive.user.dto;

public record MeResponse(
        UserDto user,
        PreferencesDto preferences,
        FavouritesDto favourites
) {
}