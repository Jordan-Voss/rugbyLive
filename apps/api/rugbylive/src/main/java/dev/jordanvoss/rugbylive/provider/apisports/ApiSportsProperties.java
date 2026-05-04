package dev.jordanvoss.rugbylive.provider.apisports;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api-sports")
public record ApiSportsProperties(
        String baseUrl,
        String apiKey
) {
}

