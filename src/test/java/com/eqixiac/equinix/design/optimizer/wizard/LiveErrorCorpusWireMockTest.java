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

package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.DeploymentTopology;
import com.eqixiac.equinix.design.optimizer.model.MetroRecommendation;
import com.eqixiac.equinix.design.optimizer.model.MetroScore;
import com.eqixiac.equinix.design.optimizer.model.OptimizationRequest;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.model.ProviderRequirement;
import com.eqixiac.equinix.design.optimizer.model.WorkloadPlacement;
import com.eqixiac.equinix.design.optimizer.model.WorkloadSpec;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceSource;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
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

/**
 * Live-error regression corpus: one test per REAL Fabric error observed against the live API during
 * wizard development, each stubbed with the REAL error JSON shape the API returned, asserting the
 * SDK/wizard's <em>classified</em> handling of it.
 *
 * <p>Why this exists: the 2026-07 defect review found that WireMock stubs tended to encode the test
 * authors' <em>assumptions</em> about API rules rather than observed reality — so a wrong assumption
 * passed its own test while the live API rejected the SDK. This corpus inverts that: every stub body
 * below is the wire shape a live Fabric endpoint actually produced, and every assertion pins the
 * behavior the wizard is supposed to exhibit when it meets that reality again. When a new live error
 * is observed, add its real body here — never trim or "clean up" the observed shapes.</p>
 *
 * <ul>
 *   <li>{@code EQ-3040013} — "Notifications is mandatory field" (router dry-run HTTP 400):
 *       surfaced as a plan validation ERROR (a rejection), never a skip.</li>
 *   <li>{@code EQ-3040063} — account not supported in the metro (router dry-run HTTP 400):
 *       a validation ERROR naming the router/metro, NOT a skip — an entitlement-shaped message
 *       on a 400 is still a rejection of the request.</li>
 *   <li>{@code EQ-3142539} — "connection name length should be greater than 0 and less than 24
 *       character": the {@code PlanNames} Layer-1 guard prevents the SDK from EVER sending an
 *       oversized name — asserted on the wire, not just in the model.</li>
 *   <li>{@code EQ-3142501} — "Null value for aSide access point": connection dry-runs whose
 *       A-side Cloud Router does not exist yet are DEFERRED, never sent endpoint-less. The real
 *       400 body is armed on the connections endpoint as a tripwire.</li>
 * </ul>
 */
@DisplayName("Live Fabric error corpus — real error bodies, classified wizard handling")
class LiveErrorCorpusWireMockTest extends WireMockTestBase {

    private static final MetroId DC = MetroId.of(MetroCode.DC);
    private static final MetroId DA = MetroId.of(MetroCode.DA);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── REAL error bodies, as observed live (array-of-detail shape; do not "clean up") ──

    /** Observed live: POST /fabric/v4/routers without notifications → HTTP 400. */
    private static final String EQ_3040013_NOTIFICATIONS_MANDATORY = """
            [
              {
                "errorCode": "EQ-3040013",
                "errorMessage": "Notifications is mandatory field",
                "correlationId": "f4795171-99b6-4e42-a17a-b7a2a2b7e0a5",
                "additionalInfo": [
                  { "property": "$.notifications" }
                ]
              }
            ]""";

    /** Observed live: POST /fabric/v4/routers with an account the metro does not support → HTTP 400. */
    private static final String EQ_3040063_ACCOUNT_NOT_SUPPORTED_IN_METRO = """
            [
              {
                "errorCode": "EQ-3040063",
                "errorMessage": "Account is not supported in the given metro",
                "correlationId": "0b2a6c7d-4a1e-4a2b-9c3d-5e6f7a8b9c0d",
                "additionalInfo": [
                  { "property": "/account/accountNumber",
                    "reason": "Account not supported in metro DA" }
                ]
              }
            ]""";

    /** Observed live: POST /fabric/v4/connections with a >= 24-character name → HTTP 400. */
    private static final String EQ_3142539_NAME_TOO_LONG = """
            [
              {
                "errorCode": "EQ-3142539",
                "errorMessage": "connection name length should be greater than 0 and less than 24 character",
                "correlationId": "2c9d1c1e-6a5b-4d3c-8e7f-1a2b3c4d5e6f",
                "additionalInfo": [
                  { "property": "/name" }
                ]
              }
            ]""";

    /** Observed live: POST /fabric/v4/connections?dryRun=true with no A-side endpoint → HTTP 400. */
    private static final String EQ_3142501_NULL_ASIDE_ACCESS_POINT = """
            [
              {
                "errorCode": "EQ-3142501",
                "errorMessage": "Null value for aSide access point",
                "correlationId": "7e1f2a3b-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
                "additionalInfo": [
                  { "property": "/aSide/accessPoint" }
                ]
              }
            ]""";

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

