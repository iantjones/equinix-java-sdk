package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * The run's {@link com.eqixiac.equinix.design.spec.DesignSpec} restated in the shape the Deployment
 * Wizard reads: sites, clouds (as {@link ProviderRequirement}s labelled by the spec's cloud id) and
 * workloads (as {@link WorkloadSpec}s whose dependencies carry the same labels). A derived view: the
 * spec on {@code OptimizationResult.getSpec()} is the source of truth.
 */
@Value
@Builder
public class OptimizationRequest {

    /** The spec's sites. */
    List<UserSite> sites;

    /** One requirement per spec cloud; {@code label} is the cloud id. */
    List<ProviderRequirement> providers;

    /** The spec's workloads. */
    List<WorkloadSpec> workloads;

    /** The rate card the run priced with, when the caller supplied one; may be {@code null}. */
    @JsonIgnore
    RateCard rateCard;

    /** The commitment term the run priced at. */
    Term term;

    /**
     * <b>Beta.</b> The catalog of native multicloud environments the optimizer consults for the
     * {@code NATIVE_MULTICLOUD_ALTERNATIVE} finding, and the Deployment Wizard's default when its
     * own lever is not set. {@code null} means {@link MulticloudEnvironmentCatalog#standard()}.
     * Reference data, not part of the request's serialized form.
     */
    @JsonIgnore
    MulticloudEnvironmentCatalog multicloudEnvironments;
}
