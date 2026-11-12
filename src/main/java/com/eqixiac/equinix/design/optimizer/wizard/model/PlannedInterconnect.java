package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.core.model.MetroId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A Fabric Interconnect (the managed routed network behind Fabric One) that a deployment plan
 * includes, but that this SDK cannot create.
 *
 * <p><b>What the public API offers (spec Fabric v4 4.34.0, checked 2026-11-12):</b> the {@code IC}
 * access-point type on connections, the {@code IC_ROUTER} Cloud Router type (read side only),
 * {@code IC_PROFILE} service profiles with {@code IC_ENV} provider environments, and activation-key
 * validation. There is <em>no</em> public endpoint that orders or creates an Interconnect. A plan
 * element of this type is therefore planned and rendered, priced as unknown (no public price exists),
 * and never sent to the API: reconcile and execute report it as {@code NOT_EXECUTABLE} with
 * {@link #notExecutableReason()}, and drift detection reports it as not managed by the SDK.</p>
 *
 * <p>When the customer already holds an Interconnect (ordered outside the API), its uuid can be
 * recorded in {@link #existingInterconnectUuid} so the plan documents which one it relies on.</p>
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class PlannedInterconnect {

    /** Why an Interconnect is never executable through this SDK today. */
    public static final String NOT_EXECUTABLE_REASON = "The public Fabric v4 API (4.34.0) has no endpoint that "
            + "creates or orders a Fabric Interconnect; it exposes only IC access points, IC_ROUTER (read), "
            + "IC_PROFILE / IC_ENV and activation-key validation. Order the Interconnect through Equinix, then "
            + "record its uuid on the plan.";

    /** Stable logical key, for example {@code interconnect/DC/core}. */
    String key;

    /** A short display name for the Interconnect. */
    String name;

    /** The metro the Interconnect is planned in. */
    MetroId metro;

    /** The bandwidth the design needs from it in Mbps, or {@code null} when not stated. */
    Integer bandwidthMbps;

    /** Free text: what the Interconnect is for. May be {@code null}. */
    String purpose;

    /** The uuid of an Interconnect the customer already holds, or {@code null}. */
    String existingInterconnectUuid;

    /**
     * Always {@code false}: no public API creates an Interconnect.
     *
     * @return {@code false}
     */
    @JsonIgnore
    public boolean isExecutable() {
        return false;
    }

    /**
     * Why the SDK will not create this element.
     *
     * @return {@link #NOT_EXECUTABLE_REASON}
     */
    public String notExecutableReason() {
        return NOT_EXECUTABLE_REASON;
    }

    /**
     * Why the element carries no price.
     *
     * @return a fixed statement that no public price exists
     */
    public String unpricedReason() {
        return "unpriced: Equinix publishes no API price for a Fabric Interconnect; the cost is unknown, not zero";
    }
}
