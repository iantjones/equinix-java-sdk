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

import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.exception.EquinixAuthorizationException;
import api.equinix.javasdk.core.exception.EquinixNotFoundException;
import api.equinix.javasdk.core.exception.EquinixServiceException;
import api.equinix.javasdk.core.model.MetroId;
import api.equinix.javasdk.design.optimizer.model.DeploymentTopology;
import api.equinix.javasdk.design.optimizer.model.MetroRecommendation;
import api.equinix.javasdk.design.optimizer.model.MetroScore;
import api.equinix.javasdk.design.optimizer.model.OptimizationRequest;
import api.equinix.javasdk.design.optimizer.model.ProviderAvailability;
import api.equinix.javasdk.design.optimizer.model.ProviderRequirement;
import api.equinix.javasdk.design.optimizer.model.WorkloadPlacement;
import api.equinix.javasdk.design.optimizer.model.WorkloadSpec;
import api.equinix.javasdk.design.optimizer.wizard.enums.ConnectionPurpose;
import api.equinix.javasdk.design.optimizer.wizard.model.ConnectionInputRequirement;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedCloudRouter;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedConnection;
import api.equinix.javasdk.design.optimizer.wizard.model.PlannedRoutingProtocol;
import api.equinix.javasdk.fabric.client.CloudRouters;
import api.equinix.javasdk.fabric.client.Connections;
import api.equinix.javasdk.fabric.client.ServiceProfiles;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.fabric.model.CloudRouter;
import api.equinix.javasdk.fabric.model.CloudRouterPackage;
import api.equinix.javasdk.fabric.model.ServiceProfile;
import api.equinix.javasdk.fabric.model.implementation.AccessPointTypeConfig;
import api.equinix.javasdk.fabric.model.implementation.ServiceProfileMetro;
import api.equinix.javasdk.fabric.model.implementation.cloud.CloudProviderType;
import api.equinix.javasdk.fabric.model.json.creators.CloudRouterOperator;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlanValidator} — the layered, plan-time validation the Deployment Wizard runs
 * without provisioning anything. Catalog behaviour is driven with Mockito service-profile / cloud-router
 * / connection clients so the pass/fail cases are exact and HTTP-free.
 */
