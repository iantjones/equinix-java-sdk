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
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import api.equinix.javasdk.design.optimizer.wizard.model.ConnectionInputRequirement;
import api.equinix.javasdk.design.optimizer.wizard.model.DeploymentPlan;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedConnection;
import api.equinix.javasdk.design.value.ratecard.PriceQuote;
import api.equinix.javasdk.design.value.ratecard.PriceSource;
import api.equinix.javasdk.design.value.ratecard.RateCard;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.RedundancyPriority;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
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
 * Locks the wizard engine's rebuilt plan-time validation contract over the real Fabric wire:
 *
 * <ul>
 *   <li><b>Layer&nbsp;2 — live router dry-run:</b> {@code plan()} validates every planned Cloud Router
 *       against the native Fabric dry-run surface
 *       ({@code POST /fabric/v4/routers?dryRun=true} through {@code cloudRouters().define(..).dryRun().create()}).
 *       Self-contained FCRs need nothing the customer owns, so this runs for real. A rejection folds
 *       into the plan's validation errors and invalidates the plan.</li>
 *   <li><b>Layer&nbsp;3 — connection endpoint dry-run is DEFERRED:</b> a connection whose A-side Cloud
 *       Router does not exist yet is NOT sent a doomed, endpoint-less connection dry-run
 *       (the old {@code EQ-3142501 "Null value for aSide access point"} failure). Zero connection POSTs
 *       are made; the connection is recorded as deferred, with the customer authorization it will need
 *       enumerated.</li>
 *   <li><b>Best-effort:</b> a gateway with no usable Fabric surface (a bare test stub) skips every live
 *       step instead of failing the plan.</li>
 * </ul>
 */
@DisplayName("DeploymentWizard — layered plan-time validation (router dry-run, deferred connections)")
class DeploymentWizardConnectionValidationWireMockTest extends WireMockTestBase {

    // Named MetroId constants, for readability. The topology lookup matches on VALUE, not reference
    // identity — sharing an instance is a convenience here, never a requirement.
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

    // ── Layer 2 (router dry-run) + Layer 3 (deferred connection) ──

