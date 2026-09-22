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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An immutable catalog of native provider-to-provider multicloud environments: the pairs of cloud
 * regions that two providers interconnect directly, with no Equinix resource on the path.
 *
 * <p><b>Beta</b>: the offerings catalogued here reached general availability in 2026.</p>
 *
 * <h2>The bundled catalog goes stale</h2>
 * <p>{@link #standard()} loads {@code /json/multicloud_environments_2026_09.json}, a dated copy of
 * the providers' public documentation. It is not refreshed at run time: this SDK calls no AWS,
 * Google Cloud, Oracle or Azure API. Providers add region pairs and change launch stages without
 * notice, so an absent pair means "not in this copy", not "not offered". Each entry carries the
 * URL it was read from and the date it was read; {@link #asOf()} is the catalog's own date. Supply
 * newer data with {@link #with(MulticloudEnvironment...)}, {@link #of(Collection)} or
 * {@link #load(InputStream)} and hand the result to
 * {@code DeploymentWizard.Builder.multicloudEnvironments(...)} or
 * {@code MetroOptimizer.Builder.multicloudEnvironments(...)}.</p>
 *
 * <table>
 *   <caption>Bundled entries (as of 2026-09-21)</caption>
 *   <tr><th>Pair</th><th>Entries</th><th>Status</th><th>Primary source</th></tr>
 *   <tr><td>AWS and Google Cloud</td><td>8 region pairs</td><td>{@code GA}</td>
 *       <td>{@code https://docs.aws.amazon.com/interconnect/latest/userguide/region-availability.html},
 *       confirmed against Google's paired-locations page</td></tr>
 *   <tr><td>AWS and Oracle Cloud</td><td>{@code us-east-1} and {@code us-ashburn-1}</td><td>{@code GA}</td>
 *       <td>the same AWS page, confirmed against
 *       {@code https://docs.oracle.com/en-us/iaas/Content/multicloud/interconnect-aws.htm}</td></tr>
 *   <tr><td>AWS and Microsoft Azure</td><td>4 region pairs</td><td>{@code PREVIEW}</td>
 *       <td>the same AWS page, which labels the provider "Preview"</td></tr>
 * </table>
 *
 * <h2>Lookup rules</h2>
 * <p>{@link #find} and {@link #regionsFor} treat the two clouds as an unordered pair. Region names
 * are compared after trimming, ignoring case. At most one entry exists per region pair: adding an
 * entry for a pair already present replaces it.</p>
 *
 * <h2>File format</h2>
 * <pre>{@code
 * { "asOf": "2026-09-21", "disclaimer": "...",
 *   "environments": [
 *     { "sites": [ {"provider": "aws", "site": "us-east-1", "displayName": "..."},
 *                  {"provider": "gcp", "site": "us-east4"} ],
 *       "status": "GA",
 *       "supportedConnectionSizeMbps": [1000, 10000],
 *       "sizesNote": "...", "sources": ["https://..."], "asOf": "2026-09-21", "note": "..." } ] }
 * }</pre>
 * <p>{@code provider} is a specification-style provider id ({@code aws}, {@code gcp}, {@code oci},
 * {@code azure}). A missing or unrecognized {@code status} loads as {@code UNVERIFIED}. An entry
 * without exactly two sites of different providers is rejected with an
 * {@link IllegalArgumentException} naming its index.</p>
 */
public final class MulticloudEnvironmentCatalog {

    private static final String RESOURCE = "/json/multicloud_environments_2026_09.json";

    private static final MulticloudEnvironmentCatalog EMPTY =
            new MulticloudEnvironmentCatalog(null, null, List.of());

    private static volatile MulticloudEnvironmentCatalog standard;

    private final String asOf;
    private final String disclaimer;
    private final List<MulticloudEnvironment> environments;

    private MulticloudEnvironmentCatalog(String asOf, String disclaimer, Collection<MulticloudEnvironment> entries) {
        this.asOf = asOf;
        this.disclaimer = disclaimer;
        // Keyed by the order-insensitive region-pair id so a later entry for the same pair replaces
        // the earlier one while keeping the earlier entry's position.
        Map<String, MulticloudEnvironment> byPair = new LinkedHashMap<>();
        for (MulticloudEnvironment entry : entries) {
            Objects.requireNonNull(entry, "catalog entries must not be null");
            byPair.put(pairKey(entry), entry);
        }
        this.environments = List.copyOf(byPair.values());
    }

    /**
     * The bundled catalog, loaded from the classpath on first use and shared afterwards. A failed
     * load is not retained: the next call reads the resource again.
     *
     * @return the bundled catalog
     * @throws IllegalStateException if the bundled resource is missing from the classpath
     * @throws UncheckedIOException  if it cannot be read
     */
    public static MulticloudEnvironmentCatalog standard() {
        MulticloudEnvironmentCatalog local = standard;
        if (local == null) {
            synchronized (MulticloudEnvironmentCatalog.class) {
                local = standard;
                if (local == null) {
                    try (InputStream in = MulticloudEnvironmentCatalog.class.getResourceAsStream(RESOURCE)) {
                        if (in == null) {
                            throw new IllegalStateException(
                                    "Bundled multicloud environment catalog not found on classpath: " + RESOURCE);
                        }
                        local = load(in);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to close multicloud environment catalog: " + RESOURCE, e);
                    }
                    standard = local;
                }
            }
        }
        return local;
    }

    /**
     * @return a catalog with no entries; every lookup on it is empty
     */
    public static MulticloudEnvironmentCatalog empty() {
        return EMPTY;
    }

    /**
     * Creates a catalog from caller-supplied entries, with no catalog-level date or disclaimer.
     *
     * @param environments the entries; a later entry for a region pair replaces an earlier one
     * @return the catalog
     * @throws NullPointerException if the collection or an element is {@code null}
     */
    public static MulticloudEnvironmentCatalog of(Collection<MulticloudEnvironment> environments) {
        Objects.requireNonNull(environments, "environments");
        return new MulticloudEnvironmentCatalog(null, null, environments);
    }

    /**
     * Reads a catalog in the file format given in the class documentation. The stream is read to
     * its end and not closed.
     *
     * @param json a UTF-8 JSON document
     * @return the catalog
     * @throws UncheckedIOException     if the stream cannot be read or is not JSON
     * @throws IllegalArgumentException if an entry is malformed; the message names the entry index
     */
    public static MulticloudEnvironmentCatalog load(InputStream json) {
        Objects.requireNonNull(json, "json");
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read multicloud environment catalog", e);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("a multicloud environment catalog is a JSON object");
        }
        List<MulticloudEnvironment> entries = new ArrayList<>();
        int index = 0;
        for (JsonNode node : root.path("environments")) {
            try {
                entries.add(parseEntry(node));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("multicloud environment catalog entry " + index
                        + " is malformed: " + e.getMessage(), e);
            }
            index++;
        }
        return new MulticloudEnvironmentCatalog(root.path("asOf").asText(null),
                root.path("disclaimer").asText(null), entries);
    }

    private static MulticloudEnvironment parseEntry(JsonNode node) {
        JsonNode sites = node.path("sites");
        if (!sites.isArray() || sites.size() != 2) {
            throw new IllegalArgumentException("'sites' must list exactly two provider sites");
        }
        ProviderSite one = parseSite(sites.get(0));
        ProviderSite two = parseSite(sites.get(1));

        List<Integer> sizes = new ArrayList<>();
        for (JsonNode size : node.path("supportedConnectionSizeMbps")) {
            if (size.isIntegralNumber()) {
                sizes.add(size.asInt());
            }
        }
        List<String> sources = new ArrayList<>();
        for (JsonNode source : node.path("sources")) {
            if (source.isTextual() && !source.asText().isBlank()) {
                sources.add(source.asText());
            }
        }
        EnvironmentRef ref = EnvironmentRef.builder()
                .environmentId(node.path("id").isTextual()
                        ? node.path("id").asText()
                        : MulticloudEnvironment.catalogId(one, two))
                .providerSite(one)
                .providerSite(two)
                .supportedConnectionSizeMbps(BandwidthTier.of(sizes))
                .build();
        return MulticloudEnvironment.builder()
                .environment(ref)
                .status(parseStatus(node.path("status").asText(null)))
                .sourceUrls(sources)
                .asOf(node.path("asOf").asText(null))
                .sizesNote(node.path("sizesNote").asText(null))
                .note(node.path("note").asText(null))
                .build();
    }

    private static ProviderSite parseSite(JsonNode node) {
        if (!node.path("provider").isTextual() || !node.path("site").isTextual()) {
            throw new IllegalArgumentException("each site needs a textual 'provider' and 'site'");
        }
        return ProviderSite.builder()
                .providerRef(ProviderRef.of(node.path("provider").asText()))
                .site(node.path("site").asText())
                .displayName(node.path("displayName").asText(null))
                .build();
    }

    private static MulticloudEnvironmentStatus parseStatus(String text) {
        if (text != null) {
            for (MulticloudEnvironmentStatus status : MulticloudEnvironmentStatus.values()) {
                if (status.name().equalsIgnoreCase(text.trim())) {
                    return status;
                }
            }
        }
        return MulticloudEnvironmentStatus.UNVERIFIED;
    }

    /**
     * Returns a catalog holding this catalog's entries plus the given ones. This catalog is
     * unchanged. An addition for a region pair already present replaces that entry in place; other
     * additions are appended in argument order. The catalog-level {@link #asOf()} and
     * {@link #disclaimer()} are carried over and describe the original entries only; each added
     * entry carries its own date.
     *
     * @param additions the entries to add or replace
     * @return the extended catalog
     * @throws NullPointerException if the array or an element is {@code null}
     */
    public MulticloudEnvironmentCatalog with(MulticloudEnvironment... additions) {
        Objects.requireNonNull(additions, "additions");
        List<MulticloudEnvironment> merged = new ArrayList<>(environments);
        merged.addAll(List.of(additions));
        return new MulticloudEnvironmentCatalog(asOf, disclaimer, merged);
    }

    /**
     * Finds the environment joining two cloud regions. The two endpoints may be given in either
     * order: {@code find(AWS, "us-east-1", GOOGLE_CLOUD, "us-east4")} and
     * {@code find(GOOGLE_CLOUD, "us-east4", AWS, "us-east-1")} return the same entry. Region names
     * are compared after trimming, ignoring case.
     *
     * @param a       one cloud
     * @param aRegion its region in its own notation
     * @param z       the other cloud
     * @param zRegion its region in its own notation
     * @return the entry, or empty when the catalog has none for the pair or any argument is
     *         {@code null}. Empty does not mean the providers offer no such link; see the class
     *         documentation
     */
    public Optional<MulticloudEnvironment> find(CloudProviderType a, String aRegion,
                                                CloudProviderType z, String zRegion) {
        if (a == null || z == null || aRegion == null || zRegion == null) {
            return Optional.empty();
        }
        return environments.stream()
                .filter(entry -> entry.connects(a, aRegion, z, zRegion))
                .findFirst();
    }

    /**
     * The region pairs the catalog holds between two clouds, in catalog order. The two clouds may
     * be given in either order.
     *
     * @param a one cloud
     * @param z the other cloud
     * @return the entries joining {@code a} and {@code z}, unmodifiable; empty when there is none,
     *         when either argument is {@code null}, or when both are the same cloud
     */
    public List<MulticloudEnvironment> regionsFor(CloudProviderType a, CloudProviderType z) {
        if (a == null || z == null || a == z) {
            return List.of();
        }
        return environments.stream()
                .filter(entry -> entry.joins(a, z))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * @return every entry, in catalog order; unmodifiable
     */
    public List<MulticloudEnvironment> environments() {
        return environments;
    }

    /**
     * @return {@code true} when the catalog has no entries
     */
    public boolean isEmpty() {
        return environments.isEmpty();
    }

    /**
     * The ISO-8601 date the catalog file as a whole was compiled ({@code 2026-09-21} for the
     * bundled catalog).
     *
     * @return the date, or {@code null} for a catalog built with {@link #of(Collection)} or
     *         {@link #empty()}
     */
    public String asOf() {
        return asOf;
    }

    /**
     * @return the catalog file's disclaimer, or {@code null} when it has none
     */
    public String disclaimer() {
        return disclaimer;
    }

    private static String pairKey(MulticloudEnvironment entry) {
        List<ProviderSite> sites = entry.getEnvironment().getProviderSites();
        return MulticloudEnvironment.catalogId(sites.get(0), sites.get(1)).toLowerCase(Locale.ROOT);
    }
}