@DisplayName("PlanValidator — layered plan-time validation")
class PlanValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MetroId DC = MetroId.of(MetroCode.DC);
    private static final MetroId SV = MetroId.of(MetroCode.SV);

    // ══════════════════════════════════════════════
    //  Layer 1 — catalog checks
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a bandwidth outside the profile's allowed tiers is a precise error")
    void badBandwidthTier() throws Exception {
        FabricGateway fabric = catalogGateway("sp-aws", profile(
                List.of(spMetro("DC", "us-east-1", null)),
                List.of(apConfig(List.of(1000, 10000), false))));

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC)),
                List.of(provider("FCR-DC-to-aws", "FCR-DC", DC, 8000, "sp-aws", "us-east-1", CloudProviderType.AWS)));

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("not an allowed tier") && e.contains("8000")),
                () -> "expected a bad-bandwidth error: " + r.errors);
    }

    @Test
    @DisplayName("a bandwidth over the per-metro profile ceiling is a precise error")
    void badBandwidthCeiling() throws Exception {
        FabricGateway fabric = catalogGateway("sp-aws", profile(
                List.of(spMetro("DC", "us-east-1", 5000)),
                List.of(apConfig(null, true))));

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC)),
                List.of(provider("FCR-DC-to-aws", "FCR-DC", DC, 8000, "sp-aws", "us-east-1", CloudProviderType.AWS)));

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("exceeds the profile's maximum") && e.contains("5000")),
                () -> "expected a per-metro ceiling error: " + r.errors);
    }

    @Test
    @DisplayName("a service-profile UUID the catalog returns 404 for is flagged as not found")
    void unknownProfile() {
        ServiceProfiles sp = mock(ServiceProfiles.class);
        when(sp.getByUuid("sp-missing")).thenThrow(new EquinixNotFoundException("no such profile"));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.serviceProfiles()).thenReturn(sp);

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC)),
                List.of(provider("FCR-DC-to-aws", "FCR-DC", DC, 1000, "sp-missing", "us-east-1", CloudProviderType.AWS)));

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("sp-missing") && e.contains("not found in the catalog")),
                () -> "expected an unknown-profile error: " + r.errors);
    }

    @Test
    @DisplayName("a profile not offered in the connection's metro is flagged (new-market gap)")
    void profileNotInMetro() throws Exception {
        FabricGateway fabric = catalogGateway("sp-aws", profile(
                List.of(spMetro("SV", "us-west-1", null)),   // offered in SV only
                List.of(apConfig(null, true))));

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC)),
                List.of(provider("FCR-DC-to-aws", "FCR-DC", DC, 1000, "sp-aws", "us-west-1", CloudProviderType.AWS)));

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("is not offered in metro DC")),
                () -> "expected a metro-availability error: " + r.errors);
    }

    @Test
    @DisplayName("a seller region the profile does not offer in that metro is flagged")
    void invalidSellerRegion() throws Exception {
        FabricGateway fabric = catalogGateway("sp-aws", profile(
                List.of(spMetro("DC", "us-east-1", null)),
                List.of(apConfig(null, true))));

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC)),
                List.of(provider("FCR-DC-to-aws", "FCR-DC", DC, 1000, "sp-aws", "eu-west-1", CloudProviderType.AWS)));

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("seller region 'eu-west-1'")),
                () -> "expected a seller-region error: " + r.errors);
    }

    @Test
    @DisplayName("a fully in-catalog provider connection produces no catalog error")
    void catalogHappyPath() throws Exception {
        FabricGateway fabric = catalogGateway("sp-aws", profile(
                List.of(spMetro("DC", "us-east-1", 10000)),
                List.of(apConfig(List.of(1000, 8000, 10000), false))));

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC)),
                List.of(provider("FCR-DC-to-aws", "FCR-DC", DC, 8000, "sp-aws", "us-east-1", CloudProviderType.AWS)));

        assertTrue(r.errors.isEmpty(), () -> "a catalog-clean plan must have no errors: " + r.errors);
    }

    // ══════════════════════════════════════════════
    //  Layer 1 — router package ceilings
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("more connections than the router package allows is flagged")
    void tooManyConnectionsForPackage() {
        CloudRouterPackage pkg = mock(CloudRouterPackage.class);
        when(pkg.getVcCountMax()).thenReturn(2);
        when(pkg.getVcBandwidthMax()).thenReturn(null);
        FabricGateway fabric = routerPackageGateway(pkg);

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC)),
                List.of(
                        provider("FCR-DC-to-aws", "FCR-DC", DC, 1000, "sp-aws", "us-east-1", CloudProviderType.AWS),
                        provider("FCR-DC-to-azure", "FCR-DC", DC, 1000, "sp-az", "eastus", CloudProviderType.AZURE),
                        provider("FCR-DC-to-gcp", "FCR-DC", DC, 1000, "sp-gcp", "us-central1", CloudProviderType.GOOGLE_CLOUD)));

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("plans 3 connection(s), exceeding package STANDARD")),
                () -> "expected a VC-count ceiling error: " + r.errors);
    }

    @Test
    @DisplayName("a per-VC bandwidth over the router package ceiling is flagged")
    void connectionBandwidthOverPackageCeiling() {
        CloudRouterPackage pkg = mock(CloudRouterPackage.class);
        when(pkg.getVcCountMax()).thenReturn(50);
        when(pkg.getVcBandwidthMax()).thenReturn(5000);
        FabricGateway fabric = routerPackageGateway(pkg);

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC)),
                List.of(provider("FCR-DC-to-aws", "FCR-DC", DC, 8000, "sp-aws", "us-east-1", CloudProviderType.AWS)));

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("per-VC maximum") && e.contains("5000")),
                () -> "expected a per-VC bandwidth ceiling error: " + r.errors);
    }

    // ══════════════════════════════════════════════
    //  Layer 1 — local structural
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a duplicate resource name is flagged")
    void duplicateName() {
        PlanValidator.Result r = validate(null,
                List.of(router("FCR-DC", DC), router("FCR-DC", SV)),
                Collections.emptyList());

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("Duplicate resource name 'FCR-DC'")),
                () -> "expected a duplicate-name error: " + r.errors);
    }

    @Test
    @DisplayName("two connections assigned the same /30 subnet is flagged as an IP overlap")
    void ipOverlap() {
        PlannedRoutingProtocol a = PlannedRoutingProtocol.builder()
                .name("conn-a-BGP").connectionName("conn-a").type(RoutingProtocolType.BGP)
                .equinixPeerIpv4("10.100.0.1/30").customerPeerIpv4("10.100.0.2/30").build();
        PlannedRoutingProtocol b = PlannedRoutingProtocol.builder()
                .name("conn-b-BGP").connectionName("conn-b").type(RoutingProtocolType.BGP)
                .equinixPeerIpv4("10.100.0.1/30").customerPeerIpv4("10.100.0.2/30").build();

        PlanValidator.Result r = PlanValidator.validate(
                null, null, null,
                List.of(router("FCR-DC", DC)),
                Collections.emptyList(), Collections.emptyList(), List.of(a, b),
                65100L, null);

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("IP overlap")
                        && e.contains("conn-a") && e.contains("conn-b")),
                () -> "expected an IP overlap error: " + r.errors);
    }

    @Test
    @DisplayName("the same connection's DIRECT+BGP pair sharing a /30 is NOT an overlap")
    void samePairSharedSubnetIsFine() {
        PlannedRoutingProtocol direct = PlannedRoutingProtocol.builder()
                .name("conn-a-DIRECT").connectionName("conn-a").type(RoutingProtocolType.DIRECT)
                .equinixIfaceIpv4("10.100.0.1/30").build();
        PlannedRoutingProtocol bgp = PlannedRoutingProtocol.builder()
                .name("conn-a-BGP").connectionName("conn-a").type(RoutingProtocolType.BGP)
                .equinixPeerIpv4("10.100.0.1/30").customerPeerIpv4("10.100.0.2/30").build();

        PlanValidator.Result r = PlanValidator.validate(
                null, null, null,
                List.of(router("FCR-DC", DC)),
                Collections.emptyList(), Collections.emptyList(), List.of(direct, bgp),
                65100L, null);

        assertFalse(r.errors.stream().anyMatch(e -> e.contains("IP overlap")),
                () -> "a single connection's shared /30 must not be reported as overlap: " + r.errors);
    }

    @Test
    @DisplayName("an out-of-range customer ASN is flagged")
    void badAsn() {
        PlanValidator.Result r = PlanValidator.validate(
                null, null, null,
                List.of(router("FCR-DC", DC)),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                0L, null);

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("Customer ASN 0 is out of the valid range")),
                () -> "expected an ASN error: " + r.errors);
    }

    @Test
    @DisplayName("a name of 24+ characters is flagged (never learned from a 400)")
    void overlongName() {
        PlanValidator.Result r = validate(null,
                List.of(PlannedCloudRouter.builder()
                        .name("this-router-name-is-way-too-long").metroId(DC)
                        .packageCode(GatewayPackageCode.STANDARD).build()),
                Collections.emptyList());

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("characters") && e.contains("EQ-3142539")),
                () -> "expected an over-long-name error: " + r.errors);
    }

    // ══════════════════════════════════════════════
    //  Layer 1 — new-market gap
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a required cloud not available at a metro hosting a dependent workload is a new-market gap")
    void newMarketGap() {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());
        MetroRecommendation dc = MetroRecommendation.builder()
                .rank(1).metroId(DC).metroName("Ashburn").score(score).reasons(List.of("Primary"))
                .availableProviders(Collections.emptyList())   // GCP is NOT available here
                .build();
        WorkloadSpec gpu = WorkloadSpec.builder()
                .label("GPU").bandwidthMbps(1000)
                .dependsOnProviders(List.of(ProviderRequirement.builder().label("GCP").build()))
                .build();
        DeploymentTopology topology = new DeploymentTopology(List.of(
                WorkloadPlacement.builder().workloadLabel("GPU").assignedMetro(DC).reasoning("x").build()));

        PlanValidator.Result r = PlanValidator.validate(
                List.of(dc),
                OptimizationRequest.builder().workloads(List.of(gpu)).build(),
                topology,
                List.of(router("FCR-DC", DC)),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                65100L, null);

        assertTrue(r.errors.stream().anyMatch(e -> e.contains("New-market gap")
                        && e.contains("metro DC") && e.contains("GCP")),
                () -> "expected a new-market gap error: " + r.errors);
    }

    @Test
    @DisplayName("an available-but-unrequired provider absent at a metro is NOT a new-market gap")
    void unrequiredProviderIsNotAGap() {
        MetroScore score = new MetroScore(90.0, Collections.emptyList());
        MetroRecommendation dc = MetroRecommendation.builder()
                .rank(1).metroId(DC).metroName("Ashburn").score(score).reasons(List.of("Primary"))
                .availableProviders(Collections.emptyList())
                .build();
        // A workload with no provider dependency placed at DC.
        WorkloadSpec web = WorkloadSpec.builder().label("Web").bandwidthMbps(1000).build();
        DeploymentTopology topology = new DeploymentTopology(List.of(
                WorkloadPlacement.builder().workloadLabel("Web").assignedMetro(DC).reasoning("x").build()));

        PlanValidator.Result r = PlanValidator.validate(
                List.of(dc),
                OptimizationRequest.builder().workloads(List.of(web)).build(),
                topology,
                List.of(router("FCR-DC", DC)),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                65100L, null);

        assertTrue(r.errors.isEmpty(), () -> "no dependency means no new-market gap: " + r.errors);
    }

    // ══════════════════════════════════════════════
    //  Layer 2 — router dry-run
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("each planned Cloud Router is dry-run over the SDK cloudRouter surface")
    void routerDryRunInvoked() {
        CloudRouters cr = mock(CloudRouters.class);
        CloudRouterOperator.CloudRouterBuilder crb =
                mock(CloudRouterOperator.CloudRouterBuilder.class, org.mockito.Answers.RETURNS_SELF);
        when(cr.define()).thenReturn(crb);
        when(crb.create()).thenReturn(mock(CloudRouter.class));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.cloudRouters()).thenReturn(cr);

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC), router("FCR-SV", SV)),
                Collections.emptyList());

        // Two routers → two dry-run defines, each terminated with dryRun().create().
        verify(cr, org.mockito.Mockito.times(2)).define();
        verify(crb, org.mockito.Mockito.times(2)).dryRun();
        verify(crb, org.mockito.Mockito.times(2)).create();
        assertTrue(r.errors.isEmpty(), () -> "a passing router dry-run adds no error: " + r.errors);
    }

    @Test
    @DisplayName("a 403 on the router dry-run is a SKIPPED(reason), not an error — the plan stays valid")
    void routerDryRun403IsSkippedNotError() {
        // A brand-new customer whose credential is not entitled to the dry-run endpoint gets a 403. That
        // is an infeasibility, not a plan defect: it must NOT invalidate an otherwise-sound plan.
        FabricGateway fabric = dryRunThrowsGateway(
                new EquinixAuthorizationException("Authorization denied (HTTP 403)."));

        PlanValidator.Result r = validate(fabric,
                List.of(router("globalpay-NY", DC)), Collections.emptyList());

        assertTrue(r.errors.isEmpty(), () -> "a 403 must not become a plan error: " + r.errors);
        assertTrue(r.skipped.stream().anyMatch(s -> s.contains("globalpay-NY") && s.contains("403")),
                () -> "expected a router dry-run SKIP naming the router and the 403: " + r.skipped);
    }

    @Test
    @DisplayName("a 429 / 5xx / transport failure on the router dry-run is SKIPPED, never an error")
    void routerDryRunInfeasibilityIsSkipped() {
        FabricGateway rateLimited = dryRunThrowsGateway(
                new api.equinix.javasdk.core.exception.EquinixRateLimitException("Rate limit exceeded (HTTP 429)."));
        PlanValidator.Result rl = validate(rateLimited, List.of(router("FCR-DC", DC)), Collections.emptyList());
        assertTrue(rl.errors.isEmpty(), () -> "429 is infeasibility, not an error: " + rl.errors);
        assertTrue(rl.skipped.stream().anyMatch(s -> s.contains("FCR-DC") && s.contains("429")), () -> rl.skipped.toString());

        FabricGateway unreachable = dryRunThrowsGateway(new RuntimeException("connect timed out"));
        PlanValidator.Result tr = validate(unreachable, List.of(router("FCR-DC", DC)), Collections.emptyList());
        assertTrue(tr.errors.isEmpty(), () -> "a transport error is infeasibility, not an error: " + tr.errors);
        assertTrue(tr.skipped.stream().anyMatch(s -> s.contains("FCR-DC") && s.toLowerCase().contains("timeout")),
                () -> "expected a transport-timeout skip: " + tr.skipped);
    }

    @Test
    @DisplayName("a 400 validation rejection on the router dry-run IS an error and invalidates the plan")
    void routerDryRun400IsError() {
        // A well-formed request the API rejects for being wrong (400) is a real defect — an ERROR.
        FabricGateway fabric = dryRunThrowsGateway(
                new EquinixServiceException("Bad request", 400, "/fabric/v4/routers", null, null));

        PlanValidator.Result r = validate(fabric, List.of(router("FCR-DC", DC)), Collections.emptyList());

        assertTrue(r.errors.stream().anyMatch(e -> e.startsWith("Router dry-run validation warning for 'FCR-DC'")),
                () -> "a 400 rejection must be a plan error: " + r.errors);
        assertTrue(r.skipped.stream().noneMatch(s -> s.contains("Router dry-run skipped")),
                () -> "a 400 rejection must not be a skip: " + r.skipped);
    }

    // ══════════════════════════════════════════════
    //  Layer 1 — notifications required (EQ-3040013)
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a Cloud Router with no notification email is a Layer-1 error per router AND its live dry-run is never attempted")
    void missingNotificationIsStructuralErrorAndSkipsDryRun() {
        // A CloudRouters surface that WOULD serve a dry-run if one were attempted — so verifying it is
        // never called proves the doomed dry-run (which would 400 live on the mandatory $.notifications
        // field, EQ-3040013) is skipped, not sent.
        CloudRouters cr = mock(CloudRouters.class);
        CloudRouterOperator.CloudRouterBuilder crb =
                mock(CloudRouterOperator.CloudRouterBuilder.class, org.mockito.Answers.RETURNS_SELF);
        when(cr.define()).thenReturn(crb);
        when(crb.create()).thenReturn(mock(CloudRouter.class));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.cloudRouters()).thenReturn(cr);

        PlannedCloudRouter noNotify = PlannedCloudRouter.builder()
                .name("FCR-DC").metroId(DC).packageCode(GatewayPackageCode.STANDARD).build();
        PlannedCloudRouter blankNotify = PlannedCloudRouter.builder()
                .name("FCR-SV").metroId(SV).packageCode(GatewayPackageCode.STANDARD)
                .notificationEmail("   ").build();

        PlanValidator.Result r = validate(fabric, List.of(noNotify, blankNotify), Collections.emptyList());

        // A clear per-router structural error, naming each router and the fix — not a raw EQ-3040013.
        assertTrue(r.errors.stream().anyMatch(e -> e.contains("FCR-DC")
                        && e.contains("requires at least one notification recipient")
                        && e.contains("deployment.notifications")),
                () -> "expected a missing-notification error for FCR-DC: " + r.errors);
        assertTrue(r.errors.stream().anyMatch(e -> e.contains("FCR-SV")
                        && e.contains("requires at least one notification recipient")),
                () -> "a blank notification is flagged too, per router: " + r.errors);

        // The doomed live dry-run is NEVER attempted for a router that failed the notifications check.
        verify(cr, never()).define();
    }

    @Test
    @DisplayName("a Cloud Router WITH a notification email raises no notification error and proceeds to the live dry-run")
    void presentNotificationHasNoErrorAndDryRunProceeds() {
        CloudRouters cr = mock(CloudRouters.class);
        CloudRouterOperator.CloudRouterBuilder crb =
                mock(CloudRouterOperator.CloudRouterBuilder.class, org.mockito.Answers.RETURNS_SELF);
        when(cr.define()).thenReturn(crb);
        when(crb.create()).thenReturn(mock(CloudRouter.class));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.cloudRouters()).thenReturn(cr);

        // router() carries a notification email, so the notifications check passes.
        PlanValidator.Result r = validate(fabric, List.of(router("FCR-DC", DC)), Collections.emptyList());

        assertTrue(r.errors.stream().noneMatch(e -> e.contains("notification recipient")),
                () -> "a router with a notification email must not raise a notification error: " + r.errors);
        // The live router dry-run proceeds exactly as before, stamping the notification onto the body.
        verify(cr).define();
        verify(crb).notification(any(), any());
        verify(crb).dryRun();
        verify(crb).create();
    }

    // ══════════════════════════════════════════════
    //  Skipped — offline / bare-stub infeasibility
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("an offline gateway emits a SKIPPED note for EVERY live layer — never silence, never an error")
    void offlineGatewayEmitsSkippedForEachLiveLayer() {
        // A bare Mockito stub returns null from serviceProfiles()/cloudRouters()/connections(): every
        // live layer must be recorded as skipped (with a reason) rather than silently doing nothing.
        FabricGateway bareStub = mock(FabricGateway.class);

        PlanValidator.Result r = validate(bareStub,
                List.of(router("FCR-DC", DC)),
                List.of(provider("FCR-DC-to-aws", "FCR-DC", DC, 1000, "sp-aws", "us-east-1", CloudProviderType.AWS)));

        assertTrue(r.errors.isEmpty(), () -> "an offline run is never a hard error: " + r.errors);
        assertTrue(r.skipped.stream().anyMatch(s -> s.contains("Catalog validation skipped")),
                () -> "the catalog layer must be skipped-with-a-reason offline: " + r.skipped);
        assertTrue(r.skipped.stream().anyMatch(s -> s.contains("Package-ceiling checks skipped")),
                () -> "the package-ceiling layer must be skipped offline: " + r.skipped);
        assertTrue(r.skipped.stream().anyMatch(s -> s.contains("Live router dry-run skipped")),
                () -> "the router dry-run layer must be skipped offline: " + r.skipped);
        // A to-be-created-FCR connection is still DEFERRED (not skipped) even offline.
        assertTrue(r.deferred.stream().anyMatch(d -> d.contains("FCR-DC-to-aws")),
                () -> "a to-be-created connection remains deferred offline: " + r.deferred);
    }

    @Test
    @DisplayName("a pre-existing-endpoint connection with a null connections surface is SKIPPED(offline), not DEFERRED")
    void preExistingEndpointOfflineIsSkippedNotDeferred() {
        // The A-side endpoint already exists, so this connection is NOT waiting on a to-be-created FCR;
        // when the connections surface is unavailable it must be skipped (offline), never deferred with
        // the false "Cloud Router does not exist yet" reason.
        PlannedConnection existing = PlannedConnection.builder()
                .name("to-aws-existing").connectionType(ConnectionType.EVPL_VC).purpose(ConnectionPurpose.PROVIDER)
                .bandwidthMbps(1000).aSideMetro(DC).aSideRouterName("FCR-DC")
                .zSideServiceProfileUuid("sp-aws").zSideProviderLabel("AWS").zSideSellerRegion("us-east-1")
                .zSideCloudType(CloudProviderType.AWS)
                .zSideAuthenticationKey("123456789012")
                .zSideVlanTag(1000)
                .aSideExistingRouterUuid("cr-existing-uuid")
                .build();
        FabricGateway bareStub = mock(FabricGateway.class); // connections() returns null → offline

        PlanValidator.Result r = validate(bareStub, List.of(router("FCR-DC", DC)), List.of(existing));

        assertTrue(r.deferred.stream().noneMatch(d -> d.contains("to-aws-existing")),
                () -> "a pre-existing-endpoint connection must NOT be deferred with a false FCR reason: " + r.deferred);
        assertTrue(r.skipped.stream().anyMatch(s -> s.contains("to-aws-existing")
                        && s.toLowerCase().contains("offline")),
                () -> "expected an offline SKIP for the pre-existing-endpoint connection: " + r.skipped);
        assertTrue(r.errors.isEmpty(), () -> "offline is never an error: " + r.errors);
    }

    // ══════════════════════════════════════════════
    //  Layer 3 — connection endpoint dispatch
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("a to-be-created-FCR connection is DEFERRED, never dry-run, and its inputs are enumerated")
    void toBeCreatedConnectionIsDeferred() {
        Connections connections = mock(Connections.class);
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.connections()).thenReturn(connections);

        PlanValidator.Result r = validate(fabric,
                List.of(router("FCR-DC", DC)),
                List.of(provider("FCR-DC-to-aws", "FCR-DC", DC, 1000, "sp-aws", "us-east-1", CloudProviderType.AWS)));

        // No live connection dry-run is attempted for a to-be-created FCR.
        verify(connections, never()).define(any());
        assertTrue(r.errors.isEmpty(), () -> "a structurally-fine deferred connection is not an error: " + r.errors);
        assertTrue(r.deferred.stream().anyMatch(d -> d.contains("FCR-DC-to-aws")),
                () -> "expected a deferred note: " + r.deferred);

        ConnectionInputRequirement req = r.requiredInputs.stream()
                .filter(x -> x.getConnectionName().equals("FCR-DC-to-aws")).findFirst().orElseThrow();
        assertEquals(CloudProviderType.AWS, req.getCloudType());
        assertTrue(req.getAuthenticationKeyLabel().contains("AWS Account ID"));
        assertFalse(req.isAuthenticationKeyProvided());
    }

    @Test
    @DisplayName("a connection with a pre-existing endpoint + auth key triggers a REAL live dry-run at plan time")
    void preExistingEndpointTriggersRealDryRun() {
        Connections connections = mock(Connections.class);
        ConnectionOperator.ConnectionBuilder builder =
                mock(ConnectionOperator.ConnectionBuilder.class, org.mockito.Answers.RETURNS_SELF);
        when(connections.define(any())).thenReturn(builder);
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.connections()).thenReturn(connections);

        PlannedConnection existing = PlannedConnection.builder()
                .name("FCR-DC-to-aws").connectionType(ConnectionType.EVPL_VC).purpose(ConnectionPurpose.PROVIDER)
                .bandwidthMbps(1000).aSideMetro(DC).aSideRouterName("FCR-DC")
                .zSideServiceProfileUuid("sp-aws").zSideProviderLabel("AWS").zSideSellerRegion("us-east-1")
                .zSideCloudType(CloudProviderType.AWS)
                .zSideAuthenticationKey("123456789012")     // customer supplied
                .zSideVlanTag(1000)
                .aSideExistingRouterUuid("cr-existing-uuid") // real, pre-existing A-side endpoint
                .build();

        PlanValidator.Result r = validate(fabric, List.of(router("FCR-DC", DC)), List.of(existing));

        // A real connection dry-run was assembled and executed at PLAN time.
        verify(connections).define(ConnectionType.EVPL_VC);
        verify(builder).dryRun();
        verify(builder).create();
        // A real dry-run means it is validated now, not deferred.
        assertTrue(r.deferred.stream().noneMatch(d -> d.contains("FCR-DC-to-aws")),
                () -> "a pre-existing-endpoint connection is validated now, not deferred: " + r.deferred);

        ConnectionInputRequirement req = r.requiredInputs.stream()
                .filter(x -> x.getConnectionName().equals("FCR-DC-to-aws")).findFirst().orElseThrow();
        assertTrue(req.isAuthenticationKeyProvided(), "the key was supplied, so it is marked provided");
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private static PlanValidator.Result validate(
            FabricGateway fabric, List<PlannedCloudRouter> routers, List<PlannedConnection> connections) {
        return PlanValidator.validate(
                null, null, null,
                routers, connections, Collections.emptyList(), Collections.emptyList(),
                65100L, fabric);
    }

    private static PlannedCloudRouter router(String name, MetroId metro) {
        // Fabric requires a notification recipient on every Cloud Router, so a plan-valid router carries
        // one. Tests that exercise the missing-notification path build their routers explicitly instead.
        return PlannedCloudRouter.builder()
                .name(name).metroId(metro).packageCode(GatewayPackageCode.STANDARD)
                .notificationEmail("noc@example.com").build();
    }

    private static PlannedConnection provider(String name, String routerName, MetroId metro, int bw,
                                              String spUuid, String region, CloudProviderType type) {
        return PlannedConnection.builder()
                .name(name).connectionType(ConnectionType.EVPL_VC).purpose(ConnectionPurpose.PROVIDER)
                .bandwidthMbps(bw).aSideMetro(metro).aSideRouterName(routerName)
                .zSideServiceProfileUuid(spUuid)
                .zSideProviderLabel(type == CloudProviderType.AWS ? "AWS" : type.getProviderName())
                .zSideSellerRegion(region).zSideCloudType(type)
                .build();
    }

    private static FabricGateway catalogGateway(String uuid, ServiceProfile profile) {
        ServiceProfiles sp = mock(ServiceProfiles.class);
        when(sp.getByUuid(uuid)).thenReturn(profile);
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.serviceProfiles()).thenReturn(sp);
        return fabric;
    }

    /**
     * A gateway whose Cloud Router dry-run {@code create()} throws the given exception, so the Layer-2
     * classifier decides between SKIPPED (infeasibility) and ERROR (rejection).
     */
    private static FabricGateway dryRunThrowsGateway(RuntimeException toThrow) {
        CloudRouters cr = mock(CloudRouters.class);
        CloudRouterOperator.CloudRouterBuilder crb =
                mock(CloudRouterOperator.CloudRouterBuilder.class, org.mockito.Answers.RETURNS_SELF);
        when(cr.define()).thenReturn(crb);
        when(crb.create()).thenThrow(toThrow);
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.cloudRouters()).thenReturn(cr);
        return fabric;
    }

    private static FabricGateway routerPackageGateway(CloudRouterPackage pkg) {
        CloudRouters cr = mock(CloudRouters.class);
        when(cr.routerPackageByCode(any())).thenReturn(pkg);
        // A succeeding router dry-run so Layer 2 adds no noise to the package-ceiling assertions.
        CloudRouterOperator.CloudRouterBuilder crb =
                mock(CloudRouterOperator.CloudRouterBuilder.class, org.mockito.Answers.RETURNS_SELF);
        when(cr.define()).thenReturn(crb);
        when(crb.create()).thenReturn(mock(CloudRouter.class));
        FabricGateway fabric = mock(FabricGateway.class);
        when(fabric.cloudRouters()).thenReturn(cr);
        return fabric;
    }

    private static ServiceProfile profile(List<ServiceProfileMetro> metros, List<AccessPointTypeConfig> configs) {
        ServiceProfile profile = mock(ServiceProfile.class);
        when(profile.metros()).thenReturn(metros);
        when(profile.getAccessPointTypeConfigs()).thenReturn(configs);
        return profile;
    }

    private static ServiceProfileMetro spMetro(String code, String sellerRegion, Integer vcBandwidthMax)
            throws Exception {
        StringBuilder json = new StringBuilder("{\"code\":\"").append(code)
                .append("\",\"sellerRegions\":{\"").append(sellerRegion).append("\":\"").append(sellerRegion)
                .append("\"}");
        if (vcBandwidthMax != null) {
            json.append(",\"vcBandwidthMax\":").append(vcBandwidthMax);
        }
        json.append("}");
        return MAPPER.readValue(json.toString(), ServiceProfileMetro.class);
    }

    private static AccessPointTypeConfig apConfig(List<Integer> supportedBandwidths, boolean allowCustom)
            throws Exception {
        StringBuilder json = new StringBuilder("{\"allowCustomBandwidth\":").append(allowCustom);
        if (supportedBandwidths != null) {
            json.append(",\"supportedBandwidths\":[");
            for (int i = 0; i < supportedBandwidths.size(); i++) {
                if (i > 0) json.append(",");
                json.append(supportedBandwidths.get(i));
            }
            json.append("]");
        }
        json.append("}");
        return MAPPER.readValue(json.toString(), AccessPointTypeConfig.class);
    }
}
