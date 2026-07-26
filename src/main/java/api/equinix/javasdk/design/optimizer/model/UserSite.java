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
     * Returns this site's <em>stated</em> weight: the explicit {@code weight} if positive,
     * otherwise the headcount normalized against the given total.
     *
     * <p><strong>Returns {@code 0} when neither weight nor headcount is set</strong> (or the total
     * headcount is 0) — but that does <em>not</em> mean the site has no influence on scoring. The
     * engine applies a per-site fallback on top of this value: a site with a stated weight of 0 is
     * weighted as an average site for its {@link SiteRole} (the stated weight per unit of role
     * importance, times its own role multiplier), and the result's explanation names such sites as
     * inferred. This method therefore reports the caller-supplied portion only, not the final
     * scoring weight.</p>
     *
     * @param totalHeadcount the headcount summed across all sites, used to normalize this site's
     *                       headcount
     * @return the stated weight, or {@code 0.0} when the caller supplied neither weight nor
     *         headcount
     */
    public double effectiveWeight(int totalHeadcount) {
        if (weight > 0) {
            return weight;
        }
        return totalHeadcount > 0 ? (double) headcount / totalHeadcount : 0.0;
    }
}
