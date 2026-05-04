package dev.jordanvoss.rugbylive.admin.externaldata.resolution;
import java.util.Map;
/**
 * Wraps results from multiple providers for the same operation.
 * Used by the controller when no specific provider is requested.
 */
public record MultiProviderDataResult(
        String entityType,
        boolean commit,
        Map<String, ExternalDataResult> byProvider
) {}
