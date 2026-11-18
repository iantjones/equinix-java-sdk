package com.eqixiac.equinix.design.optimizer.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Indicates whether a specific provider is available at a metro,
 * and if so, which seller regions are supported.
 *
 * <p>{@link #serviceProfileUuid} and {@link #sellerRegions} carry the single <em>outranks</em>
 * winner - the profile chosen for scoring and seller-region preference, and the default the wizard
 * pins when it has no bandwidth to reconcile. {@link #profileOptions} carries EVERY matching profile
 * for the metro (each with its bandwidth capability) so the wizard can instead pick a profile whose
 * allowed tiers cover the connection's speed once that speed is known. It is {@code null} on
 * hand-built availability entries that predate bandwidth-aware selection, which the wizard reads as
 * "no capability data - fall back to the default uuid".</p>
 */
@Value
@Builder(toBuilder = true)
public class ProviderAvailability {

    String providerLabel;
    boolean available;
    List<String> sellerRegions;
    String serviceProfileUuid;

    /**
     * All candidate service profiles for this (provider, metro), each with its bandwidth capability,
     * for bandwidth-aware selection at wizard time. {@code null} means no capability data was carried
     * (a legacy/hand-built entry); an available entry produced by the optimizer always lists at least
     * the winner.
     */
    List<ServiceProfileOption> profileOptions;

    /**
     * The cloud region the design asked for ({@code CloudIntent.region}), or {@code null} when it named
     * none.
     */
    String requestedRegion;

    /**
     * Whether the on-ramp at this metro serves {@code requestedRegion}: {@code TRUE} when the chosen
     * profile lists it, {@code FALSE} when the profile lists other regions only, {@code null} when the
     * design named no region or the profile publishes no region list. A {@code FALSE} on-ramp still
     * reaches the provider; traffic to the named region then continues over the provider's own network
     * (for example an AWS Direct Connect gateway), which the SDK neither models nor prices.
     */
    Boolean servesRequestedRegion;
}
