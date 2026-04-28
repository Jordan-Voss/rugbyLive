package dev.jordanvoss.rugbylive.user.dto;

import java.util.List;

public record FavouritesDto(
        List<Object> teams,
        List<Object> competitions
) {
}