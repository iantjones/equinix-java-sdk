/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.core.model.multicloud;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A specification environment seen from the customer side: the pair of provider sites a
 * connection joins, the environment id both providers agreed on, the connection sizes the
 * environment supports and its visibility ({@code environment.yaml#/Environment}).
 *
 * <p><b>Beta</b>: derived from the open specification at commit {@code bbfc763} (see the package
 * documentation).</p>
 *
 * <p>The specification's example: AWS {@code us-east-1} and GCP {@code us-east4} joined by the
 * environment id {@code aws-gcp-us-east}, addressed as
 * {@code providers/aws/environments/aws-gcp-us-east} by one provider and
 * {@code providers/gcp/environments/aws-gcp-us-east} by the other
 * ({@code docs/Protocols.md}).</p>
 *
 * <table>
 *   <caption>Fields</caption>
 *   <tr><th>Field</th><th>Specification field</th><th>Constraint</th></tr>
 *   <tr><td>{@code environmentId}</td><td>last segment of {@code name}</td>
 *       <td>required, not blank, no {@code /}; trimmed; case is preserved</td></tr>
 *   <tr><td>{@code providerSites}</td><td>{@code providerSites}</td>
 *       <td>exactly two entries with different providers; stored in canonical order (by
 *       provider id), unmodifiable</td></tr>
 *   <tr><td>{@code supportedConnectionSizeMbps}</td><td>{@code supportedConnectionSizeMbps}</td>
 *       <td>Mbps; {@code null} becomes {@link BandwidthTier#none()}</td></tr>
 *   <tr><td>{@code visibility}</td><td>{@code environmentVisibility}</td>
 *       <td>{@code null} becomes {@link EnvironmentVisibility#UNKNOWN}</td></tr>
 * </table>
 *
 * <h2>Order-insensitivity</h2>
 * <p>The two sites are an unordered pair. The constructor sorts them by provider id, so two
 * instances built with the sites in either order are equal, have the same hash code and the same
 * {@code toString()}. {@link #connects(ProviderRef, String, ProviderRef, String)} accepts its two
 * endpoints in either order.</p>
 *
 * <h2>Omitted specification fields</h2>
 * <p>{@code apiUri}, {@code supportedFeatures}, {@code deferralTimeoutHours} and
 * {@code deferConnectionProvisioning} describe provider-to-provider coordination and are not
 * modelled.</p>
 *
 * <p>Construct with {@link #of(String, ProviderSite, ProviderSite)} or the Lombok builder
 * ({@code EnvironmentRef.builder().environmentId(...).providerSite(...).providerSite(...)
 * .supportedConnectionSizeMbps(...).visibility(...).build()}).</p>
 *
 * @author ianjones
 */
@Value
public class EnvironmentRef {

    private static final String ENVIRONMENTS_SEGMENT = "environments";

    private static final Comparator<ProviderSite> CANONICAL_ORDER =
            Comparator.comparing(ProviderSite::getProviderRef);

    /** The environment id both providers agreed on, for example {@code aws-gcp-us-east}. */
    String environmentId;

    /** The two provider sites, sorted by provider id. Unmodifiable; always size 2. */
    List<ProviderSite> providerSites;

    /** The connection sizes, in Mbps, supported by the environment. Never null; may be empty. */
    BandwidthTier supportedConnectionSizeMbps;

    /** The environment's visibility. Never null; {@code UNKNOWN} when none was supplied. */
    EnvironmentVisibility visibility;

    @Builder
    private EnvironmentRef(String environmentId,
                           @Singular List<ProviderSite> providerSites,
                           BandwidthTier supportedConnectionSizeMbps,
                           EnvironmentVisibility visibility) {
        Objects.requireNonNull(environmentId, "environmentId");
        String trimmedId = environmentId.trim();
        if (trimmedId.isEmpty()) {
            throw new IllegalArgumentException("environmentId must not be blank");
        }
        if (trimmedId.indexOf('/') >= 0) {
            throw new IllegalArgumentException(
                    "environmentId must be the bare id, not a resource name: '" + environmentId + "'");
        }
        if (providerSites == null || providerSites.size() != 2) {
            throw new IllegalArgumentException("an environment joins exactly two provider sites, got "
                    + (providerSites == null ? 0 : providerSites.size()));
        }
        ProviderSite one = Objects.requireNonNull(providerSites.get(0), "providerSites[0]");
        ProviderSite two = Objects.requireNonNull(providerSites.get(1), "providerSites[1]");
        if (one.getProviderRef().equals(two.getProviderRef())) {
            throw new IllegalArgumentException(
                    "an environment joins two different providers, got '" + one.getProviderRef() + "' twice");
        }
        List<ProviderSite> sorted = new ArrayList<>(List.of(one, two));
        sorted.sort(CANONICAL_ORDER);

        this.environmentId = trimmedId;
        this.providerSites = List.copyOf(sorted);
        this.supportedConnectionSizeMbps =
                supportedConnectionSizeMbps == null ? BandwidthTier.none() : supportedConnectionSizeMbps;
        this.visibility = visibility == null ? EnvironmentVisibility.UNKNOWN : visibility;
    }

    /**
     * Creates an environment reference with no published sizes and {@code UNKNOWN} visibility.
     * The two sites are an unordered pair; swapping them produces an equal instance.
     *
     * @param environmentId the agreed environment id
     * @param oneSide one provider site
     * @param otherSide the other provider site; its provider must differ from {@code oneSide}'s
     * @return the environment reference
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code environmentId} is blank or contains {@code /}, or
     *         both sites name the same provider
     */
    public static EnvironmentRef of(String environmentId, ProviderSite oneSide, ProviderSite otherSide) {
        Objects.requireNonNull(oneSide, "oneSide");
        Objects.requireNonNull(otherSide, "otherSide");
        return new EnvironmentRef(environmentId, List.of(oneSide, otherSide), null, null);
    }

    /**
     * @param provider a provider
     * @return {@code true} when {@code provider} is one of the two providers of this environment
     */
    public boolean involves(ProviderRef provider) {
        return siteOf(provider).isPresent();
    }

    /**
     * @param provider a provider
     * @return that provider's site name in this environment, or empty when {@code provider} is
     *         null or is not one of the two providers
     */
    public Optional<String> siteOf(ProviderRef provider) {
        for (ProviderSite providerSite : providerSites) {
            if (providerSite.getProviderRef().equals(provider)) {
                return Optional.of(providerSite.getSite());
            }
        }
        return Optional.empty();
    }

    /**
     * @param provider one of the two providers of this environment
     * @return the other provider, or empty when {@code provider} is null or is not one of the two
     *         providers
     */
    public Optional<ProviderRef> otherSide(ProviderRef provider) {
        ProviderRef first = providerSites.get(0).getProviderRef();
        ProviderRef second = providerSites.get(1).getProviderRef();
        if (first.equals(provider)) {
            return Optional.of(second);
        }
        if (second.equals(provider)) {
            return Optional.of(first);
        }
        return Optional.empty();
    }

    /**
     * Tests whether this environment joins the two given provider sites. The endpoints may be
     * given in either order. Site names are compared after trimming and without regard to case
     * (see {@link ProviderSite#matches(ProviderRef, String)}).
     *
     * @param a one provider
     * @param aSite the site name at {@code a}
     * @param z the other provider
     * @param zSite the site name at {@code z}
     * @return {@code true} when one site of this environment matches {@code (a, aSite)} and the
     *         other matches {@code (z, zSite)}; {@code false} when any argument is null
     */
    public boolean connects(ProviderRef a, String aSite, ProviderRef z, String zSite) {
        ProviderSite first = providerSites.get(0);
        ProviderSite second = providerSites.get(1);
        return (first.matches(a, aSite) && second.matches(z, zSite))
                || (first.matches(z, zSite) && second.matches(a, aSite));
    }

    /**
     * The specification resource name of this environment as addressed under one provider's
     * prefix: {@code providers/{provider}/environments/{environmentId}}.
     *
     * @param addressedAs the provider whose id forms the {@code providers/} prefix; must be one of
     *                    the two providers of this environment
     * @return the resource name
     * @throws NullPointerException if {@code addressedAs} is null
     * @throws IllegalArgumentException if {@code addressedAs} is not one of the two providers
     */
    public String resourceName(ProviderRef addressedAs) {
        Objects.requireNonNull(addressedAs, "addressedAs");
        if (!involves(addressedAs)) {
            throw new IllegalArgumentException(
                    "provider '" + addressedAs + "' is not part of environment '" + environmentId + "'");
        }
        return addressedAs.resourceName() + "/" + ENVIRONMENTS_SEGMENT + "/" + environmentId;
    }

    /**
     * Tests whether an environment URI, such as an activation key's
     * {@code destinationEnvironmentUri}, names this environment. The URI matches when its
     * environment id (see
     * {@link #environmentIdOf(String)}) equals this {@code environmentId} exactly. The
     * {@code providers/{provider}} segment is not compared: the specification addresses one
     * environment under either provider's prefix and does not state which prefix an activation
     * key carries.
     *
     * @param environmentUri a resource name or URL containing {@code environments/{id}}
     * @return {@code true} when the URI's environment id equals this environment's id;
     *         {@code false} when {@code environmentUri} is null or carries no environment id
     */
    public boolean matchesUri(String environmentUri) {
        return environmentIdOf(environmentUri).map(environmentId::equals).orElse(false);
    }

    /**
     * Extracts the environment id from a resource name or URL: the path segment that follows the
     * last {@code environments} segment. Query strings and fragments are ignored.
     *
     * <table>
     *   <caption>Examples</caption>
     *   <tr><th>Input</th><th>Result</th></tr>
     *   <tr><td>{@code providers/aws/environments/aws-gcp-us-east}</td><td>{@code aws-gcp-us-east}</td></tr>
     *   <tr><td>{@code /providers/gcp/environments/aws-gcp-us-east/interconnects/i1}</td>
     *       <td>{@code aws-gcp-us-east}</td></tr>
     *   <tr><td>{@code https://host/v1/providers/aws/environments/e1?x=1}</td><td>{@code e1}</td></tr>
     *   <tr><td>{@code providers/aws}, {@code environments/}, {@code null}</td><td>empty</td></tr>
     * </table>
     *
     * @param environmentUri a resource name or URL; may be null
     * @return the environment id, or empty when none is present
     */
    public static Optional<String> environmentIdOf(String environmentUri) {
        if (environmentUri == null) {
            return Optional.empty();
        }
        String path = environmentUri.trim();
        int cut = indexOfFirst(path, '?', '#');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        String[] segments = path.split("/");
        for (int i = segments.length - 2; i >= 0; i--) {
            if (ENVIRONMENTS_SEGMENT.equals(segments[i]) && !segments[i + 1].isEmpty()) {
                return Optional.of(segments[i + 1]);
            }
        }
        return Optional.empty();
    }

    private static int indexOfFirst(String s, char a, char b) {
        int ia = s.indexOf(a);
        int ib = s.indexOf(b);
        if (ia < 0) {
            return ib;
        }
        if (ib < 0) {
            return ia;
        }
        return Math.min(ia, ib);
    }
}
