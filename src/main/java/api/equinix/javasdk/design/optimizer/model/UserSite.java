package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.enums.SiteRole;
import lombok.Builder;
import lombok.Value;

/**
 * A user-defined location representing a workforce concentration, customer market,
 * or operational site. Used by the optimizer to compute latency-weighted metro scores.
 */
@Value
@Builder
public class UserSite {

    String label;

    /**
     * The Equinix metro code nearest to this site. Used for direct latency lookups
     * in the Fabric metro interconnection data. This is the preferred way to specify
     * location; if set, it takes precedence over {@link #latitude}/{@link #longitude}
     * for latency lookups.
     */
    MetroId nearestMetro;

    /**
     * Latitude in decimal degrees. Used as a fallback for distance-based latency
     * estimation when {@link #nearestMetro} is not set or no direct latency data exists.
     */
    Double latitude;

    /**
     * Longitude in decimal degrees. Used together with {@link #latitude} for
     * geo-distance estimation.
     */
    Double longitude;

    SiteRole role;

    /**
     * Number of employees or users at this site. When no explicit {@link #weight} is set,
     * headcount is normalized across all sites to derive relative importance.
     */
    int headcount;

    /**
     * Explicit importance weight for this site. If positive, overrides headcount-based
     * normalization. Values are relative to other sites.
     */
    double weight;

    /**
     * Returns the effective weight for this site in scoring calculations.
     * If an explicit weight is set, it is used; otherwise the headcount is
     * normalized against the total across all sites by the engine.
     */
    public double effectiveWeight(int totalHeadcount) {
        if (weight > 0) {
            return weight;
        }
        return totalHeadcount > 0 ? (double) headcount / totalHeadcount : 0.0;
    }
}
