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

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.optimizer.enums.ComplianceZone;
import com.eqixiac.equinix.design.optimizer.enums.LatencySensitivity;
import com.eqixiac.equinix.design.optimizer.enums.RedundancyTier;
import com.eqixiac.equinix.design.optimizer.enums.SiteRole;
import com.eqixiac.equinix.design.optimizer.enums.WorkloadType;
import com.eqixiac.equinix.design.solve.WeightProfiles;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.eqixiac.equinix.mcp.server.Schemas.array;
import static com.eqixiac.equinix.mcp.server.Schemas.bool;
import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.mapOf;
import static com.eqixiac.equinix.mcp.server.Schemas.number;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.string;
import static com.eqixiac.equinix.mcp.server.Schemas.stringEnum;
import static com.eqixiac.equinix.mcp.server.Schemas.with;

/**
 * The JSON Schema (2020-12) of a {@code DesignSpec} document and the commented starter spec, both
 * written for a calling model. The schema mirrors the strict reader ({@code DesignSpecIO}): every
 * object is closed ({@code additionalProperties: false}) and every enum lists the values the reader
 * accepts (matching is case-insensitive). Semantic rules the schema cannot express (unique labels,
 * cloud references, min/max metro consistency) are checked by {@code design_validate_spec}.
 */
final class DesignSpecSchema {

    private static final String METRO_PATTERN = "^[A-Z]{2}$";

    private DesignSpecSchema() {
    }

