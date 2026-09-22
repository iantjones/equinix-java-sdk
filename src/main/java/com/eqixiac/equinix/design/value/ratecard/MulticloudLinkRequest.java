package com.eqixiac.equinix.design.value.ratecard;

import com.eqixiac.equinix.core.model.multicloud.ProviderRef;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

import java.util.Optional;

/**
 * The lookup key for {@link RateCard#multicloudLink(MulticloudLinkRequest)}: one native
 * provider-to-provider multicloud link between two clouds.
 *
 * <p><b>Beta</b>: models products that reached general availability in 2026 (AWS Interconnect -
 * multicloud, Google Partner Cross-Cloud Interconnect). Field semantics follow the providers'
 * public pricing pages as retrieved on 2026-09-21 and may change with them.</p>
 *
 * <table>
 *   <caption>Fields, units and defaults</caption>
 *   <tr><th>Field</th><th>Unit / domain</th><th>Default</th><th>Required</th></tr>
 *   <tr><td>{@code providerA}, {@code providerZ}</td><td>{@code CloudProviderType}; must differ</td>
 *       <td>none</td><td>yes</td></tr>
 *   <tr><td>{@code regionA}, {@code regionZ}</td><td>the provider's own region notation
 *       ({@code "us-east-1"}, {@code "us-east4"})</td><td>{@code null}</td><td>no</td></tr>
 *   <tr><td>{@code bandwidthMbps}</td><td>Mbps, {@code > 0}</td><td>none</td><td>yes</td></tr>
 *   <tr><td>{@code pathTier}</td><td>1-5, the AWS connectivity-scope tier</td>
 *       <td>{@value MulticloudLinkQuote#DEFAULT_PATH_TIER}</td><td>no</td></tr>
 *   <tr><td>{@code term}</td><td>{@link Term}</td><td>{@code null}</td><td>no</td></tr>
 *   <tr><td>{@code useAwsFreeTier}</td><td>boolean</td><td>{@code false}</td><td>no</td></tr>
 * </table>
 *
 * <h2>Path tier</h2>
 * <p>AWS assigns each interconnect a tier from the geographic scope of the Region paths it
 * serves: 1 local, 2 regional, 3 continental, 4 long-haul, 5 maximum scope (source:
 * {@code https://docs.aws.amazon.com/interconnect/latest/userguide/interconnect-pricing.html}).
 * AWS publishes no Region-path-to-tier table, so this SDK cannot derive the tier; it is an
 * explicit input. The default of 1 assumes every workload endpoint is in the interconnect's
 * local Region. Providers without a tier concept ignore the field.</p>
 *
 * <h2>AWS free tier</h2>
 * <p>{@code useAwsFreeTier} is an opt-in. AWS offers one free local (tier 1) 500 Mbps
 * interconnect per AWS Region per generally-available provider. A rate card that models the
 * offer prices the AWS side at zero only when this flag is set and the request is eligible
 * (bandwidth at or below 500 Mbps, tier 1, an eligible peer). The flag is never inferred: a
 * customer that already consumed the free interconnect for the Region and provider pays the
 * listed rate.</p>
 *
 * <h2>Validation</h2>
 * <p>{@code build()} throws {@link IllegalArgumentException} when a provider is {@code null},
 * both providers are the same, {@code bandwidthMbps <= 0}, or {@code pathTier} is outside 1-5.</p>
 */
@Value
public class MulticloudLinkRequest {

    /** One end of the link. */
    CloudProviderType providerA;

    /** {@code providerA}'s region in its own notation, or {@code null} when not stated. */
    String regionA;

    /** The other end of the link; never equal to {@code providerA}. */
    CloudProviderType providerZ;

    /** {@code providerZ}'s region in its own notation, or {@code null} when not stated. */
    String regionZ;

    /** The link size in Mbps; always positive. */
    int bandwidthMbps;

    /** The AWS connectivity-scope tier, 1-5. */
    int pathTier;