    @Test
    @DisplayName("plan() dry-runs each planned Cloud Router over REST and defers every connection")
    void routerDryRunRunsAndConnectionsAreDeferred() {
        stubRouterDryRunOk();
        stubServiceProfileOk();

        DeploymentPlan plan = wizard().plan();

        assertTrue(plan.isValid(),
                () -> "a passing router dry-run + structurally-fine connections must leave the plan valid: "
                        + plan.getValidationErrors());
        assertTrue(plan.getValidationErrors().isEmpty());

        // The wire: exactly TWO router dry-runs (FCR-DC, FCR-DA), each carrying dryRun=true. NO
        // connection is posted — the to-be-created-FCR connection is deferred, never dry-run.
        wireMock.verify(2, postRequestedFor(urlPathEqualTo("/fabric/v4/routers"))
                .withQueryParam("dryRun", equalTo("true")));
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/routers"))
                .withQueryParam("dryRun", equalTo("true"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("FCR-DC"))));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));

        // The provider connection is recorded as DEFERRED (validated at provisioning), not an error.
        assertNotNull(plan.getDeferredValidations());
        assertTrue(plan.getDeferredValidations().stream().anyMatch(d -> d.contains("FCR-DC-to-aws")),
                () -> "expected a deferred note for the provider connection: " + plan.getDeferredValidations());
    }

    @Test
    @DisplayName("required customer inputs are enumerated per connection (AWS Account ID, VLAN) — nothing fabricated")
    void requiredInputsEnumerated() {
        stubRouterDryRunOk();
        stubServiceProfileOk();

        DeploymentPlan plan = wizard().plan();

        assertNotNull(plan.getRequiredInputs());
        ConnectionInputRequirement aws = plan.getRequiredInputs().stream()
                .filter(r -> r.getConnectionName().equals("FCR-DC-to-aws"))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "expected a required-input entry for FCR-DC-to-aws: " + plan.getRequiredInputs()));

        assertEquals(CloudProviderType.AWS, aws.getCloudType());
        assertTrue(aws.isAuthenticationKeyRequired());
        assertFalse(aws.isAuthenticationKeyProvided(), "the customer has not supplied the key at plan time");
        assertTrue(aws.getAuthenticationKeyLabel().contains("AWS Account ID"), aws.getAuthenticationKeyLabel());
        assertTrue(aws.isVlanTagRequired(), "a cloud VC always needs a VLAN tag");
        assertFalse(aws.isPeeringTypeRequired(), "peering type is Azure-only");
    }

    @Test
    @DisplayName("a router dry-run rejection folds into validation errors and invalidates the plan")
    void routerDryRunRejectionInvalidates() {
        stubServiceProfileOk();
        stubErrorInline(wireMock, "/fabric/v4/routers",
                422, "[{\"errorCode\":\"ERR-422\",\"errorMessage\":\"Package unavailable in metro\"}]");

        DeploymentPlan plan = wizard().plan();

        assertFalse(plan.isValid(), "a rejected router dry-run must invalidate the plan");
        assertTrue(plan.getValidationErrors().stream().anyMatch(e ->
                        e.startsWith("Router dry-run validation warning for 'FCR-")),
                () -> "expected a router dry-run warning naming the router: " + plan.getValidationErrors());

        // Warnings annotate, never abort: the plan is still fully generated, and no connection is posted.
        assertEquals(2, plan.getCloudRouters().size());
        assertEquals(1, plan.getProviderConnections().size());
        assertEquals(1, plan.getBackboneLinks().size());
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));
    }

    @Test
    @DisplayName("a gateway with no usable Fabric surface skips every live step — best-effort, never fatal")
    void bareStubGatewaySkipsLiveValidation() {
        // A bare Mockito stub returns null from cloudRouters()/serviceProfiles()/connections(): the
        // engine must skip validation silently (offline planning stays possible) rather than warn or throw.
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
        // Even offline, the customer inputs are enumerated and the connection is deferred.
        assertFalse(plan.getRequiredInputs().isEmpty());
        assertFalse(plan.getDeferredValidations().isEmpty());
        // An offline run is best-effort, but NEVER silent: each live layer is called out as SKIPPED
        // with a reason so the gap is surfaced rather than hidden.
        assertNotNull(plan.getSkippedValidations());
        assertFalse(plan.getSkippedValidations().isEmpty(),
                () -> "an offline run must call out each skipped live layer, not stay silent: "
                        + plan.getSkippedValidations());
        assertTrue(plan.getSkippedValidations().stream().anyMatch(s -> s.contains("Live router dry-run skipped")),
                () -> "expected a router-dry-run skip note offline: " + plan.getSkippedValidations());
        assertTrue(plan.getSkippedValidations().stream().anyMatch(s -> s.contains("Catalog validation skipped")),
                () -> "expected a catalog skip note offline: " + plan.getSkippedValidations());
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/routers")));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));
    }

    // ── connection-config resilience: redundancy assembly (lens-3b real dry-run) ──

    @Test
    @DisplayName("a connection-level redundancy group is stamped onto the connection body, not silently dropped")
    void redundancyGroupIsStampedOntoConnectionBody() {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections"))
                .willReturn(okJson(loadFixture("/json/fabric/connection_response.json"))));

        // A lens-3b provider connection (pre-existing A-side + auth key present) so the live endpoint
        // dry-run runs for REAL at plan time — the wire is what proves the redundancy group survives.
        DeploymentPlan plan = lens3bPlan(PlannedConnection.builder()
                .name("FCR-DC-to-aws")
                .connectionType(ConnectionType.EVPL_VC)
                .purpose(ConnectionPurpose.PROVIDER)
                .bandwidthMbps(1000)
                .aSideMetro(DC)
                .aSideRouterName("FCR-DC")
                .aSideExistingRouterUuid("cr-existing-uuid")
                .zSideServiceProfileUuid("sp-aws")
                .zSideProviderLabel("AWS")
                .zSideSellerRegion("us-east-1")
                .zSideCloudType(CloudProviderType.AWS)
                .zSideAuthenticationKey("123456789012")
                .zSideVlanTag(1001)
                .zSideRedundancyGroup("aws-redundant-pair")
                .redundancyPriority(RedundancyPriority.SECONDARY)
                .build());

        plan.dryRun();

        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                .withQueryParam("dryRun", equalTo("true"))
                .withRequestBody(matchingJsonPath("$.redundancy.group", equalTo("aws-redundant-pair")))
                .withRequestBody(matchingJsonPath("$.redundancy.priority", equalTo("SECONDARY")))
                .withRequestBody(matchingJsonPath("$.zSide.accessPoint.linkProtocol.vlanTag", equalTo("1001"))));
    }

    @Test
    @DisplayName("a connection with no redundancy group stays standalone — no redundancy is sent")
    void noRedundancyGroupLeavesConnectionStandalone() {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections"))
                .willReturn(okJson(loadFixture("/json/fabric/connection_response.json"))));

        DeploymentPlan plan = lens3bPlan(PlannedConnection.builder()
                .name("FCR-DC-to-aws")
                .connectionType(ConnectionType.EVPL_VC)
                .purpose(ConnectionPurpose.PROVIDER)
                .bandwidthMbps(1000)
                .aSideMetro(DC)
                .aSideRouterName("FCR-DC")
                .aSideExistingRouterUuid("cr-existing-uuid")
                .zSideServiceProfileUuid("sp-aws")
                .zSideProviderLabel("AWS")
                .zSideSellerRegion("us-east-1")
                .zSideCloudType(CloudProviderType.AWS)
                .zSideAuthenticationKey("123456789012")
                .zSideVlanTag(1001)
                .build());

        plan.dryRun();

        // The connection is still dry-run (the standalone path works), but carries no redundancy block.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                .withQueryParam("dryRun", equalTo("true"))
                .withRequestBody(notMatching("(?s).*\"redundancy\".*")));
    }

    @Test
    @DisplayName("a connection with no VLAN tag still dry-runs — a tagless DOT1Q, not a crash or a guessed tag")
    void missingVlanTagDegradesToTaglessDot1q() {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections"))
                .willReturn(okJson(loadFixture("/json/fabric/connection_response.json"))));

        // Auth key present so the dry-run is attempted; VLAN deliberately null (the customer input the
        // wizard never fabricates). The body must be a valid DOT1Q request WITHOUT a guessed vlanTag.
        DeploymentPlan plan = lens3bPlan(PlannedConnection.builder()
                .name("FCR-DC-to-aws")
                .connectionType(ConnectionType.EVPL_VC)
                .purpose(ConnectionPurpose.PROVIDER)
                .bandwidthMbps(1000)
                .aSideMetro(DC)
                .aSideRouterName("FCR-DC")
                .aSideExistingRouterUuid("cr-existing-uuid")
                .zSideServiceProfileUuid("sp-aws")
                .zSideProviderLabel("AWS")
                .zSideSellerRegion("us-east-1")
                .zSideCloudType(CloudProviderType.AWS)
                .zSideAuthenticationKey("123456789012")
                // zSideVlanTag intentionally omitted (null)
                .build());

        plan.dryRun();

        // The dry-run is attempted (no crash from the null VLAN) and the body carries NO guessed vlanTag —
        // the API is left to surface the missing tag rather than the SDK inventing one.
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/fabric/v4/connections"))
                .withQueryParam("dryRun", equalTo("true"))
                .withRequestBody(notMatching("(?s).*\"vlanTag\".*")));
    }

    /**
     * A minimal plan carrying a single lens-3b provider connection (pre-existing A-side endpoint), so
     * {@code dryRun()} runs the connection's live endpoint dry-run for real against WireMock.
     */
    private DeploymentPlan lens3bPlan(PlannedConnection connection) {
        return DeploymentPlan.builder()
                .cloudRouters(Collections.emptyList())
                .providerConnections(List.of(connection))
                .backboneLinks(Collections.emptyList())
                .routingProtocols(Collections.emptyList())
                .fabric(fabric)
                .build();
    }

    // ── stubbing helpers ──

    /** Stubs the Cloud Router dry-run POST to accept the request (the fixture body is a router echo). */
    private static void stubRouterDryRunOk() {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers"))
                .willReturn(okJson(loadFixture("/json/fabric/cloud_router_response.json"))));
    }

    /** Stubs the AWS service-profile lookup so the Layer-1 catalog checks resolve and pass. */
    private static void stubServiceProfileOk() {
        wireMock.stubFor(get(urlPathMatching("/fabric/v4/serviceProfiles/.*"))
                .willReturn(okJson(loadFixture("/json/fabric/service_profile_response.json"))));
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
     * Two metros: DC with AWS available and a single 8000 Mbps AWS-dependent workload placed there,
     * plus DA with no providers — so the plan carries exactly one provider connection
     * (FCR-DC-to-aws, 8000 Mbps) and one backbone link (FCR-DC-to-DA). sp-aws is a real catalog UUID
     * whose service_profile_response fixture offers metro DC with seller region us-east-1.
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