    /**
     * @return the schema of a DesignSpec document
     */
    static Map<String, Object> schema() {
        Map<String, Object> metro = with(string("Equinix Fabric metro code: two upper-case letters, e.g. LD, NY, "
                + "DC, SV, FR."), "pattern", METRO_PATTERN);

        Map<String, Object> site = object(props(
                "label", string("Unique short name, e.g. 'London HQ'."),
                "metro", metro,
                "latitude", with(number("Decimal degrees; give with longitude when there is no metro."),
                        "minimum", -90, "maximum", 90),
                "longitude", with(number("Decimal degrees."), "minimum", -180, "maximum", 180),
                "role", stringEnum("What the site is; the role scales its weight.", names(SiteRole.class)),
                "headcount", with(integer("People at the site (informational)."), "minimum", 0),
                "weight", with(number("Relative importance in the latency objective; unweighted sites count as "
                        + "an average site."), "exclusiveMinimum", 0)),
                "label");

        Map<String, Object> workload = object(props(
                "label", string("Unique short name, e.g. 'analytics'."),
                "type", stringEnum("Workload archetype (sets default latency sensitivity and placement).",
                        names(WorkloadType.class)),
                "bandwidthMbps", with(integer("Sustained bandwidth in Mbps; sizes and prices the workload's cloud "
                        + "connections. Unsized workloads leave those connections unpriced."), "minimum", 1),
                "latencySensitivity", stringEnum("Descriptive latency class.", names(LatencySensitivity.class)),
                "maxLatencyMs", with(number("Hard ceiling on ROUND-TRIP ms from every site to this workload's metro. "
                        + "A hard constraint: designs that break it are infeasible."), "exclusiveMinimum", 0),
                "clouds", array("Ids of clouds (from clouds[].id) this workload must reach. Two or more clouds "
                        + "make a cloud-to-cloud flow.", string("A cloud id.")),
                "pinnedMetro", with(string("Metro this workload must run in."), "pattern", METRO_PATTERN)),
                "label");

        Map<String, Object> cloud = object(props(
                "id", string("Unique id referenced by workloads[].clouds, e.g. 'aws-use1'."),
                "provider", stringEnum("Cloud provider.", names(CloudProviderType.class)),
                "region", string("Provider region in the provider's notation, e.g. us-east-1, eastus, "
                        + "europe-west2."),
                "serviceProfileUuid", string("Exact Fabric service profile uuid (instead of provider)."),
                "serviceProfileName", string("Fabric service profile name (instead of provider)."),
                "required", bool("Whether a design must reach this cloud. Default true."),
                "monthlyEgressGb", with(number("Data leaving the cloud per month, GB; prices egress."), "minimum", 0),
                "monthlyIngressGb", with(number("Data entering the cloud per month, GB."), "minimum", 0)),
                "id");

        Map<String, Object> peer = object(props(
                "asn", with(integer("Autonomous system number of a network to peer with."), "minimum", 1,
                        "maximum", 4_294_967_295L),
                "label", string("Readable name of the network.")),
                "asn");

        Map<String, Object> region = stringEnum("Equinix region.", regionCodes());
        Map<String, Object> constraints = object(props(
                "requiredMetros", array("Metros every design must include.", metro),
                "excludedMetros", array("Metros no design may use.", metro),
                "requiredRegions", array("Regions a design must include.", region),
                "excludedRegions", array("Regions no design may use.", region),
                "complianceZones", array("Compliance zones every metro must satisfy.",
                        stringEnum("Compliance zone.", names(ComplianceZone.class))),
                "geoZones", array("Fabric geo zones/scopes every metro must belong to (live geoScopes; metros "
                        + "without them fall back to their country), e.g. EU, UK, US.", string("Geo zone.")),
                "minimumRedundancy", stringEnum("Minimum redundancy tier: none (1 metro), n_plus_1 (2 metros, two in "
                        + "one region), multi_metro (2 metros anywhere), multi_region (2 metros in 2 regions).",
                        names(RedundancyTier.class)),
                "maxLatencyMs", with(number("Ceiling on ROUND-TRIP ms from every site to the metro that serves it."),
                        "exclusiveMinimum", 0),
                "minMetros", with(integer("Fewest metros a design may use."), "minimum", 1),
                "maxMetros", with(integer("Most metros a design may use (default 3)."), "minimum", 1)));

        Map<String, Object> objectives = object(props(
                "profile", stringEnum("Named weight profile.", WeightProfiles.names().toArray(new String[0])),
                "weights", mapOf("Objective weights keyed by objective id (latency, cost, provider_coverage, "
                                + "resilience, compliance; also simulated_resilience, and carbon (Beta: country-level grid "
                                + "intensity, a siting indicator), each measured only when given a weight). Override the "
                                + "profile's weights; normalised to sum 1.",
                        with(number("Non-negative weight."), "minimum", 0))));

        Map<String, Object> exchangeRates = object(props(
                "rates", mapOf("Units of the spec currency per ONE unit of each keyed ISO currency, e.g. "
                                + "{\"GBP\": 1.27} with currency USD.",
                        with(number("Positive rate."), "exclusiveMinimum", 0)),
                "asOf", string("When the rates were observed, e.g. 2026-11-12."),
                "source", string("Where they came from, e.g. a treasury rate sheet or the ECB."),
                "bandPercent", with(number("FX allowance either side, percent (0 = exact)."), "minimum", 0,
                        "maximum", 50)),
                "rates", "asOf", "source");

        Map<String, Object> commercial = object(props(
                "currency", with(string("ISO 4217 currency every figure is compared in. Default USD."),
                        "pattern", "^[A-Z]{3}$"),
                "budgetMaxMonthly", with(number("Monthly budget ceiling in the spec currency (a hard constraint)."),
                        "exclusiveMinimum", 0),
                "budgetMinMonthly", with(number("Monthly budget floor (informational)."), "minimum", 0),
                "term", stringEnum("Commitment term for Equinix prices. Default on demand (MONTH_1).",
                        names(Term.class)),
                "annualGrowthRate", with(number("Annual traffic growth as a fraction, e.g. 0.25."),
                        "exclusiveMinimum", -1),
                "horizonMonths", with(integer("Projection horizon in months. Default 12."), "minimum", 1),
                "exchangeRates", exchangeRates));

        Map<String, Object> schema = object(props(
                "schemaVersion", with(string("Document format version. Default \"" + DesignSpec.SCHEMA_VERSION + "\"."),
                        "enum", new ArrayList<>(List.of(DesignSpec.SCHEMA_VERSION))),
                "name", string("Short design name; also the deployment name of a plan made from it."),
                "description", string("What the design is for."),
                "sites", array("Where users are. Sites drive latency (all ROUND-TRIP ms).", site),
                "workloads", array("What runs and what it must reach.", workload),
                "clouds", array("Clouds the design connects to.", cloud),
                "peers", array("Networks to peer with (ASNs); drives design_analyze_peering.", peer),
                "constraints", constraints,
                "objectives", objectives,
                "commercial", commercial,
                "labels", mapOf("Free-form string labels (part of the fingerprint).", string("Label value."))),
                "name");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        root.put("$id", DesignResources.SPEC_SCHEMA_URI);
        root.put("title", "Equinix DesignSpec v" + DesignSpec.SCHEMA_VERSION);
        root.put("description", "One design: sites, workloads, clouds, peers, constraints, objectives and commercial "
                + "terms. Strict: unknown properties are errors. All latency is ROUND-TRIP milliseconds. Its "
                + "fingerprint (SHA-256 of the canonical JSON) identifies results across tools.");
        root.putAll(schema);
        return root;
    }

