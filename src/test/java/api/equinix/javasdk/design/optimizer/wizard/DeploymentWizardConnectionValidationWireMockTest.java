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

package api.equinix.javasdk.design.optimizer.wizard;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.FabricGateway;
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
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.fabric.enums.ConnectionType;
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

import static api.equinix.javasdk.core.ResponseStubs.stubErrorInline;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Locks the wizard engine's phase-2b connection-validation contract:
 *
 * <ul>
 *   <li><b>REST dry-run:</b> {@code plan()} validates every planned <em>provider</em>
 *       connection against the native Fabric dry-run surface
 *       ({@code POST /fabric/v4/connections?dryRun=true} through
 *       {@code connections().define(..).dryRun().create()}).
 *       Nothing is provisioned; an API rejection folds into the plan's validation errors as a
 *       warning naming the connection.</li>
 *   <li><b>Best-effort:</b> a gateway with no usable {@code connections()} surface (a bare test
 *       stub) skips the dry run instead of failing the plan.</li>
 * </ul>
 */
@DisplayName("DeploymentWizard phase 2b — REST dry-run connection validation")
class DeploymentWizardConnectionValidationWireMockTest extends WireMockTestBase {

    // Shared MetroId instances so the engine's reference-based topology lookup resolves.
    private static final MetroId DC = MetroId.of(MetroCode.DC);
    private static final MetroId DA = MetroId.of(MetroCode.DA);

    private static Fabric fabric;

    @BeforeAll
    static void setUpClients() {
        fabric = new Fabric(testCredentials());
        redirectToWireMock(fabric);
        fabric.authenticate();
    }

    @AfterAll
    static void tearDownClients() throws Exception {
        if (fabric != null) fabric.close();
    }

    @BeforeEach
    void resetBeforeEach() {
        resetStubs();
    }

    // ── REST dry-run path ──

    @Test
    @DisplayName("plan() dry-run validates each provider connection over REST")
    void restDryRunRunsByDefault() {
        stubDryRunOk();

        DeploymentPlan plan = wizard().plan();

        assertTrue(plan.isValid(), () -> "a passing dry run must leave the plan valid: " + plan.getValidationErrors());
        assertTrue(plan.getValidationErrors().isEmpty());

        // The wire: exactly ONE connections POST — the provider connection's dry run, carrying
        // dryRun=true plus the planned name and sized bandwidth. The backbone link (FCR-DC-to-DA)
        // is NOT dry-run validated, and nothing is provisioned.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                .withQueryParam("dryRun", equalTo("true"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("FCR-DC-to-aws")))
                .withRequestBody(matchingJsonPath("$.bandwidth", equalTo("8000"))));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/routers")));
    }

    @Test
    @DisplayName("a dry-run rejection folds into the plan's validation errors as a warning naming the connection")
    void restDryRunRejectionBecomesWarning() {
        stubErrorInline(wireMock, "/fabric/v4/connections",
                422, "[{\"errorCode\":\"ERR-422\",\"errorMessage\":\"Bandwidth unavailable\"}]");

        DeploymentPlan plan = wizard().plan();

        assertFalse(plan.isValid(), "a rejected dry run must invalidate the plan");
        assertTrue(plan.getValidationErrors().stream().anyMatch(e ->
                        e.startsWith("Dry-run validation warning for 'FCR-DC-to-aws'")),
                () -> "expected a dry-run warning naming the connection: " + plan.getValidationErrors());

        // Warnings annotate, never abort: the plan is still fully generated.
        assertEquals(2, plan.getCloudRouters().size());
        assertEquals(1, plan.getProviderConnections().size());
        assertEquals(1, plan.getBackboneLinks().size());
    }

    @Test
    @DisplayName("a gateway with no usable connections() surface skips the dry run — best-effort, never fatal")
    void bareStubGatewaySkipsRestDryRun() {
        // A bare Mockito stub returns null from connections(): the engine must skip validation
        // silently (offline planning stays possible) rather than warn or throw.
        FabricGateway bareStub = mock(FabricGateway.class);

        DeploymentPlan plan = DeploymentWizard.builder(bareStub, twoMetroResult())
                .routerPackage("STANDARD")
                .routerNamePrefix("FCR")
                .providerConnectionType(ConnectionType.EVPL_VC)
                .rateCard(fixedRateCard())
                .plan();

        assertTrue(plan.isValid(), () -> "an unusable gateway surface must not invalidate the plan: "
                + plan.getValidationErrors());
        assertTrue(plan.getValidationErrors().isEmpty());
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));
    }

    // ── stubbing helpers ──

    /** Stubs the connections dry-run POST to accept the spec (the fixture body is a provisioned connection). */
    private static void stubDryRunOk() {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections"))
                .willReturn(okJson(loadFixture("/json/fabric/connection_provisioned_response.json"))));
    }

    // ── plan helpers ──

    private DeploymentWizard.Builder wizard() {
        return fabric.deploymentWizard(twoMetroResult())
                .routerPackage("STANDARD")
                .routerNamePrefix("FCR")
                .providerConnectionType(ConnectionType.EVPL_VC)
                .rateCard(fixedRateCard());
    }

    /**
     * Two metros: DC with AWS available and a single 8000 Mbps AWS-dependent workload placed
     * there, plus DA with no providers — so the plan carries exactly one provider connection
     * (FCR-DC-to-aws, 8000 Mbps) and one backbone link (FCR-DC-to-DA).
     */
    private static OptimizationResult twoMetroResult() {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());
        WorkloadSpec ml = WorkloadSpec.builder()
                .label("ML Training").bandwidthMbps(8000)
                .dependsOnProviders(List.of(ProviderRequirement.builder().label("AWS").build()))
                .build();

        return OptimizationResult.builder()
                .request(OptimizationRequest.builder().workloads(List.of(ml)).build())
                .recommendations(List.of(
                        MetroRecommendation.builder()
                                .rank(1).metroId(DC).metroName("Ashburn").score(score).reasons(List.of("Primary"))
                                .availableProviders(List.of(ProviderAvailability.builder()
                                        .providerLabel("AWS")
                                        .available(true)
                                        .sellerRegions(List.of("us-east-1"))
                                        .serviceProfileUuid("sp-aws")
                                        .build()))
                                .build(),
                        MetroRecommendation.builder()
                                .rank(2).metroId(DA).metroName("Dallas").score(score).reasons(List.of("Secondary"))
                                .availableProviders(Collections.emptyList())
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

    /** A rate card that always quotes, so pricing issues no HTTP of its own. */
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