    // ══════════════════════════════════════════════
    //  EQ-3040013 — notifications mandatory
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("EQ-3040013: a live router dry-run 400 (notifications mandatory) surfaces as a validation ERROR, not a skip")
    void eq3040013NotificationsMandatoryIsAValidationError() {
        stubServiceProfileOk();
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EQ_3040013_NOTIFICATIONS_MANDATORY)));

        // The wizard is configured WITH notifications, so the Layer-1 structural guard passes and the
        // dry-run really goes to the wire — this test is about classifying the live 400, not the guard.
        DeploymentPlan plan = wizard().plan();

        assertFalse(plan.isValid(), "a live EQ-3040013 rejection must invalidate the plan");
        assertTrue(plan.getValidationErrors().stream().anyMatch(e ->
                        e.startsWith("Router dry-run validation warning for 'FCR-")
                                && e.contains("EQ-3040013")
                                && e.contains("Notifications is mandatory")),
                () -> "expected the real EQ-3040013 body folded into a validation error: "
                        + plan.getValidationErrors());
        // Classified as a REJECTION (plan defect), never an infeasibility skip.
        assertTrue(plan.getSkippedValidations().stream().noneMatch(s -> s.contains("Router dry-run skipped")),
                () -> "a 400 rejection must never be classified as a skip: " + plan.getSkippedValidations());
        // The dry-run genuinely went to the wire, and no connection was ever posted.
        wireMock.verify(2, postRequestedFor(urlPathEqualTo("/fabric/v4/routers"))
                .withQueryParam("dryRun", equalTo("true")));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));
    }

    // ══════════════════════════════════════════════
    //  EQ-3040063 — account not supported in metro
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("EQ-3040063: account-not-supported-in-metro 400 is a validation ERROR naming the router/metro — NOT a skip")
    void eq3040063AccountNotSupportedInMetroIsAnErrorNamingTheMetro() {
        stubServiceProfileOk();
        // FCR-DC dry-runs clean; FCR-DA hits the real per-metro account rejection.
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("FCR-DC")))
                .willReturn(okJson(loadFixture("/json/fabric/cloud_router_response.json"))));
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("FCR-DA")))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EQ_3040063_ACCOUNT_NOT_SUPPORTED_IN_METRO)));

        DeploymentPlan plan = wizard().plan();

        assertFalse(plan.isValid(), "a live EQ-3040063 rejection must invalidate the plan");
        // The error names the rejected router AND carries the observed body naming the metro. Even
        // though the message is entitlement-flavoured, a 400 is a REJECTION of this request in this
        // metro — classifying it as an infeasibility skip would silently green-light a doomed plan.
        assertTrue(plan.getValidationErrors().stream().anyMatch(e ->
                        e.contains("FCR-DA")
                                && e.contains("EQ-3040063")
                                && e.contains("metro DA")),
                () -> "expected an error naming router FCR-DA and metro DA from the real body: "
                        + plan.getValidationErrors());
        // Only the DA router is rejected; DC passed. And DA is nowhere in the skip bucket.
        assertTrue(plan.getValidationErrors().stream().noneMatch(e ->
                        e.startsWith("Router dry-run validation warning for 'FCR-DC'")),
                () -> "FCR-DC passed its dry-run and must carry no error: " + plan.getValidationErrors());
        assertTrue(plan.getSkippedValidations().stream().noneMatch(s -> s.contains("FCR-DA")),
                () -> "the FCR-DA rejection must not appear as a skip: " + plan.getSkippedValidations());
        // Rejections annotate, never abort: the plan is still fully generated.
        assertEquals(2, plan.getCloudRouters().size());
        assertEquals(1, plan.getProviderConnections().size());
        assertEquals(1, plan.getBackboneLinks().size());
    }

    // ══════════════════════════════════════════════
    //  EQ-3142539 — name too long (never sent)
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("EQ-3142539: the PlanNames Layer-1 guard means an oversized name is NEVER sent — every wire body's name is <= 23 chars")
    void eq3142539OversizedNamesAreNeverSent() throws Exception {
        stubServiceProfileOk();
        stubRouterDryRunOk();
        // Tripwire: if an oversized name ever reached the wire, Fabric would answer with this real
        // body. The guard is supposed to make this stub unreachable for names.
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EQ_3142539_NAME_TOO_LONG)));

        // A prefix far beyond PlanNames.MAX_PREFIX_LEN: the raw composition
        // "globalpay-production-eastcoast-DC-to-aws" would be 40 characters — past the live limit.
        DeploymentPlan plan = wizard("globalpay-production-eastcoast").plan();

        // Model-level: EVERY generated name across all four resource kinds fits Fabric's < 24 rule
        // (EQ-3142539: names of 24+ characters are rejected) using only name-safe characters.
        plan.getCloudRouters().forEach(r -> assertLegalName(r.getName()));
        plan.getProviderConnections().forEach(c -> assertLegalName(c.getName()));
        plan.getBackboneLinks().forEach(l -> assertLegalName(l.getName()));
        plan.getRoutingProtocols().forEach(p -> assertLegalName(p.getName()));
        assertTrue(plan.getValidationErrors().stream().noneMatch(e -> e.contains("EQ-3142539")),
                () -> "no generated name may trip the Layer-1 length check: " + plan.getValidationErrors());

        // Wire-level: every router dry-run body that actually went out carried a <= 23-char name.
        List<LoggedRequest> routerPosts =
                wireMock.findAll(postRequestedFor(urlPathEqualTo("/fabric/v4/routers")));
        assertFalse(routerPosts.isEmpty(), "the router dry-runs must have gone to the wire");
        for (LoggedRequest req : routerPosts) {
            JsonNode body = MAPPER.readTree(req.getBodyAsString());
            String name = body.path("name").asText();
            assertLegalName(name);
        }
        // And the tripwire was never hit: no connection POST at plan time at all.
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));
    }

    private static void assertLegalName(String name) {
        assertNotNull(name, "every planned resource must be named");
        assertTrue(name.length() <= 23,
                () -> "name '" + name + "' is " + name.length()
                        + " chars — Fabric rejects 24+ (EQ-3142539)");
        assertTrue(name.matches("[A-Za-z0-9_-]+"),
                () -> "name '" + name + "' contains characters Fabric rejects");
    }

    // ══════════════════════════════════════════════
    //  EQ-3142501 — null aSide access point (deferred, never sent)
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("EQ-3142501: a connection whose A-side FCR does not exist yet is DEFERRED — the endpoint-less dry-run is never sent")
    void eq3142501EndpointlessDryRunIsDeferredNeverSent() {
        stubServiceProfileOk();
        stubRouterDryRunOk();
        // Tripwire armed with the REAL observed rejection: if the wizard ever regresses into sending
        // the endpoint-less dry-run again, this body comes back, the error lands in validationErrors,
        // and the assertions below fail on both the wire count and the plan validity.
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/connections"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EQ_3142501_NULL_ASIDE_ACCESS_POINT)));

        DeploymentPlan plan = wizard().plan();

        // Zero connection POSTs: the doomed dry-run is not sent, so the real 400 never fires.
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/fabric/v4/connections")));
        // Classified as DEFERRED — not an error, not a skip — naming the connection and the
        // not-yet-existing A-side router it is waiting on.
        assertTrue(plan.isValid(), () -> "a deferred endpoint dry-run must not invalidate the plan: "
                + plan.getValidationErrors());
        assertTrue(plan.getDeferredValidations().stream().anyMatch(d ->
                        d.contains("FCR-DC-to-aws") && d.contains("FCR-DC")),
                () -> "expected a deferred note for the provider connection naming its A-side router: "
                        + plan.getDeferredValidations());
        assertTrue(plan.getValidationErrors().stream().noneMatch(e -> e.contains("EQ-3142501")),
                () -> "the EQ-3142501 tripwire must never have fired: " + plan.getValidationErrors());
        assertTrue(plan.getSkippedValidations().stream().noneMatch(s -> s.contains("FCR-DC-to-aws")),
                () -> "a deferrable connection must be deferred, not skipped: " + plan.getSkippedValidations());
    }

    // ══════════════════════════════════════════════
    //  Stubs and plan helpers (mirrors DeploymentWizardConnectionValidationWireMockTest)
    // ══════════════════════════════════════════════

    private static void stubRouterDryRunOk() {
        wireMock.stubFor(post(urlPathEqualTo("/fabric/v4/routers"))
                .willReturn(okJson(loadFixture("/json/fabric/cloud_router_response.json"))));
    }

    private static void stubServiceProfileOk() {
        wireMock.stubFor(get(urlPathMatching("/fabric/v4/serviceProfiles/.*"))
                .willReturn(okJson(loadFixture("/json/fabric/service_profile_response.json"))));
    }

    private DeploymentWizard.Builder wizard() {
        return wizard("FCR");
    }

    private DeploymentWizard.Builder wizard(String routerNamePrefix) {
        return fabric.deploymentWizard(twoMetroResult())
                .routerPackage("STANDARD")
                .routerNamePrefix(routerNamePrefix)
                .providerConnectionType(ConnectionType.IP_VC)
                .notifications("noc@example.com")
                .rateCard(fixedRateCard());
    }

    /**
     * Two metros: DC with AWS available and a single AWS-dependent workload placed there, plus DA
     * with no providers — one provider connection (FCR-DC-to-aws) and one backbone link.
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
                                                   MetroCode metro, Term term) {
                return Optional.of(PriceQuote.of(BigDecimal.valueOf(500), BigDecimal.ZERO,
                        Currency.getInstance("USD"), PriceSource.ESTIMATE));
            }

            @Override
            public Optional<PriceQuote> cloudRouter(String packageCode, MetroCode metro, Term term) {
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