    /** The commitment term, or {@code null}. Hourly on-demand products ignore it. */
    Term term;

    /** Whether the caller opted in to the AWS free tier. */
    boolean useAwsFreeTier;

    @Builder(toBuilder = true)
    private MulticloudLinkRequest(CloudProviderType providerA, String regionA,
                                  CloudProviderType providerZ, String regionZ,
                                  int bandwidthMbps, Integer pathTier, Term term, boolean useAwsFreeTier) {
        if (providerA == null || providerZ == null) {
            throw new IllegalArgumentException("both providers of a multicloud link are required");
        }
        if (providerA == providerZ) {
            throw new IllegalArgumentException(
                    "a multicloud link connects two different providers; both ends are " + providerA);
        }
        if (bandwidthMbps <= 0) {
            throw new IllegalArgumentException("bandwidthMbps must be positive: " + bandwidthMbps);
        }
        int tier = pathTier == null ? MulticloudLinkQuote.DEFAULT_PATH_TIER : pathTier;
        if (tier < MulticloudLinkQuote.MIN_PATH_TIER || tier > MulticloudLinkQuote.MAX_PATH_TIER) {
            throw new IllegalArgumentException("pathTier must be between "
                    + MulticloudLinkQuote.MIN_PATH_TIER + " and " + MulticloudLinkQuote.MAX_PATH_TIER
                    + ": " + tier);
        }
        this.providerA = providerA;
        this.regionA = blankToNull(regionA);
        this.providerZ = providerZ;
        this.regionZ = blankToNull(regionZ);
        this.bandwidthMbps = bandwidthMbps;
        this.pathTier = tier;
        this.term = term;
        this.useAwsFreeTier = useAwsFreeTier;
    }

    /**
     * Whether either end of the link is the given provider.
     *
     * @param provider the provider to test
     * @return {@code true} when {@code provider} is {@code providerA} or {@code providerZ}
     */
    public boolean involves(CloudProviderType provider) {
        return provider != null && (provider == providerA || provider == providerZ);
    }

    /**
     * Maps a specification-style provider id to the {@code CloudProviderType} the value layer
     * keys prices by.
     *
     * @param ref a provider reference from {@code core.model.multicloud}
     * @return {@code AWS} for {@code aws}, {@code GOOGLE_CLOUD} for {@code gcp},
     *         {@code ORACLE_CLOUD} for {@code oci}, {@code AZURE} for {@code azure}; empty for
     *         {@code null}, {@code equinix} and every other id
     */
    public static Optional<CloudProviderType> cloudProviderOf(ProviderRef ref) {
        if (ref == null) {
            return Optional.empty();
        }
        if (ProviderRef.AWS.equals(ref)) {
            return Optional.of(CloudProviderType.AWS);
        }
        if (ProviderRef.GCP.equals(ref)) {
            return Optional.of(CloudProviderType.GOOGLE_CLOUD);
        }
        if (ProviderRef.OCI.equals(ref)) {
            return Optional.of(CloudProviderType.ORACLE_CLOUD);
        }
        if (ProviderRef.AZURE.equals(ref)) {
            return Optional.of(CloudProviderType.AZURE);
        }
        return Optional.empty();
    }

    /**
     * The inverse of {@link #cloudProviderOf(ProviderRef)}.
     *
     * @param provider a cloud provider
     * @return the specification-style provider id, or empty for {@code null} and for providers
     *         the specification vocabulary has no well-known id for
     */
    public static Optional<ProviderRef> providerRefOf(CloudProviderType provider) {
        if (provider == null) {
            return Optional.empty();
        }
        switch (provider) {
            case AWS:
                return Optional.of(ProviderRef.AWS);
            case GOOGLE_CLOUD:
                return Optional.of(ProviderRef.GCP);
            case ORACLE_CLOUD:
                return Optional.of(ProviderRef.OCI);
            case AZURE:
                return Optional.of(ProviderRef.AZURE);
            default:
                return Optional.empty();
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
