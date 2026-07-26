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
        return call(name, context, argsJson);
    }

    private ObjectNode call(String name, ServerContext ctx, String argsJson) throws Exception {
        JsonNode args = MAPPER.readTree(argsJson);
        return tool(name).getHandler().handle(args, ctx);
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

    @Test
    @DisplayName("a workload's latency_sensitivity and max_latency_ms compose: both are honoured and both are reported")
    void workloadLatencyLeversCompose() throws Exception {
        // The two levers arrive together in one workload object, which is the common MCP call shape.
        // WorkloadSpec.resolvedProfile() used to consult latencySensitivity only when there was no
        // profile override at all — and maxLatencyToleranceMs synthesizes one — so setting both
        // silently discarded the sensitivity and the workload fell through to the by-score rule.
        // 0.1ms is beyond every metro here (the HQ site sits in DC, whose own worst case is 0.5ms),
        // so the ceiling must be reported as unhonoured rather than quietly dropped.
        ObjectNode payload = call("design_optimize_placement", """
                {"workloads": [{"label": "Edge", "type": "general_compute", "bandwidth_mbps": 1000,
                                "latency_sensitivity": "critical", "max_latency_ms": 0.1}],
                 "sites": [{"label": "HQ", "metro_code": "DC"}]}
                """);

        String reasoning = placementReasoning(payload, "Edge");
        assertTrue(reasoning.contains("Lowest weighted latency to user sites"),
                "'critical' must still reach the engine when max_latency_ms also set a profile "
                        + "override; the by-score rule would say 'Placed in highest-scored metro': " + reasoning);
        assertTrue(reasoning.contains("0.1ms latency tolerance"),
                "the per-workload ceiling must appear on the placement: " + reasoning);
        assertTrue(reasoning.contains("NOT honoured"),
                "a ceiling no metro meets must say so rather than read like an unconstrained "
                        + "placement: " + reasoning);

        assertTrue(riskCategories(payload).contains("WORKLOAD_LATENCY_TOLERANCE_UNMET"),
                "the unmet ceiling is also raised as a risk finding: " + payload.toPrettyString());
    }

    @Test
    @DisplayName("a non-positive latency ceiling is rejected with a message naming what to fix")
    void nonPositiveLatencyCeilingsRejected() {
        IllegalArgumentException workload = assertThrows(IllegalArgumentException.class,
                () -> call("design_optimize_placement", """
                        {"workloads": [{"label": "Edge", "max_latency_ms": 0}],
                         "sites": [{"label": "HQ", "metro_code": "DC"}]}
                        """));
        assertTrue(workload.getMessage().contains("Edge"),
                "the message names the offending workload: " + workload.getMessage());

        IllegalArgumentException global = assertThrows(IllegalArgumentException.class,
                () -> call("design_optimize_placement", """
                        {"workloads": [{"label": "Edge"}],
                         "sites": [{"label": "HQ", "metro_code": "DC"}],
                         "constraints": {"max_latency_ms": -5}}
                        """));
        assertTrue(global.getMessage().contains("maxLatencyMs"), global.getMessage());
    }

    @Test
    @DisplayName("redundancy=multi_region HARD-spreads the set across regions, not just the score")
    void optimizeMultiRegionSpansRegions() throws Exception {
        // Three metros, one per region — the shape the shared fixture (all AMER) cannot exercise.
        // Round-robin best-per-region must reach EMEA and APAC, not cluster the top-N in AMER.
        ServerContext ctx = contextWith(List.of(
                metro("NY", "New York", Region.AMER, 40.7128, -74.0060, List.of()),
                metro("LD", "London", Region.EMEA, 51.5074, -0.1278, List.of()),
                metro("SG", "Singapore", Region.APAC, 1.3521, 103.8198, List.of())),
                List.of());

        ObjectNode payload = call("design_optimize_placement", ctx, """
                {"workloads": [{"label": "App", "type": "general_compute", "bandwidth_mbps": 1000}],
                 "sites": [{"label": "HQ", "metro_code": "NY"}],
                 "constraints": {"redundancy": "multi_region"}}
                """);

        java.util.Set<String> regions = new java.util.HashSet<>();
        payload.get("recommendations").forEach(r -> {
            if (r.hasNonNull("region")) {
                regions.add(r.get("region").asText());
            }
        });
        assertTrue(regions.size() >= 2,
                "multi_region must span at least two regions; got " + regions + ": " + payload.toPrettyString());
        assertFalse(riskCategories(payload).contains("SINGLE_REGION"),
                "a genuinely multi-region set raises no SINGLE_REGION finding: " + payload.toPrettyString());
    }

    @Test
    @DisplayName("require_clouds is a coverage-across-the-set guarantee: a single-cloud metro can still host a single-cloud workload")
    void optimizeRequireCloudsCoversAcrossSet() throws Exception {
        // DC carries AWS+Azure; SG carries AWS only. require_clouds=[aws,azure] used to exclude SG for
        // lacking Azure. Now SG qualifies because it carries all of the AWS-only DR workload's clouds,
        // and both required clouds are still reachable somewhere in the set (aws@DC,SG azure@DC).
        ServerContext ctx = contextWith(List.of(
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of()),
                metro("SG", "Singapore", Region.APAC, 1.3521, 103.8198, List.of())),
                List.of(
                        profile("sp-aws", "Amazon Web Services Direct Connect",
                                serviceProfileMetro("DC", "us-east-1"),
                                serviceProfileMetro("SG", "ap-southeast-1")),
                        profile("sp-azure", "Azure ExpressRoute",
                                serviceProfileMetro("DC", "us-east-1"))));

        ObjectNode payload = call("design_optimize_placement", ctx, """
                {"workloads": [
                    {"label": "Payments", "type": "general_compute", "bandwidth_mbps": 1000, "requires_clouds": ["aws", "azure"]},
                    {"label": "DR", "type": "disaster_recovery", "bandwidth_mbps": 500, "requires_clouds": ["aws"]}],
                 "sites": [{"label": "HQ", "metro_code": "DC"}],
                 "require_clouds": ["aws", "azure"]}
                """);

        java.util.Set<String> recommended = new java.util.HashSet<>();
        payload.get("recommendations").forEach(r -> recommended.add(r.get("metro").asText()));
        assertTrue(recommended.contains("SG"),
                "SG carries only AWS but hosts the AWS-only DR workload, so require_clouds=[aws,azure] must "
                        + "not filter it out: " + payload.toPrettyString());
        assertFalse(riskCategories(payload).contains("REQUIRED_CLOUD_NOT_COVERED"),
                "both required clouds are reachable across the set, so there is no coverage gap: "
                        + payload.toPrettyString());
    }

    @Test
    @DisplayName("require_clouds raises REQUIRED_CLOUD_NOT_COVERED when the selected set leaves a required cloud unreached")
    void optimizeRequireCloudsCoverageGapIsFlagged() throws Exception {
        // AWS lives only in DC, Azure only in SG. Each metro is eligible via a single-cloud workload.
        // max_metros=1 forces one metro, so whichever is chosen, the other required cloud (available in
        // the account but not in the selected set) is a coverage gap — the new HIGH finding.
        ServerContext ctx = contextWith(List.of(
                metro("DC", "Ashburn", Region.AMER, DC_LAT, DC_LON, List.of()),
                metro("SG", "Singapore", Region.APAC, 1.3521, 103.8198, List.of())),
                List.of(
                        profile("sp-aws", "Amazon Web Services Direct Connect",
                                serviceProfileMetro("DC", "us-east-1")),
                        profile("sp-azure", "Azure ExpressRoute",
                                serviceProfileMetro("SG", "ap-southeast-1"))));

        ObjectNode payload = call("design_optimize_placement", ctx, """
                {"workloads": [
                    {"label": "A", "type": "general_compute", "bandwidth_mbps": 1000, "requires_clouds": ["aws"]},
                    {"label": "B", "type": "general_compute", "bandwidth_mbps": 1000, "requires_clouds": ["azure"]}],
                 "sites": [{"label": "HQ", "metro_code": "DC"}],
                 "require_clouds": ["aws", "azure"],
                 "constraints": {"max_metros": 1}}
                """);

        assertEquals(1, payload.get("recommendations").size(), "max_metros caps the set to one metro");
        assertTrue(riskCategories(payload).contains("REQUIRED_CLOUD_NOT_COVERED"),
                "one required cloud lives only in the unselected metro, so coverage across the set fails: "
                        + payload.toPrettyString());
    }

    /** The engine's rationale for where a workload landed, from any recommendation in the payload. */
    private static String placementReasoning(ObjectNode payload, String workloadLabel) {
        for (JsonNode rec : payload.get("recommendations")) {
            JsonNode placed = rec.get("assigned_workloads");
            if (placed == null) {
                continue;
            }
            for (JsonNode wp : placed) {
                if (workloadLabel.equals(wp.get("workload").asText())) {
                    return wp.get("reasoning").asText();
                }
            }
        }
        throw new AssertionError("no placement for '" + workloadLabel + "' in " + payload.toPrettyString());
    }

    private static List<String> riskCategories(ObjectNode payload) {
        List<String> categories = new java.util.ArrayList<>();
        payload.get("risk_assessment").get("findings").forEach(f -> categories.add(f.get("category").asText()));
        return categories;
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
                 "deployment": {"customer_asn": 65001, "router_package": "STANDARD",
                                "notifications": ["noc@example.com"]}}
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
        JsonNode validation = plan.get("validation");
        assertNotNull(validation, "the layered validation block is surfaced");
        assertTrue(validation.get("valid").asBoolean(),
                "a structurally sound plan reads as valid: " + validation.toPrettyString());
        assertNotNull(validation.get("validated_now"), "the validated-now section is present");
        assertNotNull(validation.get("deferred_to_provisioning"), "the deferred section is present");
        assertNotNull(validation.get("skipped"), "the skipped (could-not-validate-now) section is present");
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

    /**
     * The brand-new-customer lens: a first-time customer owns no resources and the target Cloud Router
     * does not exist yet, so the AWS connection's live endpoint dry-run cannot run at plan time. The
     * plan must (a) still read as VALID — a structurally-fine, not-yet-provisionable connection is not
     * a validation error — (b) list that connection under 'deferred_to_provisioning', and (c) hand the
     * customer a per-connection 'required_inputs' checklist naming the exact cloud authorization key,
     * with nothing fabricated. This is the whole point of the layered-validation rework.
     */
    @Test
    @DisplayName("design_plan_deployment surfaces a valid plan with a deferred connection dry-run and a per-connection required-inputs checklist")
    void planSurfacesDeferredAndRequiredInputs() throws Exception {
        ObjectNode plan = call("design_plan_deployment", """
                {"optimization": {
                    "workloads": [{"label": "Web Tier", "type": "general_compute", "bandwidth_mbps": 1000}],
                    "sites": [{"label": "HQ", "metro_code": "DC"}],
                    "require_clouds": ["aws"],
                    "constraints": {"max_metros": 1}},
                 "deployment": {"notifications": ["noc@example.com"]}}
                """);

        JsonNode validation = plan.get("validation");
        assertNotNull(validation, "the layered validation block is present");
        assertTrue(validation.get("valid").asBoolean(),
                "a structurally-fine plan whose only outstanding check is a deferred connection dry-run "
                        + "must read as VALID, not failed: " + validation.toPrettyString());
        assertEquals(0, validation.get("validated_now").get("error_count").asInt(),
                "no structural / catalog / router-dry-run errors for this plan: " + validation.toPrettyString());
        // The stubbed gateway serves no cloud-router or connection surface, so no live dry-run runs and
        // no live error can appear — the plan is validated structurally only, exactly as a bare/offline
        // run should degrade.
        assertEquals(0, validation.get("validated_now").get("new_market_gaps").size(),
                "AWS is offered at DC in the fixture, so there is no new-market gap");

        JsonNode deferred = validation.get("deferred_to_provisioning");
        assertTrue(deferred.get("count").asInt() >= 1,
                "the AWS connection has no pre-existing endpoint, so its live endpoint dry-run is deferred "
                        + "to provisioning: " + deferred.toPrettyString());
        assertTrue(deferred.get("notes").get(0).asText().toLowerCase(java.util.Locale.ROOT).contains("provisioning"),
                "the deferred note explains it runs at provisioning: " + deferred.get("notes").toPrettyString());

        JsonNode requiredInputs = plan.get("required_inputs");
        assertTrue(requiredInputs.size() >= 1, "a per-connection required-inputs checklist is surfaced");
        JsonNode aws = null;
        for (JsonNode req : requiredInputs) {
            if ("AWS".equals(req.get("cloud_type").asText())) {
                aws = req;
            }
        }
        assertNotNull(aws, "the AWS connection appears in the checklist: " + requiredInputs.toPrettyString());
        assertTrue(aws.get("authentication_key_required").asBoolean(), "AWS needs an authorization key");
        assertFalse(aws.get("authentication_key_provided").asBoolean(),
                "no key is fabricated at plan time — the customer must supply it");
        assertTrue(aws.get("authentication_key_label").asText().contains("AWS Account ID"),
                "the checklist names the exact key the connection needs: " + aws.toPrettyString());
        assertTrue(aws.get("vlan_tag_required").asBoolean(), "a cloud VC always needs a VLAN tag");
        assertFalse(aws.get("peering_type_required").asBoolean(), "only Azure needs a peering type");
        assertTrue(aws.get("summary").asText().contains("AWS Account ID"),
                "the one-line summary restates what to gather: " + aws.get("summary").asText());

        // The headline summary must not read as a failure for a valid plan.
        assertFalse(plan.get("summary").asText().contains("VALIDATION ERRORS"),
                "a valid plan's summary must not shout VALIDATION ERRORS: " + plan.get("summary").asText());
    }

    /**
     * The owner's skip-and-call-out requirement, surfaced end-to-end: a live dry-run that cannot be
     * ATTEMPTED must land in a THIRD validation bucket, {@code validation.skipped}, each entry carrying a
     * reason. Here the stubbed gateway wires metros + service profiles but NO cloud-router API surface, so
     * the live router dry-run and the package-ceiling checks have nowhere to run — each is SKIPPED with a
     * reason rather than returning silently (the gap hidden) or being dumped into errors (a sound plan
     * wrongly invalidated). A skip is an infeasibility, never a plan defect: the plan stays VALID.
     */
    @Test
    @DisplayName("design_plan_deployment surfaces a third 'skipped' bucket, with reasons, that never invalidates the plan")
    void planSurfacesSkippedValidations() throws Exception {
        ObjectNode plan = call("design_plan_deployment", """
                {"optimization": {
                    "workloads": [{"label": "Web Tier", "type": "general_compute", "bandwidth_mbps": 1000}],
                    "sites": [{"label": "HQ", "metro_code": "DC"}],
                    "require_clouds": ["aws"],
                    "constraints": {"max_metros": 1}},
                 "deployment": {"notifications": ["noc@example.com"]}}
                """);

        JsonNode validation = plan.get("validation");
        JsonNode skipped = validation.get("skipped");
        assertNotNull(skipped, "the third (skipped) validation bucket is surfaced: " + validation.toPrettyString());
        assertTrue(skipped.get("scope").asText().contains("invalidate"),
                "the scope states a skip does not invalidate the plan: " + skipped.get("scope").asText());

        // The stub serves no cloud-router surface, so the live router dry-run and the package-ceiling
        // checks cannot be attempted — each is SKIPPED with a reason (never a silent return, never an error).
        assertTrue(skipped.get("count").asInt() >= 1,
                "an offline live surface makes at least one check un-attemptable, and it must be called out: "
                        + skipped.toPrettyString());
        assertEquals(skipped.get("count").asInt(), skipped.get("notes").size(),
                "count mirrors the notes array");
        assertTrue(skipped.get("notes").get(0).asText().toLowerCase(java.util.Locale.ROOT).contains("skipped"),
                "each skip note calls out the check it could not run: " + skipped.get("notes").toPrettyString());

        // A skip is an infeasibility, never a defect: it must neither invalidate the plan nor count as an error.
        assertTrue(validation.get("valid").asBoolean(),
                "skipped live checks never invalidate a structurally-sound plan: " + validation.toPrettyString());
        assertEquals(0, validation.get("validated_now").get("error_count").asInt(),
                "the skips are not folded into errors: " + validation.toPrettyString());
    }

    /**
     * The tool description is the prompt the calling model reasons from, so the layered-validation
     * contract has to live in it verbatim: validation runs with zero provisioned resources, the
     * connection endpoint dry-run is deferred to provisioning (not an error), a live dry-run that cannot
     * be performed is SKIPPED with a reason (and never invalidates a sound plan), and new customers get a
     * required-inputs checklist that is never fabricated. This also pins the deliberate decision NOT to
     * advertise a plan-time existing-endpoint input: the Deployment Wizard builder exposes no hook to
     * inject a pre-existing port/FCR (that path is SDK-only, by hand-building the plan), so a schema
     * field for it would be an accepted-but-ignored lever — the exact dishonesty this catalog forbids.
     */
    @Test
    @DisplayName("design_plan_deployment description states the layered-validation contract and adds no un-wireable existing-endpoint input")
    void planDeploymentDescriptionStatesLayeredValidation() {
        ToolRegistration plan = tool("design_plan_deployment");
        String description = plan.getDescription();

        assertTrue(description.contains("PLAN-ONLY"), "the tool never executes: " + description);
        assertTrue(description.contains("ZERO provisioned resources"),
                "validation works for a brand-new customer with nothing provisioned: " + description);
        assertTrue(description.contains("deferred_to_provisioning"),
                "the connection endpoint dry-run is deferred, and the description must name where: " + description);
        assertTrue(description.contains("NOT errors"),
                "a deferred item is not a validation error, and the description must say so: " + description);
        assertTrue(description.contains("required_inputs"),
                "the per-connection customer checklist must be named: " + description);
        assertTrue(description.contains("never fabricated"),
                "the authorization keys are gathered by the customer, not invented: " + description);

        // The skip-and-call-out contract: a dry-run that cannot be performed is SKIPPED, reported with a
        // reason, and never invalidates a structurally-sound plan — a new customer must read this verbatim.
        assertTrue(description.contains("validation.skipped"),
                "the third validation bucket must be named so a model knows where skips land: " + description);
        assertTrue(description.contains("SKIPPED"),
                "the description must state that an un-performable dry-run is SKIPPED, not errored: " + description);
        assertTrue(description.contains("NEVER invalidates"),
                "the description must promise a skip never invalidates a structurally-sound plan: " + description);
        assertTrue(description.contains("rate-limited") && description.contains("unreachable"),
                "the description must name the infeasibility cases (403/429/offline/unreachable): " + description);

        // No existing-endpoint input is advertised, because the wizard builder cannot accept one.
        Map<String, Object> deployment = child(plan.getInputSchema(), "deployment");
        Map<String, Object> deploymentProps = properties(deployment);
        assertFalse(deploymentProps.containsKey("existing_router_uuid"),
                "the wizard builder exposes no pre-existing-endpoint hook, so no such input may be advertised");
        assertFalse(deploymentProps.containsKey("existing_port_uuid"),
                "the wizard builder exposes no pre-existing-endpoint hook, so no such input may be advertised");
        assertFalse(deploymentProps.containsKey("existing_endpoints"),
                "the wizard builder exposes no pre-existing-endpoint hook, so no such input may be advertised");
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

    @Test
    @DisplayName("design_estimate_latency renders an unlisted metro's real code, not the UNKNOWN enum sentinel")
    void latencyUnlistedMetroRendersRealCode() throws Exception {
        // 'ZZ' is a metro this SDK's MetroCode enum does not list, so Metro.getCode() collapses it
        // to UNKNOWN. The endpoint used to render String.valueOf(getCode()) — emitting a misleading
        // "UNKNOWN" code for a metro that has a perfectly real one. The rendered code must be the
        // exact metroId, so the LLM reading this tool sees the true code.
        assertEquals(MetroCode.UNKNOWN, MetroCode.fromCode("ZZ"),
                "precondition: ZZ must be genuinely absent from the MetroCode enum");
        Metro dc = metro("DC", "Ashburn", DC_LAT, DC_LON, List.of());
        Metro zz = metro("ZZ", "Newly Added Metro", 48.8566, 2.3522, List.of());
        ServerContext ctx = contextWith(List.of(dc, zz), List.of());

        ObjectNode payload = call("design_estimate_latency", ctx, "{\"from\": \"DC\", \"to\": \"ZZ\"}");

        JsonNode to = payload.get("to");
        assertEquals("metro", to.get("kind").asText());
        assertEquals("ZZ", to.get("code").asText(),
                "the real metro code is rendered, not the UNKNOWN enum sentinel: " + payload.toPrettyString());
        assertFalse(to.get("code").asText().equalsIgnoreCase("UNKNOWN"),
                "a metro absent from the MetroCode enum must not surface as 'UNKNOWN'");
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

    // ── schema honesty: no accepted-but-ignored input, no mis-stated contract ──

    /**
     * The tool schemas are prompts the calling model reasons from, so a field that survives in one
     * has to be a field the engine reads, described the way the engine actually behaves. These
     * assertions pin the levers that were removed because nothing consumed them, and the wording
     * that distinguishes a candidacy filter from a placement rule — the distinction a model cannot
     * recover from the payload alone.
     */
    @Test
    @DisplayName("the optimizer schema drops the levers the engine ignores and states the rest precisely")
    void optimizerSchemaMatchesTheEngine() {
        Map<String, Object> schema = tool("design_optimize_placement").getInputSchema();

        assertFalse(properties(child(schema, "constraints")).containsKey("monthly_budget_min"),
                "BudgetRange.minMonthly is read nowhere in the engine — it must not be advertised");
        assertTrue(description(child(schema, "constraints", "monthly_budget_max")).contains("not a filter"),
                "the budget ceiling only sets cost_estimate.within_budget; the description must say so");

        // Candidacy vs placement: the two latency ceilings do different things and must read that way.
        assertTrue(description(child(schema, "workloads", "max_latency_ms")).contains("narrows PLACEMENT"),
                "the per-workload ceiling narrows placement, not the recommended metro set");
        assertTrue(description(child(schema, "constraints", "max_latency_ms")).contains("narrows CANDIDACY"),
                "the request-level bound excludes metros before scoring");

        assertTrue(description(child(schema, "workloads", "latency_sensitivity")).contains("Only 'critical'"),
                "the other tiers change neither placement nor ranking, so the schema must not imply they do");
        assertTrue(description(child(schema, "workloads", "type")).contains("never influence the metro ranking"),
                "power/cooling are recorded for facility selection, not scored");
        assertTrue(description(child(schema, "workloads", "bandwidth_mbps")).contains("cost sizing"),
                "bandwidth prices the deployment; it does not rank metros");
        assertTrue(description(child(schema, "sites", "weight")).contains("NOT weighted 1.0"),
                "an unweighted site counts as an average site, not as 1.0");
    }

    /**
     * The multi-region / per-workload-cloud / budget behaviour changed the CONTRACT of three fields,
     * not just the engine internals. Because the descriptions are the only place a calling model
     * learns that contract, these assertions pin the corrected wording so a future edit cannot quietly
     * revert to the pre-change promises (require_clouds "in every metro", redundancy as a mere score
     * nudge, budget surfacing only within_budget).
     */
    @Test
    @DisplayName("the optimizer schema states the new coverage / hard-spread / budget-finding contract")
    void optimizerSchemaStatesTheNewContract() {
        Map<String, Object> schema = tool("design_optimize_placement").getInputSchema();

        // require_clouds is now a coverage-across-the-set guarantee, not a per-metro exclusion filter.
        String requireClouds = description(child(schema, "require_clouds"));
        assertTrue(requireClouds.contains("SOMEWHERE in the recommended set"),
                "require_clouds must read as reachable-somewhere, not present-in-every-metro: " + requireClouds);
        assertTrue(requireClouds.contains("REQUIRED_CLOUD_NOT_COVERED"),
                "the coverage guarantee is enforced by this finding, which the description must name: " + requireClouds);
        assertFalse(requireClouds.contains("excluded from candidacy"),
                "the old per-metro-exclusion wording must be gone: " + requireClouds);

        // A workload's own requires_clouds now widens candidacy, not only placement.
        assertTrue(description(child(schema, "workloads", "requires_clouds")).contains("widens CANDIDACY"),
                "a single-cloud metro can now be a candidate to host a single-cloud workload");

        // multi_region / multi_metro now hard-spread; an impossible spread is a HIGH SINGLE_REGION block.
        String redundancy = description(child(schema, "constraints", "redundancy"));
        assertTrue(redundancy.contains("HARD-spread"),
                "redundancy must state the region round-robin is enforced, not merely scored: " + redundancy);
        assertTrue(redundancy.contains("SINGLE_REGION"),
                "an impossible multi_region spread is surfaced as SINGLE_REGION; the description must say so: " + redundancy);

        // The budget ceiling now also raises a BUDGET_EXCEEDED finding, and is still not a filter.
        String budget = description(child(schema, "constraints", "monthly_budget_max"));
        assertTrue(budget.contains("not a filter"), "the ceiling is still reported-against, not enforced: " + budget);
        assertTrue(budget.contains("BUDGET_EXCEEDED"),
                "an overrun now also surfaces a finding, which the description must name: " + budget);
    }

    @Test
    @DisplayName("design_plan_deployment offers only real package codes and no inert pricing term")
    void planDeploymentSchemaMatchesTheWizard() {
        Map<String, Object> deployment = child(tool("design_plan_deployment").getInputSchema(), "deployment");

        assertFalse(properties(deployment).containsKey("term"),
                "no rate card in this server's chain resolves a price by term");
        assertEquals(List.of("LAB", "BASIC", "STANDARD", "ADVANCED", "PREMIUM"),
                child(deployment, "router_package").get("enum"),
                "the schema must offer exactly the deployable GatewayPackageCode tiers — the old "
                        + "'PRO' example is not one of them and fails at plan time");
        // Every configured address now reaches every planned resource (the get(0) truncation was a
        // defect, fixed with PlannedCloudRouter/PlannedConnection.notificationEmails lists) — the
        // schema must say so and must no longer claim only the first is used.
        assertTrue(description(child(deployment, "notifications")).contains("EVERY address"),
                "every address reaches every planned resource; the schema must say so");
        assertFalse(description(child(deployment, "notifications")).contains("FIRST"),
                "the old only-the-first claim must be gone from the schema");

        // The embedded optimization block is the same shape, so the same removals apply.
        Map<String, Object> optimization =
                child(tool("design_plan_deployment").getInputSchema(), "optimization");
        assertFalse(properties(child(optimization, "constraints")).containsKey("monthly_budget_min"));
    }

    @Test
    @DisplayName("the pricing and peering tools drop the inputs their engines never read")
    void pricingAndPeeringSchemasMatchTheirEngines() {
        assertFalse(properties(tool("design_estimate_tco").getInputSchema()).containsKey("term"),
                "EquinixRateCard and ReferenceRateCard both ignore term, so no figure could change");
        assertFalse(properties(tool("design_compare_cloud_egress").getInputSchema()).containsKey("term"),
                "the provider cards price egress per GB by region, never by contract term");
        assertFalse(properties(tool("design_analyze_peering").getInputSchema())
                        .containsKey("include_fabric_connections"),
                "PeeringIntelligenceEngine reads the flag nowhere: no Fabric connection lookup exists "
                        + "behind it, so the tool advertised API calls it never makes");
        assertTrue(description(child(tool("design_analyze_peering").getInputSchema(), "metros"))
                        .contains("do not restrict"),
                "customerMetros adds columns and drives resiliency; it does not scope the analysis");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(Map<String, Object> schema) {
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");
        assertNotNull(props, "not an object schema: " + schema);
        return props;
    }

    /**
     * Walks a property path through the schema. An <em>intermediate</em> array step descends into
     * its {@code items}, so a path can name a field of a list element
     * ({@code "workloads", "max_latency_ms"}); the final step is returned as-is, so a path can also
     * name the array itself and read its own description.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> child(Map<String, Object> schema, String... path) {
        Map<String, Object> node = schema;
        for (int i = 0; i < path.length; i++) {
            Object next = properties(node).get(path[i]);
            assertNotNull(next, "no '" + path[i] + "' property in " + node);
            node = (Map<String, Object>) next;
            if (i < path.length - 1 && "array".equals(node.get("type"))) {
                node = (Map<String, Object>) node.get("items");
            }
        }
        return node;
    }

    private static String description(Map<String, Object> schema) {
        Object description = schema.get("description");
        assertNotNull(description, "every schema field carries an LLM-facing description");
        return String.valueOf(description);
    }

    // ── shared stub builders (same recipe as the optimizer engine tests) ────

    private static Metro metro(String code, String name, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        return metro(code, name, Region.AMER, lat, lon, connected);
    }

    private static Metro metro(String code, String name, Region region, double lat, double lon,
                               List<ConnectedMetro> connected) throws Exception {
        Metro m = mock(Metro.class);
        when(m.metroId()).thenReturn(MetroId.of(code));
        when(m.getCode()).thenReturn(MetroCode.fromCode(code));
        when(m.getName()).thenReturn(name);
        when(m.getRegion()).thenReturn(region);
        when(m.geoCoordinates()).thenReturn(geo(lat, lon));
        when(m.getConnectedMetros()).thenReturn(connected);
        return m;
    }

    private static ServiceProfile profile(String uuid, String name, ServiceProfileMetro... metros) {
        ServiceProfile p = mock(ServiceProfile.class);
        when(p.getUuid()).thenReturn(uuid);
        when(p.getName()).thenReturn(name);
        when(p.metros()).thenReturn(List.of(metros));
        return p;
    }

    /**
     * A fresh {@link ServerContext} over a Mockito gateway carrying exactly the given metros and
     * service profiles — used by the multi-region and required-cloud-coverage tests, which need a
     * different metro/region/provider topology than the shared three-AMER-metro fixture.
     */
    private ServerContext contextWith(List<Metro> metroList, List<ServiceProfile> profiles) throws Exception {
        Metros m = mock(Metros.class);
        when(m.list()).thenReturn(new PaginatedList<>(metroList, null, null, null, null));
        ServiceProfiles sp = mock(ServiceProfiles.class);
        when(sp.search()).thenReturn(new PaginatedFilteredList<>(profiles, null, null, null, null));
        FabricGateway f = mock(FabricGateway.class);
        when(f.metros()).thenReturn(m);
        when(f.serviceProfiles()).thenReturn(sp);
        return ServerContext.builder()
                .fabric(f)
                .metroRegistry(MetroRegistry.load(m))
                .environment(Map.of())
                .build();
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
