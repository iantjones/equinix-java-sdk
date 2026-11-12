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

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.spec.DesignSpecIO;
import com.eqixiac.equinix.design.value.ratecard.ColocationItem;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.EgressRate;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.client.Metros;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.MetroRegistry;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.eqixiac.equinix.mcp.server.DesignFixtures.MAPPER;
import static com.eqixiac.equinix.mcp.server.DesignFixtures.SPEC_DC_AWS;
import static com.eqixiac.equinix.mcp.server.DesignFixtures.SPEC_TWO_METROS;
import static com.eqixiac.equinix.mcp.server.DesignFixtures.tool;
import static com.eqixiac.equinix.mcp.server.DesignFixtures.withSpec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Per-tool handler tests for the spec-driven {@code design_*} tools over the offline fixture in
 * {@link DesignFixtures}. No HTTP calls anywhere: live pricing fails fast against the stub and falls back
 * to the bundled reference figures; FX runs offline.
 */
@DisplayName("design_* tools over a DesignSpec (stubbed FabricGateway)")
class DesignToolsTest {

    private ServerContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = DesignFixtures.context();
    }

    private ObjectNode call(String name, String args) throws Exception {
        return DesignFixtures.call(name, context, args);
    }

    private static void assertProvenance(JsonNode payload) {
        JsonNode p = payload.get("provenance");
        assertNotNull(p, "every design result carries provenance: " + payload);
        assertTrue(p.hasNonNull("kind"), p.toString());
        assertTrue(p.get("sources").isArray() && p.get("sources").size() > 0, p.toString());
    }

    // spec tools

    @Nested
    @DisplayName("design_spec_template / design_validate_spec")
    class SpecToolsTests {

        @Test
        @DisplayName("the template is commented YAML that validates clean and carries its fingerprint")
        void templateValidates() throws Exception {
            ObjectNode payload = call("design_spec_template", "{}");
            String yaml = payload.get("template").asText();
            assertTrue(yaml.contains("# "), "the YAML template is commented");
            assertTrue(yaml.contains("ROUND-TRIP"), "it states the latency convention");
            String fingerprint = DesignSpecIO.fromYaml(yaml).requireValid().fingerprint();
            assertEquals(fingerprint, payload.get("spec_fingerprint").asText());
            assertEquals("LD", payload.get("spec").get("sites").get(0).get("metro").asText());
            assertFalse(payload.has("json_schema"), "the schema is opt-in");
            assertProvenance(payload);

            ObjectNode withSchema = call("design_spec_template", "{\"format\": \"json\", \"include_schema\": true}");
            assertTrue(withSchema.get("template").asText().startsWith("{"));
            assertEquals("object", withSchema.get("json_schema").get("type").asText());
            assertFalse(withSchema.get("json_schema").get("additionalProperties").asBoolean(),
                    "the schema is strict, as the reader is");
        }

        @Test
        @DisplayName("a valid spec (object or YAML text) returns the normalised spec and a stable fingerprint")
        void validSpec() throws Exception {
            ObjectNode fromObject = call("design_validate_spec", withSpec(SPEC_DC_AWS, null));
            assertTrue(fromObject.get("valid").asBoolean(), fromObject.toPrettyString());
            assertEquals(0, fromObject.get("error_count").asInt());
            assertEquals("1", fromObject.get("normalized_spec").get("schemaVersion").asText(), "defaults applied");
            String yaml = DesignSpecIO.toYaml(DesignSpecIO.fromJson(SPEC_DC_AWS));
            ObjectNode fromYaml = call("design_validate_spec", "{\"spec\": " + MAPPER.writeValueAsString(yaml) + "}");
            assertEquals("yaml", fromYaml.get("input_format").asText());
            assertEquals(fromObject.get("spec_fingerprint").asText(), fromYaml.get("spec_fingerprint").asText(),
                    "equal content, equal fingerprint, whatever the input format");
            assertProvenance(fromObject);
        }

        @Test
        @DisplayName("semantic errors carry the path, a JSON pointer and a concrete fix")
        void semanticErrors() throws Exception {
            ObjectNode payload = call("design_validate_spec", """
                    {"spec": {"name": "", "sites": [{"label": "HQ", "metro": "London"}],
                     "workloads": [{"label": "w", "bandwidthMbps": 100, "clouds": ["nope"]}]}}
                    """);
            assertFalse(payload.get("valid").asBoolean());
            JsonNode metro = issue(payload, "sites[0].metro");
            assertEquals("/sites/0/metro", metro.get("json_pointer").asText());
            assertTrue(metro.get("fix").asText().contains("LD"), metro.toString());
            JsonNode cloudRef = issue(payload, "workloads[0].clouds[0]");
            assertEquals("/workloads/0/clouds/0", cloudRef.get("json_pointer").asText());
            assertTrue(cloudRef.get("fix").asText().contains("clouds"), cloudRef.toString());
            assertTrue(issue(payload, "name").get("fix").asText().contains("\"name\""));
        }

        @Test
        @DisplayName("an unknown property is an error naming the valid properties")
        void unknownProperty() throws Exception {
            ObjectNode payload = call("design_validate_spec", """
                    {"spec": {"name": "x", "workloads": [{"label": "w", "bandwidth_mbps": 100}]}}
                    """);
            assertFalse(payload.get("valid").asBoolean());
            JsonNode issue = payload.get("issues").get(0);
            assertEquals("workloads[0].bandwidth_mbps", issue.get("path").asText(), issue.toString());
            assertTrue(issue.get("fix").asText().contains("bandwidthMbps"), issue.toString());
            assertTrue(payload.get("spec_fingerprint").isNull(), "an unreadable spec has no fingerprint");
        }

        @Test
        @DisplayName("a bad enum value lists the valid values")
        void badEnum() throws Exception {
            ObjectNode payload = call("design_validate_spec", """
                    {"spec": {"name": "x", "clouds": [{"id": "a", "provider": "AMAZON"}]}}
                    """);
            JsonNode issue = payload.get("issues").get(0);
            assertEquals("clouds[0].provider", issue.get("path").asText(), issue.toString());
            assertTrue(issue.get("fix").asText().contains("AWS"), issue.toString());
        }

        @Test
        @DisplayName("engine tools refuse an invalid spec with every error, its path and fix")
        void enginesRefuseInvalidSpecs() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> call("design_optimize", "{\"spec\": {\"name\": \"x\"}}"));
            assertTrue(e.getMessage().contains("[workloads]"), e.getMessage());
            assertTrue(e.getMessage().contains("Fix:"), e.getMessage());
            IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                    () -> call("design_optimize", "{}"));
            assertTrue(missing.getMessage().contains("design_spec_template"), missing.getMessage());
        }

        private JsonNode issue(JsonNode payload, String path) {
            for (JsonNode i : payload.get("issues")) {
                if (i.get("path").asText().equals(path)) {
                    return i;
                }
            }
            throw new AssertionError("no issue at " + path + ": " + payload.get("issues").toPrettyString());
        }
    }

    // design_optimize

    @Nested
    @DisplayName("design_optimize")
    class OptimizeTests {

        @Test
        @DisplayName("recommends DC for a DC site needing AWS, with cost sources, fingerprint and provenance; stores the result")
        void recommends() throws Exception {
            ObjectNode payload = call("design_optimize", withSpec(SPEC_DC_AWS, null));
            assertEquals("recommended", payload.get("status").asText(), payload.toPrettyString());
            assertEquals("DC", payload.get("recommended").get("portfolio").asText());
            String fingerprint = DesignSpecIO.fromJson(SPEC_DC_AWS).fingerprint();
            assertEquals(fingerprint, payload.get("spec_fingerprint").asText());
            JsonNode cost = payload.get("recommended").get("cost");
            assertTrue(cost.has("price_basis"), cost.toString());
            assertTrue(payload.get("recommended").get("objectives").has("latency"));
            assertTrue(payload.has("pinned_spec"), "the pinned spec field is always present");
            assertTrue(payload.has("currency_conversions"));
            assertFalse(payload.has("sensitivity"), "summary leaves sensitivity for detail=standard");
            assertProvenance(payload);
            assertEquals(fingerprint, payload.get("provenance").get("spec_fingerprint").asText());
            assertTrue(context.designStore().optimization(fingerprint).found(), "the result is kept for chaining");
        }

        @Test
        @DisplayName("detail=standard adds sensitivity, cost lines, verdicts and placement; summary is smaller")
        void detailLevels() throws Exception {
            ObjectNode summary = call("design_optimize", withSpec(SPEC_TWO_METROS, null));
            ObjectNode standard = call("design_optimize", withSpec(SPEC_TWO_METROS, "\"detail\": \"standard\""));
            assertTrue(standard.has("sensitivity"), standard.toPrettyString());
            assertTrue(standard.has("constraint_verdicts"));
            assertTrue(standard.has("placement"));
            assertTrue(standard.get("recommended").get("cost").has("lines"));
            assertTrue(summary.toString().length() < standard.toString().length());
            assertTrue(summary.get("frontier").size() <= 3, "summary caps the frontier at 3");
            assertFalse(summary.get("summary").asText().contains("[truncated"),
                    "an engine summary is labelled untrusted but not cut at the 512-character field cap");
            if (summary.has("frontier_omitted")) {
                assertTrue(summary.get("frontier_omitted").asInt() > 0);
            }
        }

        @Test
        @DisplayName("an infeasible spec reports no_feasible_design and what to relax")
        void infeasible() throws Exception {
            ObjectNode payload = call("design_optimize", withSpec("""
                    {"name": "tight", "sites": [{"label": "HQ", "metro": "DC"}],
                     "workloads": [{"label": "edge", "bandwidthMbps": 100, "maxLatencyMs": 0.01}]}
                    """, null));
            assertEquals("no_feasible_design", payload.get("status").asText());
            assertFalse(payload.has("recommended"));
            assertTrue(payload.get("next_steps").get(0).asText().contains("Relax"));
        }

        @Test
        @Timeout(20)
        @DisplayName("a cancel mid-run ends the call promptly with ToolCallCancelledException")
        void cancellable() throws Exception {
            CountDownLatch entered = new CountDownLatch(1);
            Metros slow = mock(Metros.class);
            when(slow.list()).thenAnswer(inv -> {
                entered.countDown();
                Thread.sleep(30_000);
                return null;
            });
            FabricGateway fabric = mock(FabricGateway.class);
            when(fabric.metros()).thenReturn(slow);
            ServerContext ctx = ServerContext.builder().fabric(fabric).environment(Map.of()).build();
            CancellationSignal signal = CancellationSignal.create();
            Thread canceller = new Thread(() -> {
                try {
                    entered.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                signal.cancel("user pressed stop");
            });
            canceller.start();
            long start = System.nanoTime();
            ToolCallCancelledException e = assertThrows(ToolCallCancelledException.class, () ->
                    tool("design_optimize").getHandler().handle(MAPPER.readTree(withSpec(SPEC_DC_AWS, null)), ctx,
                            signal));
            long ms = (System.nanoTime() - start) / 1_000_000;
            assertTrue(ms < 5_000, "cancelled within seconds, not after the 30 s read: " + ms);
            assertTrue(e.getMessage().contains("user pressed stop"), e.getMessage());
        }
    }

    // design_analyze_resilience

    @Nested
    @DisplayName("design_analyze_resilience")
    class ResilienceTests {

        @Test
        @DisplayName("by fingerprint: reuses the stored optimization and reports verdicts, SPOFs and a Mermaid graph")
        void byFingerprint() throws Exception {
            ObjectNode optimized = call("design_optimize", withSpec(SPEC_DC_AWS, null));
            String fingerprint = optimized.get("spec_fingerprint").asText();
            ObjectNode payload = call("design_analyze_resilience", "{\"spec_fingerprint\": \"" + fingerprint + "\"}");
            assertEquals("DC", payload.get("portfolio").asText());
            assertEquals("stored design_optimize result", payload.get("optimization_source").asText());
            assertEquals(fingerprint, payload.get("spec_fingerprint").asText());
            assertNotNull(payload.get("n1").get("level"));
            assertTrue(payload.get("single_points_of_failure").size() > 0,
                    "a one-metro design has single points of failure: " + payload.toPrettyString());
            assertTrue(payload.get("graph_mermaid").asText().startsWith("graph")
                    || payload.get("graph_mermaid").asText().startsWith("flowchart"), payload.get("graph_mermaid").asText());
            assertTrue(payload.get("remediations").isArray());
            assertProvenance(payload);
        }

        @Test
        @DisplayName("a given portfolio and 'failures' explain what breaks, with round-trip latency of surviving paths")
        void failures() throws Exception {
            ObjectNode payload = call("design_analyze_resilience", withSpec(SPEC_TWO_METROS,
                    "\"portfolio\": [\"DC\", \"DA\"], \"failures\": [\"DC\", \"no-such-element\"], "
                            + "\"detail\": \"standard\""));
            assertEquals("DA+DC", payload.get("portfolio").asText());
            assertFalse(payload.has("optimization_source"), "no optimizer run for a given portfolio");
            JsonNode details = payload.get("failure_details");
            assertTrue(details.size() > 0, payload.toPrettyString());
            assertTrue(details.get(0).has("workloads"));
            assertEquals("no-such-element", payload.get("failures_unmatched").get(0).asText());
            assertTrue(payload.get("scenarios").size() > 0, "standard lists the scenario ids");
        }

        @Test
        @DisplayName("an unknown fingerprint says how to recover")
        void unknownFingerprint() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> call(
                    "design_analyze_resilience", "{\"spec_fingerprint\": \"" + "a".repeat(64) + "\"}"));
            assertTrue(e.getMessage().contains("design_optimize"), e.getMessage());
        }
    }

    // value tools

    @Nested
    @DisplayName("design_growth_and_terms / design_estimate_tco / design_compare_cloud_egress")
    class ValueToolTests {

        @Test
        @DisplayName("growth: tiers per item, term options with unpriced reasons, seeded scenarios")
        void growth() throws Exception {
            ObjectNode payload = call("design_growth_and_terms", withSpec("""
                    {"name": "g", "sites": [{"label": "HQ", "metro": "DC"}],
                     "clouds": [{"id": "aws", "provider": "AWS", "region": "us-east-1"}],
                     "workloads": [{"label": "web", "bandwidthMbps": 500, "clouds": ["aws"]}],
                     "commercial": {"annualGrowthRate": 0.5, "horizonMonths": 24}}
                    """, "\"portfolio\": [\"DC\"], \"scenarios\": {\"low_growth\": 0.1, \"high_growth\": 0.9, "
                    + "\"monte_carlo_runs\": 50, \"seed\": 7}"));
            assertEquals("DC", payload.get("portfolio").asText());
            assertEquals(24, payload.get("horizon_months").asInt());
            assertTrue(payload.get("items").size() > 0, payload.toPrettyString());
            JsonNode options = payload.get("items").get(0).get("term_options");
            assertEquals(4, options.size(), "on demand, 12, 24, 36 months");
            for (JsonNode o : options) {
                if (!o.get("priced").asBoolean()) {
                    assertFalse(o.get("unpriced_reason").asText().isBlank(), "an unpriced option says why");
                }
            }
            assertEquals(7, payload.get("scenarios").get("monte_carlo").get("seed").asInt());
            assertProvenance(payload);
        }

        @Test
        @DisplayName("growth: Monte Carlo without a seed is refused")
        void growthNeedsSeed() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                    call("design_growth_and_terms", withSpec(SPEC_DC_AWS, "\"portfolio\": [\"DC\"], "
                            + "\"scenarios\": {\"low_growth\": 0, \"high_growth\": 0.5, \"monte_carlo_runs\": 10}")));
            assertTrue(e.getMessage().contains("seed"), e.getMessage());
        }

        @Test
        @DisplayName("TCO prices the spec's cloud, with fingerprint, assumptions and archetypes")
        void tco() throws Exception {
            ObjectNode payload = call("design_estimate_tco", withSpec(SPEC_DC_AWS, "\"portfolio\": [\"DC\"]"));
            assertTrue(payload.get("breakdowns").size() >= 2, payload.toPrettyString());
            assertEquals(DesignSpecIO.fromJson(SPEC_DC_AWS).fingerprint(), payload.get("spec_fingerprint").asText());
            assertEquals("given", payload.get("portfolio_source").asText());
            assertTrue(payload.has("assumptions"));
            assertProvenance(payload);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                    call("design_estimate_tco", withSpec(SPEC_DC_AWS, "\"cloud_id\": \"azure\"")));
            assertTrue(e.getMessage().contains("Declared clouds: [aws]"), e.getMessage());
            IllegalArgumentException tier = assertThrows(IllegalArgumentException.class, () ->
                    call("design_estimate_tco", withSpec(SPEC_DC_AWS, "\"path_tier\": 2")));
            assertTrue(tier.getMessage().contains("peer_cloud_id"), tier.getMessage());
        }

        @Test
        @DisplayName("egress uses the live provider card when it answers in time")
        void egressLive() throws Exception {
            ServerContext live = ServerContext.builder()
                    .fabric(DesignFixtures.gateway())
                    .environment(Map.of())
                    .providerRateCardFactory((provider, ctx) -> Optional.of(cannedProviderCard()))
                    .build();
            ObjectNode payload = DesignFixtures.call("design_compare_cloud_egress", live, withSpec(SPEC_DC_AWS, null));
            assertEquals("AWS", payload.get("provider").asText());
            assertEquals(0.09, payload.get("internet_rate_per_gb").asDouble(), 1e-9, payload.toPrettyString());
            assertTrue(payload.get("live_pricing").get("attempted").asBoolean());
            assertFalse(payload.get("live_pricing").get("degraded").asBoolean());
            assertEquals("none: no portfolio given and no stored design_optimize result for this spec",
                    payload.get("portfolio_source").asText());
            assertProvenance(payload);
        }

        @Test
        @Timeout(20)
        @DisplayName("egress never hangs: a stalled provider degrades within the pricing budget and is named")
        void egressDegrades() throws Exception {
            RateCard stalled = new RateCard() {
                @Override
                public Optional<PriceQuote> connection(ConnectionType type, int mbps, MetroCode metro, Term term) {
                    return Optional.empty();
                }

                @Override
                public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                    return Optional.empty();
                }

                @Override
                public Optional<EgressRate> egress(CloudProviderType p, String region, EgressPath path, Term term) {
                    try {
                        Thread.sleep(60_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return Optional.empty();
                }

                @Override
                public PriceSource source() {
                    return PriceSource.PROVIDER_API;
                }
            };
            ServerContext ctx = ServerContext.builder()
                    .fabric(DesignFixtures.gateway())
                    .environment(Map.of(ServerContext.ENV_PRICING_TIMEOUT_MS, "250"))
                    .providerRateCardFactory((provider, c) -> Optional.of(stalled))
                    .build();
            ObjectNode payload = DesignFixtures.call("design_compare_cloud_egress", ctx, withSpec(SPEC_DC_AWS, null));
            assertTrue(payload.get("live_pricing").get("degraded").asBoolean(), payload.toPrettyString());
            assertTrue(payload.get("live_pricing").get("failures").get(0).asText().contains("AWS"));
        }
    }

    private static RateCard cannedProviderCard() {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int mbps, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<EgressRate> egress(CloudProviderType provider, String region, EgressPath path, Term term) {
                BigDecimal rate = path == EgressPath.INTERNET ? new BigDecimal("0.09") : new BigDecimal("0.02");
                return Optional.of(EgressRate.of(rate, Currency.getInstance("USD"), PriceSource.PROVIDER_API));
            }

            @Override
            public Optional<PriceQuote> colocation(ColocationItem item, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public PriceSource source() {
                return PriceSource.PROVIDER_API;
            }
        };
    }

    // latency and peering

    @Test
    @DisplayName("latency: a measured pair returns Fabric's round trip; the floor is always below it")
    void latencyMeasured() throws Exception {
        ObjectNode payload = call("design_estimate_latency", "{\"from\": \"DC\", \"to\": \"SV\"}");
        assertEquals("measured", payload.get("basis").asText());
        assertEquals(60.0, payload.get("measured_round_trip_ms").asDouble(), 1e-9);
        assertEquals(60.0, payload.get("estimated_round_trip_ms").asDouble(), 1e-9);
        assertTrue(payload.get("floor_round_trip_ms").asDouble() < 60.0);
        assertTrue(payload.get("distance_km").asDouble() > 3000);
        assertEquals("live", payload.get("provenance").get("kind").asText());
    }

    @Test
    @DisplayName("latency: an unmeasured pair is the floor times the route factor, marked estimated")
    void latencyEstimated() throws Exception {
        ServerContext ctx = ServerContext.builder()
                .metroRegistry(MetroRegistry.load(DesignFixtures.gateway().metros()))
                .environment(Map.of())
                .build();
        // DA lists DC, but a pair the stub does not list (DC -> DC) has no measured figure.
        ObjectNode payload = DesignFixtures.call("design_estimate_latency", ctx, "{\"from\": \"DC\", \"to\": \"DC\"}");
        assertEquals("estimated", payload.get("basis").asText());
        assertTrue(payload.get("measured_round_trip_ms").isNull());
        assertEquals("estimate", payload.get("provenance").get("kind").asText());
        assertThrows(IllegalArgumentException.class,
                () -> DesignFixtures.call("design_estimate_latency", ctx, "{\"from\": \"ZZ\", \"to\": \"DC\"}"));
    }

    @Test
    @DisplayName("peering: a spec without peers is refused with how to add them (no PeeringDB call)")
    void peeringNeedsPeers() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call("design_analyze_peering", withSpec(SPEC_DC_AWS, null)));
        assertTrue(e.getMessage().contains("peers"), e.getMessage());
    }

    // plan lifecycle

    @Nested
    @DisplayName("design_plan_deployment / design_get_plan / design_plan_drift / design_export_terraform")
    class PlanLifecycle {

        @Test
        @DisplayName("plans from a fingerprint, returns plan_id and document; get, export and the resource resolve it")
        void lifecycle() throws Exception {
            ObjectNode optimized = call("design_optimize", withSpec(SPEC_DC_AWS, null));
            String fingerprint = optimized.get("spec_fingerprint").asText();
            ObjectNode plan = call("design_plan_deployment", "{\"spec_fingerprint\": \"" + fingerprint + "\", "
                    + "\"deployment\": {\"notifications\": [\"noc@example.com\"], \"customer_asn\": 65001}}");
            assertFalse(plan.get("executed").asBoolean());
            String planId = plan.get("plan_id").asText();
            assertTrue(planId.matches("[0-9a-f]{16}"), planId);
            assertEquals(fingerprint, plan.get("spec_fingerprint").asText(), "the plan carries the spec fingerprint");
            assertEquals("stored design_optimize result", plan.get("optimization_source").asText());
            assertEquals("dc-aws", plan.get("deployment_name").asText(), "the spec name is the deployment name");
            assertTrue(plan.get("validation").get("valid").asBoolean(), plan.get("validation").toPrettyString());
            assertEquals(planId, plan.get("plan_document").get("planId").asText(), "the plan document is returned");
            assertTrue(plan.get("required_inputs").size() > 0);
            assertProvenance(plan);
            assertEquals(1, context.designStore().size(DesignStore.Kind.PLAN));

            ObjectNode got = call("design_get_plan", "{\"plan_id\": \"" + planId + "\", \"detail\": \"summary\"}");
            assertEquals(planId, got.get("plan_id").asText());
            assertFalse(got.has("plan_document"), "summary leaves the document out");

            ObjectNode export = call("design_export_terraform", "{\"plan_id\": \"" + planId + "\"}");
            assertTrue(export.get("terraform").asText().contains("equinix_fabric_cloud_router"),
                    export.get("terraform").asText());
            assertEquals(fingerprint, export.get("spec_fingerprint").asText());

            IllegalArgumentException drift = assertThrows(IllegalArgumentException.class,
                    () -> call("design_plan_drift", "{\"plan_id\": \"" + planId + "\"}"));
            assertTrue(drift.getMessage().contains("project_id"), "drift is refused without a project scope: "
                    + drift.getMessage());
        }

        @Test
        @DisplayName("planning the same spec again yields the same content-hash plan_id")
        void deterministicPlanId() throws Exception {
            String args = withSpec(SPEC_DC_AWS, "\"deployment\": {\"notifications\": [\"noc@example.com\"]}");
            String first = call("design_plan_deployment", args).get("plan_id").asText();
            String second = call("design_plan_deployment", args).get("plan_id").asText();
            assertEquals(first, second);
            assertEquals(1, context.designStore().size(DesignStore.Kind.PLAN));
        }

        @Test
        @DisplayName("an unknown or evicted plan_id explains what happened and what to do")
        void unknownPlan() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> call("design_export_terraform", "{\"plan_id\": \"0123456789abcdef\"}"));
            assertTrue(e.getMessage().contains("unknown to this server process"), e.getMessage());
            assertTrue(e.getMessage().contains("design_plan_deployment"), e.getMessage());
        }

        @Test
        @DisplayName("the plan description states the dryRun=true router POSTs and that nothing is provisioned")
        void planDescriptionStatesDryRun() {
            String description = tool("design_plan_deployment").getDescription();
            assertTrue(description.contains("dryRun=true"), description);
            assertTrue(description.contains("NOTHING is provisioned"), description);
            assertTrue(tool("design_plan_deployment").isReadOnly());
        }

        @Test
        @DisplayName("summary detail is much smaller than the default standard result")
        void planDetail() throws Exception {
            String args = withSpec(SPEC_DC_AWS, "\"deployment\": {\"notifications\": [\"noc@example.com\"]}");
            ObjectNode standard = call("design_plan_deployment", args);
            ObjectNode summary = call("design_get_plan", "{\"plan_id\": \"" + standard.get("plan_id").asText()
                    + "\", \"detail\": \"summary\"}");
            assertNotEquals(standard.toString().length(), summary.toString().length());
            assertTrue(summary.toString().length() * 2 < standard.toString().length(),
                    summary.toString().length() + " vs " + standard.toString().length());
        }
    }
}
