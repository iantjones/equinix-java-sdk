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

import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.design.optimizer.enums.RedundancyTier;
import com.eqixiac.equinix.design.optimizer.enums.WorkloadType;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesignSpecTest {

    private static DesignSpec sample() {
        return DesignSpec.builder()
                .name("payments-eu")
                .description("Card processing, EU and UK")
                .site(SiteIntent.builder().label("London HQ").metro("LD").headcount(400).build())
                .site(SiteIntent.builder().label("Frankfurt").latitude(50.11).longitude(8.68).weight(2.0).build())
                .cloud(CloudIntent.builder().id("aws").provider(CloudProviderType.AWS).region("eu-west-2")
                        .monthlyEgressGb(50_000d).build())
                .workload(WorkloadIntent.builder().label("api").type(WorkloadType.TRANSACTIONAL).bandwidthMbps(1000)
                        .clouds(List.of("aws")).build())
                .peer(new PeerIntent(15169, "Google"))
                .constraints(DesignConstraints.builder().maxMetros(2).geoZones(List.of("UK", "EU"))
                        .requiredRegions(List.of(Region.EMEA)).minimumRedundancy(RedundancyTier.N_PLUS_1).build())
                .objectives(new DesignObjectives("latency_first", Map.of("cost", 0.3)))
                .commercial(CommercialTerms.builder().currency("EUR").budgetMaxMonthly(new BigDecimal("20000"))
                        .term(Term.MONTH_12).annualGrowthRate(0.25).build())
                .label("owner", "network-team")
                .build();
    }

    @Test
    @DisplayName("a complete spec validates clean and applies defaults")
    void validSpec() {
        DesignSpec spec = sample();
        assertEquals(List.of(), spec.validate());
        assertEquals(DesignSpec.SCHEMA_VERSION, spec.schemaVersion());
        assertEquals("aws", spec.cloud("aws").id());
        assertTrue(spec.cloud("aws").required(), "an unset required flag means required");
        assertEquals(12, spec.commercial().horizonMonthsOrDefault());
        spec.requireValid();
    }

    @Test
    @DisplayName("a minimal spec gets non-null defaults for every section")
    void defaults() {
        DesignSpec spec = DesignSpec.builder().name("x")
                .workload(WorkloadIntent.builder().label("w").bandwidthMbps(10).build()).build();
        assertEquals(DesignConstraints.none(), spec.constraints());
        assertEquals("balanced", spec.objectives().profile());
        assertEquals("USD", spec.commercial().currencyOrDefault());
        assertTrue(spec.sites().isEmpty() && spec.clouds().isEmpty() && spec.labels().isEmpty());
        List<SpecIssue> issues = spec.validate();
        assertEquals(1, issues.size(), "only the no-sites warning: " + issues);
        assertEquals(SpecIssue.Severity.WARNING, issues.get(0).severity());
    }

    @Test
    @DisplayName("JSON and YAML round-trip to an equal spec with the same fingerprint")
    void roundTrip(@TempDir Path dir) {
        DesignSpec spec = sample();
        DesignSpec viaJson = DesignSpecIO.fromJson(DesignSpecIO.toJson(spec));
        DesignSpec viaYaml = DesignSpecIO.fromYaml(DesignSpecIO.toYaml(spec));
        assertEquals(spec, viaJson);
        assertEquals(spec, viaYaml);
        assertEquals(spec.fingerprint(), viaYaml.fingerprint());
        assertEquals(64, spec.fingerprint().length());

        Path yaml = dir.resolve("design.yaml");
        Path json = dir.resolve("design.json");
        DesignSpecIO.write(spec, yaml);
        DesignSpecIO.write(spec, json);
        assertEquals(spec, DesignSpecIO.read(yaml));
        assertEquals(spec, DesignSpecIO.read(json));
    }

    @Test
    @DisplayName("the fingerprint ignores map entry order and changes with content")
    void fingerprint() {
        Map<String, Double> ab = new LinkedHashMap<>();
        ab.put("cost", 1.0);
        ab.put("latency", 2.0);
        Map<String, Double> ba = new LinkedHashMap<>();
        ba.put("latency", 2.0);
        ba.put("cost", 1.0);
        DesignSpec one = sample().toBuilder().objectives(new DesignObjectives(null, ab)).build();
        DesignSpec two = sample().toBuilder().objectives(new DesignObjectives(null, ba)).build();
        assertEquals(one.fingerprint(), two.fingerprint());
        assertNotEquals(one.fingerprint(), sample().toBuilder().name("other").build().fingerprint());
    }

    @Test
    @DisplayName("a hand-written YAML spec reads, case-insensitive enums included")
    void readsHandWrittenYaml() {
        DesignSpec spec = DesignSpecIO.fromYaml("""
                name: edge
                sites:
                  - label: HQ
                    metro: DC
                    role: headquarters
                clouds:
                  - id: az
                    provider: azure
                workloads:
                  - label: web
                    bandwidthMbps: 500
                    clouds: [az]
                constraints:
                  maxMetros: 3
                  excludedRegions: [apac]
                """);
        assertEquals(List.of(), spec.validate().stream()
                .filter(i -> i.severity() == SpecIssue.Severity.ERROR).collect(Collectors.toList()));
        assertEquals(CloudProviderType.AZURE, spec.clouds().get(0).provider());
        assertEquals(List.of(Region.APAC), spec.constraints().excludedRegions());
    }

    @Test
    @DisplayName("an unknown property is an error that names the property, not a silent drop")
    void unknownPropertyRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DesignSpecIO.fromYaml("name: x\nworkloadz: []\n"));
        assertTrue(e.getMessage().contains("workloadz"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> DesignSpecIO.fromJson(" "));
    }

    @Test
    @DisplayName("validation reports every error with its path")
    void validationErrors() {
        DesignSpec bad = DesignSpec.builder()
                .schemaVersion("9")
                .site(SiteIntent.builder().label("A").metro("london").latitude(95.0).build())
                .site(SiteIntent.builder().label("A").weight(-1.0).build())
                .cloud(CloudIntent.builder().id("c").monthlyEgressGb(-5d).build())
                .workload(WorkloadIntent.builder().label("w").bandwidthMbps(0).clouds(List.of("missing")).build())
                .peer(new PeerIntent(0, null))
                .constraints(DesignConstraints.builder().minMetros(3).maxMetros(2)
                        .requiredMetros(List.of("DC")).excludedMetros(List.of("DC")).build())
                .objectives(new DesignObjectives(null, Map.of("cost", -1.0)))
                .commercial(CommercialTerms.builder().currency("ZZZZ").budgetMaxMonthly(BigDecimal.ZERO).build())
                .build();

        String all = bad.validate().stream().map(SpecIssue::toString).collect(Collectors.joining("\n"));
        for (String expected : List.of("schemaVersion", "name:", "sites[0].metro", "sites[0].latitude",
                "sites[0]: give both latitude and longitude", "sites[1].label: duplicate", "sites[1].weight",
                "clouds[0]: name a provider", "clouds[0].monthlyEgressGb", "workloads[0].bandwidthMbps",
                "workloads[0].clouds[0]", "peers[0].asn", "both required and excluded", "minMetros (3) exceeds",
                "objectives.weights.cost", "commercial.currency", "commercial.budgetMaxMonthly")) {
            assertTrue(all.contains(expected), "missing '" + expected + "' in:\n" + all);
        }
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, bad::requireValid);
        assertTrue(e.getMessage().contains("sites[0].metro"), e.getMessage());
    }

    @Test
    @DisplayName("a spec that connects nothing is an error; collections are immutable")
    void emptyAndImmutable() {
        DesignSpec empty = DesignSpec.builder().name("nothing").build();
        assertFalse(empty.validate().isEmpty());
        assertThrows(IllegalArgumentException.class, empty::requireValid);
        assertThrows(UnsupportedOperationException.class, () -> sample().sites().add(null));
        assertThrows(UnsupportedOperationException.class, () -> sample().labels().put("a", "b"));
    }
}
