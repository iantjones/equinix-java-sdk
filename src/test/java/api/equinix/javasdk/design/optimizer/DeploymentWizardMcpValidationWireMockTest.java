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

package api.equinix.javasdk.design.optimizer;

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.Mcp;
import api.equinix.javasdk.core.WireMockTestBase;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.model.DeploymentTopology;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.MetroScore;
import api.equinix.javasdk.design.optimizer.model.OptimizationRequest;
import api.equinix.javasdk.design.optimizer.model.OptimizationResult;
import api.equinix.javasdk.design.optimizer.model.ProviderAvailability;
import api.equinix.javasdk.design.optimizer.model.ProviderRequirement;
import api.equinix.javasdk.design.optimizer.model.WorkloadPlacement;
import api.equinix.javasdk.design.optimizer.model.WorkloadSpec;
import api.equinix.javasdk.design.optimizer.wizard.DeploymentWizard;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.mcp.McpClientConfig;
import api.equinix.javasdk.mcp.bridge.McpBridge;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Exercises the design-to-mcp interop seam: {@code DeploymentWizard.Builder.withMcpValidation(McpBridge)}.
 * When a bridge is supplied, {@code plan()} posts each planned provider connection to the MCP
 * {@code check_connection} tool (JSON-RPC {@code tools/call} against a WireMock-backed {@link Mcp})
 * as an <em>optional enrichment</em> pass and folds failed validations into the plan's validation
 * errors.
 *
 * <p>The engine's <b>default</b> validation path is the native Fabric REST dry-run
 * ({@code POST /fabric/v4/connections?dryRun=true}); it is skipped in this suite because the bare
 * Mockito {@code FabricGateway} exposes no {@code connections()} surface. The REST-default contract
 * is locked by {@code design.optimizer.wizard.DeploymentWizardConnectionValidationWireMockTest}.</p>
 */
@DisplayName("DeploymentWizard.withMcpValidation() — optional MCP enrichment of planned connections")
class DeploymentWizardMcpValidationWireMockTest extends WireMockTestBase {

    private static final MetroId DC = MetroId.of(MetroCode.DC);

    private static Mcp mcpClient;
    private static McpBridge mcpBridge;

