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

/**
 * Value types for the vocabulary of the Connection Coordinator specification, an open
 * (Apache-2.0) provider-to-provider API for coordinating private connections between two cloud or
 * network providers.
 *
 * <p><b>Beta</b>. Every type in this package is derived from the specification source at
 * <a href="https://github.com/aws/Interconnect/tree/bbfc76398b1791320947dcbe003e90e2dcd03687/connection-coordinator">
 * github.com/aws/Interconnect, commit {@code bbfc763}</a> (2026-09-18), directory
 * {@code connection-coordinator/schemas}. The specification is still changing: the versioned
 * activation key and the connection-type enum were added in the 15 commits before that one. Field
 * names, enum values and the key format in this package can change in a minor SDK release to
 * follow it. Nothing here is derived from a published Equinix API.</p>
 *
 * <h2>Scope</h2>
 * <p>The specification is spoken between two <em>providers</em>. Its resource hierarchy is
 * {@code providers/{provider}/environments/{environment}/interconnects/{interconnect}} with
 * channels, connections and features beneath. Interconnects, channels, MACsec keys, features and
 * feature guidance are provider-internal; the specification states that the customer does not
 * choose or see them. This package does not model them, contains no HTTP client and calls no
 * endpoint.</p>
 *
 * <p>The customer-visible part is small, and it is what this package types:</p>
 * <ol>
 *   <li>At provider A the customer selects an environment (a pair of provider sites), a
 *       connection size in Mbps and the account id it uses at provider B. Provider A returns an
 *       activation key.</li>
 *   <li>The customer carries the activation key to provider B out of band and enters it there.
 *       Provider B reads the key, then confirms it with provider A
 *       ({@code ConfirmActivationKey}, provider-to-provider).</li>
 *   <li>The connection reports {@code VERIFICATION_STATE_UNVERIFIED} until both providers have
 *       finished, then {@code VERIFICATION_STATE_VERIFIED}.</li>
 * </ol>
 * <p>The customer-facing operations of steps 1 and 2 are provider-specific; the specification
 * does not name them.</p>
 *
 * <h2>Types</h2>
 * <table>
 *   <caption>Types and the specification schema each mirrors</caption>
 *   <tr><th>Type</th><th>Specification source</th><th>Purpose</th></tr>
 *   <tr><td>{@link com.eqixiac.equinix.core.model.multicloud.ProviderRef}</td>
 *       <td>{@code provider.yaml#/Provider.name}</td>
 *       <td>Normalized provider id; {@code resourceName()} is {@code providers/{id}}</td></tr>
 *   <tr><td>{@link com.eqixiac.equinix.core.model.multicloud.ProviderSite}</td>
 *       <td>{@code environment.yaml#/ProviderSites}</td>
 *       <td>A provider, its site name and a display name</td></tr>
 *   <tr><td>{@link com.eqixiac.equinix.core.model.multicloud.EnvironmentRef}</td>
 *       <td>{@code environment.yaml#/Environment}</td>
 *       <td>Unordered pair of provider sites, agreed environment id, supported sizes,
 *       visibility</td></tr>
 *   <tr><td>{@link com.eqixiac.equinix.core.model.multicloud.BandwidthTier}</td>
 *       <td>{@code Environment.supportedConnectionSizeMbps}</td>
 *       <td>Ascending set of sizes in Mbps; {@code coveringTier(int)} rounds a request up to the
 *       next published size</td></tr>
 *   <tr><td>{@link com.eqixiac.equinix.core.model.multicloud.ActivationKey}</td>
 *       <td>{@code environment.yaml#/ActivationKey}, {@code #/ActivationKeyV1},
 *       {@code #/ActivationKeyV2}</td>
 *       <td>Base64 JSON key codec; sealed over {@code V1}, {@code V2Encrypted},
 *       {@code Opaque}</td></tr>
 *   <tr><td>{@link com.eqixiac.equinix.core.model.multicloud.AdminState},
 *       {@link com.eqixiac.equinix.core.model.multicloud.VerificationState},
 *       {@link com.eqixiac.equinix.core.model.multicloud.ProvisioningState},
 *       {@link com.eqixiac.equinix.core.model.multicloud.MulticloudConnectionType}</td>
 *       <td>{@code common.yaml}</td>
 *       <td>Wire enums; constant names equal wire values; {@code UNKNOWN} read-side fallback</td></tr>
 *   <tr><td>{@link com.eqixiac.equinix.core.model.multicloud.EnvironmentVisibility}</td>
 *       <td>{@code environment.yaml#/EnvironmentVisibility}</td>
 *       <td>Wire enum; {@code UNKNOWN} read-side fallback</td></tr>
 * </table>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * EnvironmentRef env = EnvironmentRef.builder()
 *         .environmentId("aws-gcp-us-east")
 *         .providerSite(ProviderSite.of(ProviderRef.AWS, "us-east-1"))
 *         .providerSite(ProviderSite.of(ProviderRef.GCP, "us-east4"))
 *         .supportedConnectionSizeMbps(BandwidthTier.of(1000, 10000))
 *         .build();
 *
 * OptionalInt tier = env.getSupportedConnectionSizeMbps().coveringTier(3000);   // 10000
 *
 * ActivationKey key = ActivationKey.decode(keyFromProviderA);
 * String summary = switch (key) {
 *     case ActivationKey.V1 v1 -> v1.connectionSizeMbps() + " Mbps to " + v1.destinationEnvironmentUri();
 *     case ActivationKey.V2Encrypted v2 -> "encrypted, for " + v2.destinationEnvironmentUri();
 *     case ActivationKey.Opaque opaque -> "format not recognized";
 * };
 * boolean sameEnvironment = key.destinationEnvironmentId().map(env.getEnvironmentId()::equals).orElse(false);
 * String toEnterAtProviderB = key.encode();
 * }</pre>
 *
 * <h2>Placement and dependencies</h2>
 * <p>The package sits in {@code core} so that {@code fabric}, the {@code design} engines and any
 * later domain can share one vocabulary. It imports nothing outside {@code core}, the JDK, Jackson
 * and Lombok; a test enforces this. The Fabric enum {@code CloudProviderType} bridges to
 * {@link com.eqixiac.equinix.core.model.multicloud.ProviderRef} through
 * {@code ProviderRef.of(type.shortCode())}.</p>
 *
 * <h2>Naming</h2>
 * <p>No type here is named {@code Interconnect}. In this SDK and its sources that word already
 * denotes the specification's provider-internal redundancy group, the Fabric v4 {@code XF_IC}
 * resource, Google's Cloud Interconnect product, the Equinix Metal interconnection and the TCO
 * engine's private-path archetype. The connection-type enum is
 * {@code MulticloudConnectionType} because {@code fabric.enums.ConnectionType} exists.</p>
 *
 * <h2>Accessor style</h2>
 * <p>{@code ProviderSite} and {@code EnvironmentRef} are Lombok {@code @Value} types with
 * {@code getX()} accessors and serialize as JSON objects. {@code ProviderRef} and
 * {@code BandwidthTier} wrap one value, use {@code id()} / {@code tiersMbps()} and serialize as a
 * JSON string and a JSON array. {@code ActivationKey} variants deliberately have no JavaBean
 * getters, so Jackson does not serialize key material.</p>
 *
 * @see com.eqixiac.equinix.core.model.multicloud.ActivationKey
 * @see com.eqixiac.equinix.core.model.multicloud.EnvironmentRef
 */
package com.eqixiac.equinix.core.model.multicloud;
