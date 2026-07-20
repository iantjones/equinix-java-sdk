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

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.core.http.response.PaginatedFilteredList;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.value.ratecard.EgressPath;
import api.equinix.javasdk.design.value.ratecard.EgressRate;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.design.value.ratecard.Term;
import api.equinix.javasdk.fabric.client.Metros;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.model.Metro;
import api.equinix.javasdk.fabric.model.MetroRegistry;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.ConnectedMetro;
import api.equinix.javasdk.fabric.model.implementation.GeoCoordinate;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.design.value.ratecard.ColocationItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Per-tool handler tests for the {@code design_*} catalog over a Mockito-stubbed
 * {@link FabricGateway} (three AMER metros, one AWS service profile — the same deterministic
 * fixture the optimizer engine tests use). No HTTP calls anywhere.
 */
@DisplayName("design_* tool handlers (stubbed FabricGateway)")
class DesignToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final double DC_LAT = 39.0438, DC_LON = -77.4874;
    private static final double DA_LAT = 32.7767, DA_LON = -96.7970;
    private static final double SV_LAT = 37.3382, SV_LON = -121.8863;

    private FabricGateway fabric;
    private Metros metros;
    private ServerContext context;

    @BeforeEach
    void stubGateway() throws Exception {
        Metro dc = metro("DC", "Ashburn", DC_LAT, DC_LON, List.of(
                connectedMetro("DA", 10.0), connectedMetro("SV", 60.0)));
        Metro da = metro("DA", "Dallas", DA_LAT, DA_LON, List.of(
                connectedMetro("DC", 10.0), connectedMetro("SV", 45.0)));
        Metro sv = metro("SV", "Silicon Valley", SV_LAT, SV_LON, List.of(
                connectedMetro("DC", 60.0), connectedMetro("DA", 45.0)));

        metros = mock(Metros.class);
        when(metros.list()).thenReturn(new PaginatedList<>(List.of(dc, da, sv), null, null, null, null));

        ServiceProfile awsProfile = mock(ServiceProfile.class);
        when(awsProfile.getUuid()).thenReturn("sp-aws-1");
        when(awsProfile.getName()).thenReturn("Amazon Web Services Direct Connect");
        when(awsProfile.metros()).thenReturn(List.of(
                serviceProfileMetro("DC", "us-east-1"),
                serviceProfileMetro("DA", "us-east-1")));

        ServiceProfiles serviceProfiles = mock(ServiceProfiles.class);
        when(serviceProfiles.search()).thenReturn(
                new PaginatedFilteredList<>(List.of(awsProfile), null, null, null, null));

        fabric = mock(FabricGateway.class);
        when(fabric.metros()).thenReturn(metros);
        when(fabric.serviceProfiles()).thenReturn(serviceProfiles);

        context = ServerContext.builder()
                .fabric(fabric)
                .metroRegistry(MetroRegistry.load(metros))
                .environment(Map.of())
                .build();
    }

    private static ToolRegistration tool(String name) {
        return EquinixMcpServer.catalog(EnumSet.of(Toolset.DESIGN)).stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("tool not in catalog: " + name));
    }

    private ObjectNode call(String name, String argsJson) throws Exception {
        JsonNode args = MAPPER.readTree(argsJson);
        return tool(name).getHandler().handle(args, context);
    }

    // ── design_optimize_placement ───────────────────────────────────────────

    @Test
    @DisplayName("design_optimize_placement ranks DC first with scores, reasons, and provenance")
    void optimizePlacement() throws Exception {
        ObjectNode payload = call("design_optimize_placement", """
                {"workloads": [{"label": "Web Tier", "type": "general_compute", "bandwidth_mbps": 1000}],
                 "sites": [{"label": "HQ", "metro_code": "DC"}],
                 "require_clouds": ["aws"],
                 "strategy": "balanced"}
                """);

        assertEquals("DC", payload.get("recommendations").get(0).get("metro").asText(),
                "DC leads: site anchor + AWS present: " + payload.toPrettyString());
        JsonNode first = payload.get("recommendations").get(0);
        assertEquals(1, first.get("rank").asInt());
        assertTrue(first.get("composite_score").asDouble() > 0);
        assertTrue(first.get("score_components").size() > 0, "per-dimension scores are included");
        assertTrue(first.get("reasons").size() > 0, "reasons are included");
        assertNotNull(payload.get("cost_estimate"), "a cost estimate is included");
        assertNotNull(payload.get("cost_estimate").get("price_source"), "cost carries provenance");
        assertNotNull(payload.get("explanation"), "the methodology explanation is included");
        assertFalse(payload.get("summary").asText().isEmpty());

        // SV lacks the required AWS profile, so it must not be recommended.
        for (JsonNode rec : payload.get("recommendations")) {
            assertFalse(rec.get("metro").asText().equals("SV"), "SV is filtered by the AWS requirement");
        }
    }

    @Test
    @DisplayName("design_optimize_placement without workloads is an LLM-correctable error")
    void optimizeRequiresWorkloads() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call("design_optimize_placement", "{}"));
        assertTrue(e.getMessage().contains("workloads"), e.getMessage());
    }

    // ── design_plan_deployment + design_export_terraform ────────────────────

    @Test
    @DisplayName("design_plan_deployment plans (never executes) and design_export_terraform round-trips the plan_id")
    void planThenExport() throws Exception {
        ObjectNode plan = call("design_plan_deployment", """
                {"optimization": {
                    "workloads": [{"label": "Web Tier", "type": "general_compute", "bandwidth_mbps": 1000}],
                    "sites": [{"label": "HQ", "metro_code": "DC"}],
                    "require_clouds": ["aws"],
                    "constraints": {"max_metros": 1}},
                 "deployment": {"customer_asn": 65001, "router_package": "STANDARD"}}
                """);

        assertFalse(plan.get("executed").asBoolean(), "the wizard runs in plan-only mode");
        String planId = plan.get("plan_id").asText();
        assertTrue(planId.startsWith("plan-"), planId);
        assertTrue(plan.get("plan_id_scope").asText().contains("this MCP server process"),
                "single-process scope is documented in the payload");
        assertTrue(plan.get("cloud_routers").size() > 0, "at least one Cloud Router is planned");
        assertTrue(plan.get("provider_connections").size() > 0, "the AWS connection is planned");
        assertNotNull(plan.get("pricing"), "plan pricing is included");
        assertNotNull(plan.get("pricing").get("price_source"), "pricing carries provenance");
        assertNotNull(plan.get("validation_errors"), "validation findings are surfaced");
        assertEquals(1, context.planStore().size(), "the plan is retained for export");

        ObjectNode export = call("design_export_terraform", "{\"plan_id\": \"" + planId + "\"}");
        assertEquals(planId, export.get("plan_id").asText());
        assertEquals("terraform-hcl", export.get("format").asText());
        assertTrue(export.get("terraform").asText().contains("equinix"),
                "the HCL targets the equinix provider: " + export.get("terraform").asText());
    }

    @Test
    @DisplayName("design_export_terraform with an unknown plan_id explains the single-process TTL")
    void exportUnknownPlan() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call("design_export_terraform", "{\"plan_id\": \"plan-999\"}"));
        assertTrue(e.getMessage().contains("design_plan_deployment"), e.getMessage());
    }

    // ── design_estimate_latency ─────────────────────────────────────────────

    @Test
    @DisplayName("design_estimate_latency estimates DC↔SV round trip with method caveats")
    void estimateLatency() throws Exception {
        ObjectNode payload = call("design_estimate_latency", "{\"from\": \"DC\", \"to\": \"SV\"}");

        double km = payload.get("distance_km").asDouble();
        double ms = payload.get("estimated_latency_ms").asDouble();
        assertTrue(km > 3000 && km < 4500, "DC-SV great-circle distance is ~3,600 km: " + km);
        assertTrue(ms > 30 && ms < 60, "DC-SV fibre RTT lower bound is ~35 ms: " + ms);
        assertEquals("round_trip", payload.get("mode").asText());
        assertEquals("metro", payload.get("from").get("kind").asText());
        assertTrue(payload.get("method").asText().contains("refractive index"));
        assertTrue(payload.get("caveats").size() >= 2, "caveats are stated");

        ObjectNode oneWay = call("design_estimate_latency",
                "{\"from\": \"DC\", \"to\": \"SV\", \"mode\": \"one_way\"}");
        assertTrue(oneWay.get("estimated_latency_ms").asDouble() < ms,
                "one-way is below the round trip");
    }

    @Test
    @DisplayName("design_estimate_latency rejects unknown endpoints helpfully")
    void latencyUnknownEndpoint() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> call("design_estimate_latency", "{\"from\": \"DC\", \"to\": \"XX\"}"));
        assertTrue(e.getMessage().contains("XX"), e.getMessage());
    }

    // ── design_estimate_tco ─────────────────────────────────────────────────

    @Test
    @DisplayName("design_estimate_tco returns per-archetype breakdowns with line items and provenance")
    void estimateTco() throws Exception {
        ObjectNode payload = call("design_estimate_tco", """
                {"monthly_egress_gb": 50000, "cloud": "aws", "region": "us-east-1",
                 "metro_code": "DC", "bandwidth_mbps": 1000}
                """);

        assertTrue(payload.get("breakdowns").size() >= 2, "multiple archetypes are compared");
        for (JsonNode breakdown : payload.get("breakdowns")) {
            assertNotNull(breakdown.get("archetype"));
            assertNotNull(breakdown.get("monthly_total"));
            assertNotNull(breakdown.get("line_items"), "line items are itemized");
        }
        assertNotNull(payload.get("recommended_archetype"));
        assertNotNull(payload.get("currency"));
        assertNotNull(payload.get("disclaimer"));
        assertTrue(payload.get("price_provenance_note").asText().contains("price sources"));
    }

    // ── design_compare_cloud_egress ─────────────────────────────────────────

    /** A canned "live" provider card serving both egress paths instantly. */
    private static RateCard cannedProviderCard() {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<EgressRate> egress(CloudProviderType provider, String region,
                                               EgressPath path, Term term) {
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

    @Test
    @DisplayName("design_compare_cloud_egress uses the live provider card when it answers in time")
    void compareCloudEgressLive() throws Exception {
        ServerContext liveContext = ServerContext.builder()
                .fabric(fabric)
                .environment(Map.of())
                .providerRateCardFactory((provider, ctx) -> Optional.of(cannedProviderCard()))
                .build();

        ObjectNode payload = tool("design_compare_cloud_egress").getHandler().handle(MAPPER.readTree("""
                {"monthly_egress_gb": 10000, "cloud": "aws", "region": "us-east-1"}
                """), liveContext);

        assertEquals("AWS", payload.get("provider").asText());
        assertEquals(0.09, payload.get("internet_rate_per_gb").asDouble(), 1e-9,
                "the live card's internet rate is used: " + payload.toPrettyString());
        assertEquals(0.02, payload.get("private_rate_per_gb").asDouble(), 1e-9);
        assertTrue(payload.get("live_pricing").get("attempted").asBoolean());
        assertFalse(payload.get("live_pricing").get("degraded").asBoolean());
        assertNotNull(payload.get("monthly_egress_savings"));
        assertNotNull(payload.get("disclaimer"));
    }

    @Test
    @Timeout(20)
    @DisplayName("design_compare_cloud_egress never hangs: a stalled provider degrades and is named")
    void compareCloudEgressDegrades() throws Exception {
        RateCard stalled = new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
                return Optional.empty();
            }

            @Override
            public Optional<EgressRate> egress(CloudProviderType provider, String region,
                                               EgressPath path, Term term) {
                try {
                    Thread.sleep(60_000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Optional.empty();
            }

            @Override
            public PriceSource source() {
                return PriceSource.PROVIDER_API;
            }
        };

        ServerContext stalledContext = ServerContext.builder()
                .fabric(fabric)
                .environment(Map.of(ServerContext.ENV_PRICING_TIMEOUT_MS, "250"))
                .providerRateCardFactory((provider, ctx) -> Optional.of(stalled))
                .build();

        long start = System.nanoTime();
        ObjectNode payload = tool("design_compare_cloud_egress").getHandler().handle(MAPPER.readTree("""
                {"monthly_egress_gb": 10000, "cloud": "aws", "region": "us-east-1"}
                """), stalledContext);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs < 15_000, "the hard timeout bounds the call: " + elapsedMs + " ms");
        JsonNode live = payload.get("live_pricing");
        assertTrue(live.get("attempted").asBoolean());
        assertTrue(live.get("degraded").asBoolean(), "the stalled provider degrades gracefully");
        assertTrue(live.get("failures").size() > 0, "the failure names the provider");
        assertTrue(live.get("failures").get(0).asText().contains("AWS"), live.get("failures").toString());
        assertTrue(live.get("note").asText().contains("reference rates"),
                "the degraded note points at the fallback: " + live.get("note").asText());
    }

    @Test
    @DisplayName("design_compare_cloud_egress without a GCP key says how to enable live pricing")
    void compareCloudEgressGcpWithoutKey() throws Exception {
        ObjectNode payload = call("design_compare_cloud_egress", """
                {"monthly_egress_gb": 1000, "cloud": "gcp", "region": "us-central1"}
                """);
        JsonNode live = payload.get("live_pricing");
        assertFalse(live.get("attempted").asBoolean(), "no GCP adapter without GCP_BILLING_API_KEY");
        assertTrue(live.get("note").asText().contains("GCP_BILLING_API_KEY"), live.get("note").asText());
    }

    // ── shared stub builders (same recipe as the optimizer engine tests) ────

    private static Metro metro(String code, String name, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(Region.AMER);
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getConnectedMetros()).thenReturn(connected);
        return m;
    }

    private static ConnectedMetro connectedMetro(String code, double avgLatency) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"avgLatency\":" + avgLatency + "}",
                ConnectedMetro.class);
    }

    private static ServiceProfileMetro serviceProfileMetro(String code, String sellerRegion) throws Exception {
        return MAPPER.readValue("{\"code\":\"" + code + "\",\"name\":\"" + code
                        + "\",\"sellerRegions\":{\"" + sellerRegion + "\":\"" + sellerRegion + "\"}}",
                ServiceProfileMetro.class);
    }

    private static GeoCoordinate geo(double lat, double lon) throws Exception {
        return MAPPER.readValue("{\"latitude\":" + lat + ",\"longitude\":" + lon + "}", GeoCoordinate.class);
    }
}