    @BeforeAll
    static void setUpMcp() {
        McpClientConfig config = McpClientConfig.builder()
                .fabricEndpoint(wireMockUrl() + "/mcp/fabric")
                .tokenEndpoint(wireMockUrl() + "/oauth2/v1/token")
                .connectTimeoutMs(5000)
                .readTimeoutMs(5000)
                .maxRetries(0)
                .build();
        mcpClient = new Mcp(testCredentials(), config);

        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("initialize")))
                .willReturn(okJson(loadFixture("/json/mcp/initialize_response.json"))));
        mcpClient.initialize();
        mcpBridge = new McpBridge(mcpClient);
    }

    @AfterAll
    static void tearDownMcp() throws Exception {
        if (mcpClient != null) mcpClient.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    @Test
    @DisplayName("plan() posts check_connection per provider connection with name/type/bandwidth; a valid result leaves the plan valid")
    void planValidatesConnectionsViaMcp() {
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.params.name", equalTo("check_connection")))
                .willReturn(okJson(loadFixture("/json/mcp/validate_connection_result.json"))));

        DeploymentPlan plan = wizard().plan();

        assertTrue(plan.isValid(), () -> "MCP said valid, so the plan must be valid: " + plan.getValidationErrors());
        assertTrue(plan.getValidationErrors().isEmpty());

        // The wire: one JSON-RPC tools/call per planned provider connection (a single DC-AWS
        // connection here), carrying the planned name, connection type, and sized bandwidth.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.jsonrpc", equalTo("2.0")))
                .withRequestBody(matchingJsonPath("$.method", equalTo("tools/call")))
                .withRequestBody(matchingJsonPath("$.params.name", equalTo("check_connection")))
                .withRequestBody(matchingJsonPath("$.params.arguments.name", equalTo("FCR-DC-to-aws")))
                .withRequestBody(matchingJsonPath("$.params.arguments.type", equalTo("EVPL_VC")))
                .withRequestBody(matchingJsonPath("$.params.arguments.bandwidth", equalTo("8000"))));
    }

    @Test
    @DisplayName("an MCP validation failure invalidates the plan with a warning naming the connection")
    void mcpValidationFailureInvalidatesPlan() {
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.params.name", equalTo("check_connection")))
                .willReturn(okJson(loadFixture("/json/mcp/validate_connection_invalid_result.json"))));

        DeploymentPlan plan = wizard().plan();

        assertFalse(plan.isValid(), "a failed MCP validation must invalidate the plan");
        assertTrue(plan.getValidationErrors().stream().anyMatch(e ->
                        e.contains("MCP validation warning for 'FCR-DC-to-aws'")
                                && e.contains("Bandwidth 8000 Mbps is not available")),
                () -> "expected the MCP warning to name the connection and message: " + plan.getValidationErrors());

        // The plan itself is still fully generated — validation failures annotate, never abort.
        assertEquals(1, plan.getCloudRouters().size());
        assertEquals(1, plan.getProviderConnections().size());
    }

    @Test
    @DisplayName("an MCP transport/tool error is swallowed: validation is best-effort and never blocks planning")
    void mcpErrorDoesNotBlockPlanning() {
        // The JSON-RPC error object makes Mcp.callTool throw McpException; the engine's
        // per-connection try/catch must swallow it and leave the plan valid.
        wireMock.stubFor(post(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.params.name", equalTo("check_connection")))
                .willReturn(okJson(loadFixture("/json/mcp/error_response.json"))));

        DeploymentPlan plan = assertDoesNotThrow(() -> wizard().plan());

        assertTrue(plan.isValid(),
                () -> "an MCP failure must not surface as a validation error: " + plan.getValidationErrors());
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/mcp/fabric"))
                .withRequestBody(matchingJsonPath("$.params.name", equalTo("check_connection"))));
    }

    // ── helpers ──

    private DeploymentWizard.Builder wizard() {
        // A bare stub exposes no connections() surface, so the engine's default REST dry-run
        // validation is skipped and only the MCP enrichment pass under test hits the wire.
        FabricGateway fabric = mock(FabricGateway.class);
        return DeploymentWizard.builder(fabric, singleMetroResult())
                .routerPackage("STANDARD")
                .routerNamePrefix("FCR")
                .providerConnectionType(ConnectionType.EVPL_VC)
                .withMcpValidation(mcpBridge)
                .rateCard(fixedRateCard());
    }

    /** One metro (DC) with AWS available and a single 8000 Mbps AWS-dependent workload placed there. */
    private static OptimizationResult singleMetroResult() {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());
        WorkloadSpec ml = WorkloadSpec.builder()
                .label("ML Training").bandwidthMbps(8000)
                .dependsOnProviders(List.of(ProviderRequirement.builder().label("AWS").build()))
                .build();

        return OptimizationResult.builder()
                .request(OptimizationRequest.builder().workloads(List.of(ml)).build())
                .recommendations(List.of(MetroRecommendation.builder()
                        .rank(1).metroId(DC).metroName("Ashburn").score(score).reasons(List.of("Primary"))
                        .availableProviders(List.of(ProviderAvailability.builder()
                                .providerLabel("AWS")
                                .available(true)
                                .sellerRegions(List.of("us-east-1"))
                                .serviceProfileUuid("sp-aws")
                                .build()))
                        .build()))
                .topology(new DeploymentTopology(List.of(WorkloadPlacement.builder()
                        .workloadLabel("ML Training")
                        .assignedMetro(DC)
                        .reasoning("AWS at DC")
                        .build())))
                .computedAt(Instant.now())
                .computeTimeMs(5)
                .build();
    }

    private static RateCard fixedRateCard() {
        return new RateCard() {
            @Override
            public Optional<PriceQuote> connection(ConnectionType type, int bandwidthMbps,
                                                   MetroCode metro,
                                                   api.equinix.javasdk.design.value.ratecard.Term term) {
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(500), BigDecimal.ZERO,
                        Currency.getInstance("USD"), PriceSource.ESTIMATE));
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro,
                                                    api.equinix.javasdk.design.value.ratecard.Term term) {
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(300), BigDecimal.ZERO,
                        Currency.getInstance("USD"), PriceSource.ESTIMATE));
            }

            @Override
            public PriceSource source() {
                return PriceSource.ESTIMATE;
            }
        };
    }
}
