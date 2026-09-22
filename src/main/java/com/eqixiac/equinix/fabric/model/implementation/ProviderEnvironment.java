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

package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.ProviderEnvironmentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * The Fabric v4 {@code ProviderEnvironment} schema: a provider environment of an
 * {@code IC_PROFILE} service profile. It appears in three places in the catalog:
 * {@code ServiceProfile.environments}, {@code AccessPoint.environment}, and the {@code data} array
 * of {@code GET /fabric/v4/serviceProfiles/{serviceProfileId}/environments}.
 *
 * <p><b>Beta</b>: the schema carries the catalog's Beta marker (catalog fetched 2026-09-21).</p>
 *
 * <table>
 *   <caption>Properties</caption>
 *   <tr><th>Getter</th><th>Catalog property</th><th>Catalog description / example</th></tr>
 *   <tr><td>{@code getHref()}</td><td>{@code href} (uri, read-only)</td><td>"Provider Environment URI"</td></tr>
 *   <tr><td>{@code getUuid()}</td><td>{@code uuid} (uuid)</td><td>"Equinix-assigned provider environment identifier"</td></tr>
 *   <tr><td>{@code getType()}</td><td>{@code type}</td><td>{@code ProviderEnvironmentTypeEnum}; one value, {@code IC_ENV}</td></tr>
 *   <tr><td>{@code getName()}</td><td>{@code name}</td><td>"Provider environment name"</td></tr>
 *   <tr><td>{@code getDescription()}</td><td>{@code description}</td><td>"Provider environment description"</td></tr>
 *   <tr><td>{@code getRegion()}</td><td>{@code region}</td><td>"Cloud provider region identifier", e.g. {@code us-west-1}</td></tr>
 *   <tr><td>{@code getSupportedBandwidths()}</td><td>{@code supportedBandwidths} (integer[])</td><td>"Supported bandwidths in Mbps", e.g. {@code [1000, 10000, 100000]}</td></tr>
 *   <tr><td>{@code getMetros()}</td><td>{@code metros} ({@code ServiceMetro[]})</td><td>"Derived response attribute."</td></tr>
 *   <tr><td>{@code getSupportedFeatures()}</td><td>{@code supportedFeatures} (string[])</td><td>"Supported Feature Types", e.g. {@code [FEATURE_TYPE_L3_BASE]}</td></tr>
 *   <tr><td>{@code getChangeLog()}</td><td>{@code changeLog}</td><td>{@code Changelog}</td></tr>
 * </table>
 *
 * <p>{@code supportedFeatures} is typed {@code string[]} in the catalog, not an enum, so it is kept
 * as strings here. List getters return {@code null} when the response omits the property; the
 * catalog's {@code ServiceProfileEnvironmentsResponse} example omits {@code supportedFeatures} and
 * {@code changeLog}.</p>
 *
 * <p>The same catalog example publishes two shapes for a {@code metros} item: the
 * {@code ServiceMetro} shape ({@code code}, {@code name}, {@code displayName}) and a reference
 * shape ({@code href}, {@code metroCode}, {@code type}). {@link MetroRef} reads {@code metroCode}
 * as an alias of {@code code}, so {@code metroId()} is populated for both.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderEnvironment {

    @JsonProperty("href")
    private String href;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private ProviderEnvironmentType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    /** Cloud provider region identifier, in the provider's own notation (e.g. {@code us-west-1}). */
    @JsonProperty("region")
    private String region;

    /** Supported bandwidths in Mbps. */
    @JsonProperty("supportedBandwidths")
    private List<Integer> supportedBandwidths;

    /** Equinix metros serving this environment. Derived response attribute. */
    @JsonProperty("metros")
    private List<ServiceMetro> metros;

    /** Supported feature type literals as sent by the API (e.g. {@code FEATURE_TYPE_L3_BASE}). */
    @JsonProperty("supportedFeatures")
    private List<String> supportedFeatures;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