    /**
     * The commented starter spec (YAML). It parses and validates clean: one London and one New York site,
     * an AWS us-east-1 cloud, a 1 Gbps workload and at most two metros.
     *
     * @return the YAML document
     */
    static String templateYaml() {
        return """
                # Equinix DesignSpec v1: the one input every design tool takes.
                # All latency is ROUND-TRIP milliseconds. Unknown properties are errors.
                # Validate with design_validate_spec; the JSON Schema is the resource equinix-design://schema/design-spec.
                schemaVersion: "1"
                name: ld-ny-aws            # required; a plan made from this spec uses it as its deployment name
                description: Two offices reaching AWS us-east-1 over Equinix Fabric
                sites:                     # where users are; drives the latency objective
                  - label: London office
                    metro: LD              # Fabric metro code (two letters), or give latitude + longitude instead
                    role: HEADQUARTERS     # optional; HEADQUARTERS, EMPLOYEE_HUB, PRIMARY_MARKET, DATA_CENTER, ...
                  - label: New York office
                    metro: NY
                    weight: 1.0            # optional relative importance (> 0)
                clouds:                    # clouds to reach; workloads reference them by id
                  - id: aws-use1
                    provider: AWS          # AWS, AZURE, GOOGLE_CLOUD, ORACLE_CLOUD, IBM_CLOUD, ALIBABA_CLOUD
                    region: us-east-1      # provider's own region name
                    monthlyEgressGb: 5000  # optional; prices cloud egress
                workloads:                 # what runs, how big, what it must reach
                  - label: analytics
                    type: GENERAL_COMPUTE  # optional archetype, e.g. TRANSACTIONAL, AI_ML_INFERENCE, DISASTER_RECOVERY
                    bandwidthMbps: 1000    # sustained Mbps; sizes and prices the cloud connection
                    clouds: [aws-use1]
                    # maxLatencyMs: 80     # optional HARD ceiling, round-trip ms from every site
                    # pinnedMetro: NY      # optional: force this workload into one metro
                # peers:                   # optional: networks to peer with (design_analyze_peering)
                #   - asn: 13335
                #     label: Cloudflare
                constraints:
                  maxMetros: 2             # most metros a design may use (default 3)
                  # minimumRedundancy: MULTI_METRO   # NONE, N_PLUS_1, MULTI_METRO, MULTI_REGION
                  # maxLatencyMs: 100                # ceiling, round-trip ms, site to serving metro
                  # excludedMetros: [SV]
                  # geoZones: [EU]                   # keep every metro inside these Fabric geo zones
                objectives:
                  profile: balanced        # balanced, latency_first, cost_first, redundancy_first, provider_coverage_first
                  # weights: {latency: 0.5, cost: 0.3}  # optional overrides, normalised to sum 1
                commercial:
                  currency: USD            # every figure is compared in this ISO 4217 currency
                  # budgetMaxMonthly: 5000 # HARD monthly ceiling in that currency
                  # term: MONTH_12         # MONTH_1 (on demand, default), MONTH_12, MONTH_24, MONTH_36
                  # annualGrowthRate: 0.25 # for design_growth_and_terms
                  # horizonMonths: 36
                  # exchangeRates:         # pin FX for reproducible results (design_optimize returns the rates it used)
                  #   rates: {GBP: 1.27}
                  #   asOf: "2026-11-12"
                  #   source: treasury sheet
                """;
    }

    private static String[] names(Class<? extends Enum<?>> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }

    private static String[] regionCodes() {
        return Arrays.stream(Region.values()).map(Region::getRegionCode).toArray(String[]::new);
    }
}
