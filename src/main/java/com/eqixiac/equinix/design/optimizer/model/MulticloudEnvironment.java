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

package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.core.model.multicloud.BandwidthTier;
import com.eqixiac.equinix.core.model.multicloud.EnvironmentRef;
import com.eqixiac.equinix.core.model.multicloud.ProviderRef;
import com.eqixiac.equinix.core.model.multicloud.ProviderSite;
import com.eqixiac.equinix.design.optimizer.enums.MulticloudEnvironmentStatus;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One native provider-to-provider multicloud environment: a pair of cloud regions that two
 * providers interconnect directly, with no Equinix resource on the path, together with the
 * availability status observed for it, the source of that observation and the observation date.
 *
 * <p><b>Beta</b>: models offerings that reached general availability in 2026 (AWS Interconnect -
 * multicloud, Google Cloud Partner Cross-Cloud Interconnect for AWS, Oracle Interconnect for AWS).
 * Entries are dated observations of provider documentation; see
 * {@link MulticloudEnvironmentCatalog}.</p>
 *
 * <table>
 *   <caption>Fields</caption>
 *   <tr><th>Field</th><th>Content</th><th>Constraint</th></tr>
 *   <tr><td>{@code environment}</td><td>the region pair as an {@link EnvironmentRef}: two
 *       {@link ProviderSite}s (provider id and the provider's own region name) and the connection
 *       sizes in Mbps confirmed for the pair</td><td>required</td></tr>
 *   <tr><td>{@code status}</td><td>{@link MulticloudEnvironmentStatus}</td>
 *       <td>{@code null} becomes {@code UNVERIFIED}</td></tr>
 *   <tr><td>{@code sourceUrls}</td><td>the provider pages the entry was read from, primary source
 *       first</td><td>unmodifiable; may be empty for a caller-supplied entry</td></tr>
 *   <tr><td>{@code asOf}</td><td>the date the sources were read, ISO-8601 ({@code 2026-09-21})</td>
 *       <td>{@code null} or a valid ISO-8601 date</td></tr>
 *   <tr><td>{@code sizesNote}</td><td>what the size list does and does not cover</td><td>optional</td></tr>
 *   <tr><td>{@code note}</td><td>other facts from the source (billing rules, preview limits)</td>
 *       <td>optional</td></tr>
 * </table>
 *
 * <h2>Environment id</h2>
 * <p>The providers do not publish the environment ids they agreed on under the Connection
 * Coordinator specification. {@link #of} therefore assigns a catalog-local id of the form
 * {@code {providerId}-{region}--{providerId}-{region}} with the two sides ordered by provider id
 * ({@code aws-us-east-1--gcp-us-east4}). It identifies the entry inside a catalog and is not a
 * value either provider's API accepts.</p>
 *
 * <h2>Provider mapping</h2>
 * <p>A {@link CloudProviderType} maps to the {@link ProviderRef} whose id equals
 * {@code CloudProviderType.shortCode()} ({@code aws}, {@code azure}, {@code gcp}, {@code oci},
 * {@code ibm}, {@code alibaba}). {@code CloudProviderType.OTHER} names no provider and is
 * rejected.</p>
 */
@Value
public class MulticloudEnvironment {

    /** The region pair, its catalog-local id and the connection sizes confirmed for it. */
    EnvironmentRef environment;

    /** The availability observed on {@code asOf}. Never null. */
    MulticloudEnvironmentStatus status;

    /** The pages the entry was read from, primary source first. Unmodifiable; never null. */
    List<String> sourceUrls;

    /** The ISO-8601 date the sources were read, or {@code null} when the supplier gave none. */
    String asOf;

    /** What the size list covers and omits, or {@code null}. */
    String sizesNote;

    /** Other facts from the source, or {@code null}. */
    String note;

    @Builder(toBuilder = true)
    private MulticloudEnvironment(EnvironmentRef environment, MulticloudEnvironmentStatus status,
                                  List<String> sourceUrls, String asOf, String sizesNote, String note) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.status = status == null ? MulticloudEnvironmentStatus.UNVERIFIED : status;
        this.sourceUrls = sourceUrls == null ? List.of() : List.copyOf(sourceUrls);
        if (asOf != null) {
            try {
                LocalDate.parse(asOf);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("asOf must be an ISO-8601 date such as 2026-09-21: '" + asOf + "'", e);
            }
        }
        this.asOf = asOf;
        this.sizesNote = sizesNote;
        this.note = note;
    }

    /**
     * Creates an entry for a pair of cloud regions, assigning the catalog-local environment id
     * described in the class documentation. The two sides are an unordered pair.
     *
     * @param a         one cloud
     * @param aRegion   its region in its own notation, for example {@code us-east-1}
     * @param z         the other cloud; must differ from {@code a}
     * @param zRegion   its region in its own notation, for example {@code us-east4}
     * @param status    the observed availability; {@code null} becomes {@code UNVERIFIED}
     * @param sizesMbps the connection sizes confirmed for the pair, in Mbps; {@code null} becomes
     *                  {@link BandwidthTier#none()}
     * @param sourceUrl the page the entry was read from; may be {@code null}
     * @param asOf      the ISO-8601 date the source was read; may be {@code null}
     * @return the entry
     * @throws IllegalArgumentException if a provider is {@code null} or {@code OTHER}, both
     *         providers are the same, a region is blank, or {@code asOf} is not an ISO-8601 date
     */
    public static MulticloudEnvironment of(CloudProviderType a, String aRegion,
                                           CloudProviderType z, String zRegion,
                                           MulticloudEnvironmentStatus status, BandwidthTier sizesMbps,
                                           String sourceUrl, String asOf) {
        ProviderRef refA = providerRefOf(a);
        ProviderRef refZ = providerRefOf(z);
        if (aRegion == null || aRegion.isBlank() || zRegion == null || zRegion.isBlank()) {
            throw new IllegalArgumentException("both regions of a multicloud environment are required");
        }
        ProviderSite siteA = ProviderSite.of(refA, aRegion);
        ProviderSite siteZ = ProviderSite.of(refZ, zRegion);
        EnvironmentRef ref = EnvironmentRef.builder()
                .environmentId(catalogId(siteA, siteZ))
                .providerSite(siteA)
                .providerSite(siteZ)
                .supportedConnectionSizeMbps(sizesMbps)
                .build();
        return MulticloudEnvironment.builder()
                .environment(ref)
                .status(status)
                .sourceUrls(sourceUrl == null ? List.of() : List.of(sourceUrl))
                .asOf(asOf)
                .build();
    }

    /**
     * The catalog-local id for a pair of provider sites, identical for either argument order.
     *
     * @param one   one provider site
     * @param other the other provider site
     * @return {@code {providerId}-{region}--{providerId}-{region}}, lower-case, sides ordered by
     *         provider id
     */
    public static String catalogId(ProviderSite one, ProviderSite other) {
        ProviderSite first = one.getProviderRef().compareTo(other.getProviderRef()) <= 0 ? one : other;
        ProviderSite second = first == one ? other : one;
        return (first.getProviderRef().id() + "-" + first.getSite() + "--"
                + second.getProviderRef().id() + "-" + second.getSite()).toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Tests whether this environment joins the two given cloud regions. The endpoints may be
     * given in either order; region names are compared after trimming, ignoring case.
     *
     * @param a       one cloud
     * @param aRegion its region
     * @param z       the other cloud
     * @param zRegion its region
     * @return {@code true} when the pair matches; {@code false} when any argument is {@code null}
     *         or a cloud has no provider id
     */
    public boolean connects(CloudProviderType a, String aRegion, CloudProviderType z, String zRegion) {
        Optional<ProviderRef> refA = optionalProviderRefOf(a);
        Optional<ProviderRef> refZ = optionalProviderRefOf(z);
        return refA.isPresent() && refZ.isPresent()
                && environment.connects(refA.get(), aRegion, refZ.get(), zRegion);
    }

    /**
     * @param a one cloud
     * @param z another cloud
     * @return {@code true} when the two providers of this environment are {@code a} and {@code z},
     *         in either order
     */
    public boolean joins(CloudProviderType a, CloudProviderType z) {
        Optional<ProviderRef> refA = optionalProviderRefOf(a);
        Optional<ProviderRef> refZ = optionalProviderRefOf(z);
        return refA.isPresent() && refZ.isPresent() && !refA.get().equals(refZ.get())
                && environment.involves(refA.get()) && environment.involves(refZ.get());
    }

    /**
     * @param provider a cloud
     * @return that cloud's region in this environment, or empty when the cloud is not one of the
     *         two providers
     */
    public Optional<String> regionOf(CloudProviderType provider) {
        return optionalProviderRefOf(provider).flatMap(environment::siteOf);
    }

    /**
     * The two clouds of this environment, in the environment's canonical order (by provider id).
     *
     * @return a two-element list; an element is empty when the provider id has no
     *         {@link CloudProviderType} (for example {@code equinix})
     */
    public List<Optional<CloudProviderType>> cloudProviders() {
        List<ProviderSite> sites = environment.getProviderSites();
        return List.of(cloudProviderOf(sites.get(0).getProviderRef()), cloudProviderOf(sites.get(1).getProviderRef()));
    }

    /**
     * The connection sizes confirmed for this pair, in Mbps.
     *
     * @return the sizes; empty when none was confirmed
     */
    public BandwidthTier sizesMbps() {
        return environment.getSupportedConnectionSizeMbps();
    }

    /**
     * @return the primary source URL, or empty when the entry has none
     */
    public Optional<String> primarySourceUrl() {
        return sourceUrls.isEmpty() ? Optional.empty() : Optional.of(sourceUrls.get(0));
    }

    /**
     * A one-line description: both sites, the status and the as-of date, for example
     * {@code aws us-east-1 <-> gcp us-east4 (GA, as of 2026-09-21)}.
     *
     * @return the description
     */
    public String describe() {
        List<ProviderSite> sites = environment.getProviderSites();
        return sites.get(0).getProviderRef() + " " + sites.get(0).getSite() + " <-> "
                + sites.get(1).getProviderRef() + " " + sites.get(1).getSite()
                + " (" + status + (asOf == null ? "" : ", as of " + asOf) + ")";
    }

    /**
     * Maps a cloud to its specification-style provider id through {@code shortCode()}.
     *
     * @param provider the cloud
     * @return the provider id
     * @throws IllegalArgumentException if {@code provider} is {@code null} or {@code OTHER}
     */
    public static ProviderRef providerRefOf(CloudProviderType provider) {
        return optionalProviderRefOf(provider).orElseThrow(() -> new IllegalArgumentException(
                "a multicloud environment needs a nameable cloud provider, got " + provider));
    }

    /**
     * The inverse of {@link #providerRefOf(CloudProviderType)}.
     *
     * @param ref a provider id
     * @return the cloud whose {@code shortCode()} equals the id; empty for {@code null} and for
     *         ids with no {@link CloudProviderType} constant
     */
    public static Optional<CloudProviderType> cloudProviderOf(ProviderRef ref) {
        if (ref == null) {
            return Optional.empty();
        }
        for (CloudProviderType type : CloudProviderType.values()) {
            if (type != CloudProviderType.OTHER && type.shortCode().equals(ref.id())) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private static Optional<ProviderRef> optionalProviderRefOf(CloudProviderType provider) {
        return provider == null || provider == CloudProviderType.OTHER
                ? Optional.empty()
                : Optional.of(ProviderRef.of(provider.shortCode()));
    }
}
