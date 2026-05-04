package dev.jordanvoss.rugbylive.mapping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalMappingRepository extends JpaRepository<ExternalMapping, Long> {
    Optional<ExternalMapping> findByProviderAndEntityTypeAndExternalId(
            String provider, String entityType, String externalId);

    /** Reverse lookup — find the provider external ID for a known internal entity. */
    Optional<ExternalMapping> findByProviderAndEntityTypeAndInternalId(
            String provider, String entityType, String internalId);
}

