package dev.jordanvoss.rugbylive.provider;
import java.time.LocalDate;
/** Raw season data embedded inside a competition record from an external provider. */
public record ExternalSeasonRecord(
        int year,
        Boolean current,
        LocalDate startDate,
        LocalDate endDate) {}
