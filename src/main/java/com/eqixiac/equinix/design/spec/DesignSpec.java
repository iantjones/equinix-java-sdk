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

package com.eqixiac.equinix.design.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Singular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One design, stated as intent. See the package description for the role it plays.
 *
 * <p>Build one fluently:</p>
 * <pre>{@code
 * DesignSpec spec = DesignSpec.builder()
 *     .name("payments-eu")
 *     .site(SiteIntent.builder().label("London HQ").metro("LD").headcount(400).build())
 *     .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("eu-west-2")
 *             .monthlyEgressGb(50_000d).build())
 *     .workload(WorkloadIntent.builder().label("api").bandwidthMbps(1000).clouds(List.of("aws")).build())
 *     .constraints(DesignConstraints.builder().maxMetros(2).geoZones(List.of("UK", "EU")).build())
 *     .build();
 * spec.requireValid();
 * }</pre>
 *
 * <p>or load it from a file with {@link DesignSpecIO}. Instances are immutable; use
 * {@code toBuilder()} to derive a variant (for a what-if comparison).</p>
 *
 * @param schemaVersion  the document format version; {@value #SCHEMA_VERSION} is the only one this release reads
 * @param name           short identifier for the design
 * @param description    free text; may be {@code null}
 * @param sites          places to serve
 * @param workloads      what runs
 * @param clouds         clouds and service providers to reach
 * @param peers          networks to peer with
 * @param constraints    hard limits; never {@code null}
 * @param objectives     how options are ranked; never {@code null}
 * @param commercial     budget, currency, term and growth; never {@code null}
 * @param labels         free-form key/value tags carried through to results
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DesignSpec(String schemaVersion, String name, String description,
                         @Singular List<SiteIntent> sites,
                         @Singular List<WorkloadIntent> workloads,
                         @Singular List<CloudIntent> clouds,
                         @Singular List<PeerIntent> peers,
                         DesignConstraints constraints, DesignObjectives objectives, CommercialTerms commercial,
                         @Singular Map<String, String> labels) {

    /** The document format version this release reads and writes. */
    public static final String SCHEMA_VERSION = "1";

    private static final Pattern METRO_CODE = Pattern.compile("[A-Z]{2}");

    /** Applies defaults and makes every collection immutable and non-null. */
    public DesignSpec {
        schemaVersion = schemaVersion == null ? SCHEMA_VERSION : schemaVersion;
        sites = sites == null ? List.of() : List.copyOf(sites);
        workloads = workloads == null ? List.of() : List.copyOf(workloads);
        clouds = clouds == null ? List.of() : List.copyOf(clouds);
        peers = peers == null ? List.of() : List.copyOf(peers);
        constraints = constraints == null ? DesignConstraints.none() : constraints;
        objectives = objectives == null ? DesignObjectives.balanced() : objectives;
        commercial = commercial == null ? new CommercialTerms(null, null, null, null, null, null) : commercial;
        labels = labels == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(labels));
    }

    /**
     * Looks up a cloud by its id.
     *
     * @param id the {@link CloudIntent#id()}
     * @return the cloud, or {@code null} when the spec declares none with that id
     */
    public CloudIntent cloud(String id) {
        for (CloudIntent cloud : clouds) {
            if (cloud.id() != null && cloud.id().equals(id)) {
                return cloud;
            }
        }
        return null;
    }

    /**
     * A stable identity for this spec's content: the SHA-256 of its canonical JSON. Two specs with
     * the same content have the same fingerprint whatever order their map entries were added in.
     *
     * @return 64 lowercase hex characters
     */
    @JsonIgnore
    public String fingerprint() {
        return DesignSpecIO.fingerprint(this);
    }

    /**
     * Checks the document for problems an engine could not recover from (errors) and for gaps that
     * would leave part of a result unknown (warnings). Never throws.
     *
     * @return the issues found, errors first; empty when the spec is clean
     */
    public List<SpecIssue> validate() {
        List<SpecIssue> errors = new ArrayList<>();
        List<SpecIssue> warnings = new ArrayList<>();

        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            error(errors, "schemaVersion", "unsupported version '" + schemaVersion + "'; this release reads '"
                    + SCHEMA_VERSION + "'");
        }
        if (name == null || name.isBlank()) {
            error(errors, "name", "a design needs a name");
        }
        if (workloads.isEmpty() && clouds.isEmpty() && peers.isEmpty()) {
            error(errors, "workloads", "the design states nothing to connect: add a workload, a cloud or a peer");
        }

        Set<String> siteLabels = new HashSet<>();
        for (int i = 0; i < sites.size(); i++) {
            SiteIntent site = sites.get(i);
            String at = "sites[" + i + "]";
            if (site == null) {
                error(errors, at, "must not be null");
                continue;
            }
            if (blank(site.label())) {
                error(errors, at + ".label", "every site needs a label");
            } else if (!siteLabels.add(site.label())) {
                error(errors, at + ".label", "duplicate site label '" + site.label() + "'");
            }
            checkMetro(errors, at + ".metro", site.metro());
            if ((site.latitude() == null) != (site.longitude() == null)) {
                error(errors, at, "give both latitude and longitude, or neither");
            }
            if (site.latitude() != null && !(site.latitude() >= -90 && site.latitude() <= 90)) {
                error(errors, at + ".latitude", "must be between -90 and 90");
            }
            if (site.longitude() != null && !(site.longitude() >= -180 && site.longitude() <= 180)) {
                error(errors, at + ".longitude", "must be between -180 and 180");
            }
            if (site.weight() != null && !(site.weight() > 0 && Double.isFinite(site.weight()))) {
                error(errors, at + ".weight", "must be a positive, finite number");
            }
            if (site.headcount() != null && site.headcount() < 0) {
                error(errors, at + ".headcount", "must not be negative");
            }
            if (blank(site.metro()) && !site.hasCoordinates()) {
                warnings.add(new SpecIssue(SpecIssue.Severity.WARNING, at,
                        "no metro and no coordinates: latency to this site is unknown and it is left out of "
                                + "latency scoring and latency limits"));
            }
        }
        if (sites.isEmpty()) {
            warnings.add(new SpecIssue(SpecIssue.Severity.WARNING, "sites",
                    "no sites: latency cannot be evaluated, so it will not influence the result"));
        }

        Set<String> cloudIds = new HashSet<>();
        for (int i = 0; i < clouds.size(); i++) {
            CloudIntent cloud = clouds.get(i);
            String at = "clouds[" + i + "]";
            if (cloud == null) {
                error(errors, at, "must not be null");
                continue;
            }
            if (blank(cloud.id())) {
                error(errors, at + ".id", "every cloud needs an id");
            } else if (!cloudIds.add(cloud.id())) {
                error(errors, at + ".id", "duplicate cloud id '" + cloud.id() + "'");
            }
            if (cloud.provider() == null && blank(cloud.serviceProfileUuid()) && blank(cloud.serviceProfileName())) {
                error(errors, at, "name a provider, a serviceProfileUuid or a serviceProfileName");
            }
            nonNegative(errors, at + ".monthlyEgressGb", cloud.monthlyEgressGb());
            nonNegative(errors, at + ".monthlyIngressGb", cloud.monthlyIngressGb());
        }

        Set<String> workloadLabels = new HashSet<>();
        for (int i = 0; i < workloads.size(); i++) {
            WorkloadIntent workload = workloads.get(i);
            String at = "workloads[" + i + "]";
            if (workload == null) {
                error(errors, at, "must not be null");
                continue;
            }
            if (blank(workload.label())) {
                error(errors, at + ".label", "every workload needs a label");
            } else if (!workloadLabels.add(workload.label())) {
                error(errors, at + ".label", "duplicate workload label '" + workload.label() + "'");
            }
            if (workload.bandwidthMbps() != null && workload.bandwidthMbps() <= 0) {
                error(errors, at + ".bandwidthMbps", "must be positive");
            }
            if (workload.maxLatencyMs() != null
                    && !(workload.maxLatencyMs() > 0 && Double.isFinite(workload.maxLatencyMs()))) {
                error(errors, at + ".maxLatencyMs", "must be a positive, finite number");
            }
            checkMetro(errors, at + ".pinnedMetro", workload.pinnedMetro());
            for (int c = 0; c < workload.clouds().size(); c++) {
                String ref = workload.clouds().get(c);
                if (!cloudIds.contains(ref)) {
                    error(errors, at + ".clouds[" + c + "]", "no cloud with id '" + ref + "' is declared");
                }
            }
            if (workload.bandwidthMbps() == null) {
                warnings.add(new SpecIssue(SpecIssue.Severity.WARNING, at + ".bandwidthMbps",
                        "unsized workload: its connections cannot be priced"));
            }
        }

        Set<Long> asns = new HashSet<>();
        for (int i = 0; i < peers.size(); i++) {
            PeerIntent peer = peers.get(i);
            String at = "peers[" + i + "]";
            if (peer == null) {
                error(errors, at, "must not be null");
            } else if (peer.asn() <= 0 || peer.asn() > 4_294_967_295L) {
                error(errors, at + ".asn", "must be a valid autonomous system number (1 to 4294967295)");
            } else if (!asns.add(peer.asn())) {
                error(errors, at + ".asn", "duplicate ASN " + peer.asn());
            }
        }

        validateConstraints(errors);
        validateObjectives(errors);
        validateCommercial(errors);

        List<SpecIssue> all = new ArrayList<>(errors);
        all.addAll(warnings);
        return Collections.unmodifiableList(all);
    }

    /**
     * Throws when {@link #validate()} reports any error. Warnings do not throw.
     *
     * @return this spec, for chaining
     * @throws IllegalArgumentException listing every error
     */
    public DesignSpec requireValid() {
        List<String> errors = new ArrayList<>();
        for (SpecIssue issue : validate()) {
            if (issue.severity() == SpecIssue.Severity.ERROR) {
                errors.add(issue.path() + ": " + issue.message());
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid DesignSpec" + (blank(name) ? "" : " '" + name + "'") + ": "
                    + String.join("; ", errors));
        }
        return this;
    }

    private void validateConstraints(List<SpecIssue> errors) {
        DesignConstraints c = constraints;
        for (int i = 0; i < c.requiredMetros().size(); i++) {
            checkMetro(errors, "constraints.requiredMetros[" + i + "]", c.requiredMetros().get(i));
        }
        for (int i = 0; i < c.excludedMetros().size(); i++) {
            checkMetro(errors, "constraints.excludedMetros[" + i + "]", c.excludedMetros().get(i));
        }
        for (String metro : c.requiredMetros()) {
            if (c.excludedMetros().contains(metro)) {
                error(errors, "constraints", "metro '" + metro + "' is both required and excluded");
            }
        }
        for (var region : c.requiredRegions()) {
            if (c.excludedRegions().contains(region)) {
                error(errors, "constraints", "region " + region + " is both required and excluded");
            }
        }
        if (c.maxLatencyMs() != null && !(c.maxLatencyMs() > 0 && Double.isFinite(c.maxLatencyMs()))) {
            error(errors, "constraints.maxLatencyMs", "must be a positive, finite number");
        }
        if (c.minMetros() != null && c.minMetros() < 1) {
            error(errors, "constraints.minMetros", "must be at least 1");
        }
        if (c.maxMetros() != null && c.maxMetros() < 1) {
            error(errors, "constraints.maxMetros", "must be at least 1");
        }
        if (c.minMetros() != null && c.maxMetros() != null && c.minMetros() > c.maxMetros()) {
            error(errors, "constraints", "minMetros (" + c.minMetros() + ") exceeds maxMetros (" + c.maxMetros() + ")");
        }
        if (c.maxMetros() != null && c.requiredMetros().size() > c.maxMetros()) {
            error(errors, "constraints", c.requiredMetros().size() + " metros are required but maxMetros is "
                    + c.maxMetros());
        }
        if (c.minSurvivalPct() != null && !(c.minSurvivalPct() >= 0 && c.minSurvivalPct() <= 100)) {
            error(errors, "constraints.minSurvivalPct", "must be a percentage between 0 and 100");
        }
        if (c.maxMetros() != null && c.minimumRedundancy() != null
                && c.minimumRedundancy().getMinimumMetros() > c.maxMetros()) {
            error(errors, "constraints", "minimumRedundancy " + c.minimumRedundancy() + " needs at least "
                    + c.minimumRedundancy().getMinimumMetros() + " metros but maxMetros is " + c.maxMetros());
        }
    }

    private void validateObjectives(List<SpecIssue> errors) {
        double total = 0;
        for (Map.Entry<String, Double> weight : objectives.weights().entrySet()) {
            String at = "objectives.weights." + weight.getKey();
            if (blank(weight.getKey())) {
                error(errors, "objectives.weights", "an objective id must not be blank");
            }
            if (weight.getValue() == null || !(weight.getValue() >= 0) || !Double.isFinite(weight.getValue())) {
                error(errors, at, "must be a non-negative, finite number");
            } else {
                total += weight.getValue();
            }
        }
        if (blank(objectives.profile()) && !objectives.weights().isEmpty() && total == 0) {
            error(errors, "objectives.weights", "every weight is zero and no profile is named: nothing ranks options");
        }
    }

    private void validateCommercial(List<SpecIssue> errors) {
        CommercialTerms t = commercial;
        if (t.currency() != null) {
            try {
                java.util.Currency.getInstance(t.currency());
            } catch (IllegalArgumentException e) {
                error(errors, "commercial.currency", "'" + t.currency() + "' is not an ISO 4217 currency code");
            }
        }
        if (t.budgetMaxMonthly() != null && t.budgetMaxMonthly().signum() <= 0) {
            error(errors, "commercial.budgetMaxMonthly", "must be positive");
        }
        if (t.budgetMinMonthly() != null && t.budgetMinMonthly().signum() < 0) {
            error(errors, "commercial.budgetMinMonthly", "must not be negative");
        }
        if (t.budgetMaxMonthly() != null && t.budgetMinMonthly() != null
                && t.budgetMinMonthly().compareTo(t.budgetMaxMonthly()) > 0) {
            error(errors, "commercial", "budgetMinMonthly exceeds budgetMaxMonthly");
        }
        if (t.annualGrowthRate() != null && !(t.annualGrowthRate() > -1 && Double.isFinite(t.annualGrowthRate()))) {
            error(errors, "commercial.annualGrowthRate", "must be a finite fraction greater than -1");
        }
        if (t.horizonMonths() != null && t.horizonMonths() < 1) {
            error(errors, "commercial.horizonMonths", "must be at least 1");
        }
    }

    private static void checkMetro(List<SpecIssue> errors, String path, String metro) {
        if (metro != null && !METRO_CODE.matcher(metro).matches()) {
            error(errors, path, "'" + metro + "' is not an Equinix metro code (two capital letters, e.g. SV)");
        }
    }

    private static void nonNegative(List<SpecIssue> errors, String path, Double value) {
        if (value != null && !(value >= 0 && Double.isFinite(value))) {
            error(errors, path, "must be a non-negative, finite number");
        }
    }

    private static void error(List<SpecIssue> errors, String path, String message) {
        errors.add(new SpecIssue(SpecIssue.Severity.ERROR, path, message));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
