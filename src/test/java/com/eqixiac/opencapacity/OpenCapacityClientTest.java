/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eqixiac.opencapacity.model.AnswerDate;
import com.eqixiac.opencapacity.model.Capabilities;
import com.eqixiac.opencapacity.model.CapacityAnswer;
import com.eqixiac.opencapacity.model.CapacityRequirement;
import com.eqixiac.opencapacity.model.CloseReason;
import com.eqixiac.opencapacity.model.Confidence;
import com.eqixiac.opencapacity.model.ConformanceClass;
import com.eqixiac.opencapacity.model.DeliveryType;
import com.eqixiac.opencapacity.model.DensityRange;
import com.eqixiac.opencapacity.model.DisclosureLevel;
import com.eqixiac.opencapacity.model.Event;
import com.eqixiac.opencapacity.model.EventReason;
import com.eqixiac.opencapacity.model.Feature;
import com.eqixiac.opencapacity.model.FlexibleDate;
import com.eqixiac.opencapacity.model.Grade;
import com.eqixiac.opencapacity.model.InterconnectionScope;
import com.eqixiac.opencapacity.model.Location;
import com.eqixiac.opencapacity.model.LocationInfo;
import com.eqixiac.opencapacity.model.LocationLevel;
import com.eqixiac.opencapacity.model.LocationResult;
import com.eqixiac.opencapacity.model.NotifyOn;
import com.eqixiac.opencapacity.model.Party;
import com.eqixiac.opencapacity.model.Power;
import com.eqixiac.opencapacity.model.PowerCurrent;
import com.eqixiac.opencapacity.model.PowerFeed;
import com.eqixiac.opencapacity.model.Reason;
import com.eqixiac.opencapacity.model.RedundancyLevel;
import com.eqixiac.opencapacity.model.Sharing;
import com.eqixiac.opencapacity.model.SignalState;
import com.eqixiac.opencapacity.model.SubscriptionState;
import com.eqixiac.opencapacity.model.TimingStatus;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencapacity.reference.ReferenceProvider;

/**
 * Runs the client against the in-memory reference provider, which was written without any of the
 * client's classes. Every number in these tests comes from the provider's synthetic inventory.
 */
class OpenCapacityClientTest {

    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    private ReferenceProvider provider;
    private OpenCapacity capacity;

    @BeforeEach
    void start() throws IOException {
        provider = ReferenceProvider.start().clock(Clock.fixed(NOW, ZoneOffset.UTC));
        capacity = OpenCapacity.builder()
                .endpoint(provider.endpoint() + "/")
                .credentials(ReferenceProvider.CLIENT_ID, ReferenceProvider.CLIENT_SECRET)
                .timeout(Duration.ofSeconds(10))
                .build();
    }

    @AfterEach
    void stop() {
        provider.close();
    }

    private static CapacityRequirement.Builder liquidCooledCabinets(int cabinets) {
        return CapacityRequirement.define()
                .addLocation(Location.metro("FR"))
                .cabinets(cabinets)
                .density(DensityRange.kw().target(20).max(40).create())
                .addFeature(Feature.LIQUID_COOLING, true)
                .addFeature(Feature.PRIVATE_SUITE, false);
    }

    // ------------------------------------------------------------------ configuration

    @Test
    void buildFailsWithoutAnEndpoint() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> OpenCapacity.builder().build());
        assertTrue(failure.getMessage().contains("no default host"), failure.getMessage());
    }

    @Test
    void buildRefusesPlainHttpToAnotherHost() {
        assertThrows(IllegalStateException.class,
                () -> OpenCapacity.builder().endpoint("http://provider.example/capacity/v1").build());
    }

    @Test
    void authenticatedCallFailsWithoutCredentials() {
        OpenCapacity anonymous = OpenCapacity.builder().endpoint(provider.endpoint()).build();
        assertEquals("1.0.0", anonymous.capabilities().specVersion());
        assertThrows(IllegalStateException.class, () -> anonymous.locations().list());
    }

    @Test
    void wrongSecretFailsAtTheTokenEndpoint() {
        OpenCapacity wrong = OpenCapacity.builder().endpoint(provider.endpoint())
                .tokenUrl(provider.tokenUrl()).credentials(ReferenceProvider.CLIENT_ID, "wrong").build();
        OpenCapacityException failure = assertThrows(OpenCapacityException.class, () -> wrong.locations().list());
        assertEquals(401, failure.status());
    }

    @Test
    void newTokenIsRequestedOnceAfterA401() {
        assertFalse(capacity.locations().list().isEmpty());
        provider.revokeTokens();
        assertFalse(capacity.locations().list().isEmpty());
    }

    @Test
    void rejectedCustomTokenSurfacesAsAnException() {
        OpenCapacity custom = OpenCapacity.builder().endpoint(provider.endpoint()).tokenSource(() -> "not-a-token").build();
        OpenCapacityException failure = assertThrows(OpenCapacityException.class, () -> custom.locations().list());
        assertEquals(401, failure.status());
        assertEquals("UNAUTHENTICATED", failure.code().orElseThrow());
    }

    // ------------------------------------------------------------------ discovery

    @Test
    void capabilitiesDescribeTheProvider() {
        Capabilities capabilities = capacity.capabilities();
        assertTrue(capabilities.supports(ConformanceClass.SUBSCRIPTIONS));
        assertTrue(capabilities.acceptsAreas());
        assertEquals(LocationLevel.METRO, capabilities.finestAnswerLevel());
        assertTrue(capabilities.features().contains(Feature.LIQUID_COOLING));
        assertEquals(DisclosureLevel.ALWAYS, capabilities.disclosure().density());
        assertEquals(DisclosureLevel.NEVER, capabilities.disclosure().power());
        assertEquals(provider.tokenUrl(), capabilities.authentication().tokenUrl());
        assertEquals(10, capabilities.limits().maxLocationsPerRequirement());
        assertTrue(capabilities.deliveryTypes().contains(DeliveryType.PULL));
    }

    @Test
    void locationsListFollowsPagesAndFilters() {
        List<LocationInfo> all = capacity.locations().list();
        assertEquals(12, all.size());
        List<LocationInfo> metros = capacity.locations().list(LocationLevel.METRO);
        assertEquals(List.of("FR", "AM", "LD"), metros.stream().map(LocationInfo::code).toList());
        assertEquals("DEFRA", metros.get(0).standardCode());
        assertEquals(Location.metro("FR"), metros.get(0).toLocation());
        List<LocationInfo> inCampus = capacity.locations().list(LocationLevel.FACILITY, "FR-CAMPUS-1");
        assertEquals(List.of("FR-A", "FR-B"), inCampus.stream().map(LocationInfo::code).toList());
    }

    // ------------------------------------------------------------------ queries

    @Test
    void queryForCapacityInServiceIsAvailableNow() {
        CapacityAnswer answer = capacity.queries().ask(liquidCooledCabinets(40).create(), "ref-1");
        assertEquals("ref-1", answer.reference());
        assertEquals(Grade.AVAILABLE, answer.grade());
        assertEquals(NOW.plus(Duration.ofDays(1)), answer.validUntil());
        assertFalse(answer.isExpired(NOW));
        LocationResult result = answer.results().get(0);
        assertEquals(Location.metro("FR"), result.location());
        assertEquals(TimingStatus.IN_SERVICE, result.timing().status());
        assertTrue(result.timing().availableForService().isNow());
        assertEquals(80.0, result.density().max());
        assertEquals(40, result.cabinets().min());
        assertNull(result.cabinets().max(), "the provider does not reveal how much is left");
        assertNull(result.power(), "the banded profile does not disclose a power band");
        assertEquals(Grade.AVAILABLE, result.supportedFeature(Feature.LIQUID_COOLING).support());
        assertEquals(Grade.UNAVAILABLE, result.supportedFeature(Feature.PRIVATE_SUITE).support());
        assertEquals(Grade.NOT_DISCLOSED, result.supportedFeature(Feature.IMMERSION_COOLING).support());
        assertEquals(1, answer.resultsGraded(Grade.AVAILABLE).size());
    }

    @Test
    void plannedCapacityIsAvailableByTheNeededDateAndPossibleAfterIt() {
        CapacityAnswer inTime = capacity.queries().ask(
                liquidCooledCabinets(150).neededForService(FlexibleDate.year(2027)).create());
        LocationResult planned = inTime.results().get(0);
        assertEquals(Grade.AVAILABLE, planned.grade());
        assertEquals(TimingStatus.PLANNED, planned.timing().status());
        assertEquals(AnswerDate.of(FlexibleDate.quarter(2027, 3)), planned.timing().availableForService());

        provider.disclosure(ReferenceProvider.Disclosure.FULL);
        CapacityAnswer late = capacity.queries().ask(
                liquidCooledCabinets(150).neededForService(FlexibleDate.month(2027, 2)).create());
        assertEquals(Grade.POSSIBLE, late.grade());
        assertEquals(List.of(Reason.TIMING), late.results().get(0).reasons());
        assertEquals(Grade.AVAILABLE, late.results().get(0).supportedFeature(Feature.LIQUID_COOLING).support(),
                "a positive grade grades each required feature AVAILABLE or POSSIBLE");
    }

    @Test
    void gradeOnlyProfileReturnsNoBands() {
        provider.disclosure(ReferenceProvider.Disclosure.GRADE_ONLY);
        LocationResult result = capacity.queries().ask(liquidCooledCabinets(40).create()).results().get(0);
        assertEquals(Grade.AVAILABLE, result.grade());
        assertNull(result.timing());
        assertNull(result.density());
        assertNull(result.cabinets());
        assertTrue(result.interconnection().isEmpty());
        assertEquals(Grade.AVAILABLE, result.supportedFeature(Feature.LIQUID_COOLING).support());
        assertEquals(Grade.NOT_DISCLOSED, result.supportedFeature(Feature.PRIVATE_SUITE).support());
    }

    @Test
    void fullProfileAddsPowerBandLatencyAndReasons() {
        provider.disclosure(ReferenceProvider.Disclosure.FULL);
        CapacityRequirement requirement = CapacityRequirement.define()
                .addLocation(Location.metro("FR"))
                .addLocation(Location.metro("LD"))
                .power(Power.kva(100))
                .addInterconnection(Party.asn(64500), InterconnectionScope.METRO, true)
                .addInterconnection(Party.asn(64501), InterconnectionScope.METRO, false)
                .create();
        CapacityAnswer answer = capacity.queries().ask(requirement);
        LocationResult frankfurt = answer.results().get(0);
        assertEquals(Grade.AVAILABLE, frankfurt.grade());
        assertEquals(95.0, frankfurt.power().min(), 0.001);
        assertEquals(Grade.AVAILABLE, frankfurt.presenceOf(Party.asn(64500)));
        assertEquals(1.5, frankfurt.interconnection().get(0).roundTripMs().max());
        assertEquals(Grade.NOT_DISCLOSED, frankfurt.presenceOf(Party.asn(64501)), "present, and not listed publicly");
        assertEquals(Grade.NOT_DISCLOSED, frankfurt.presenceOf(Party.named("Nobody")));
        LocationResult london = answer.results().get(1);
        assertEquals(Grade.UNAVAILABLE, london.grade());
        assertEquals(List.of(Reason.INTERCONNECTION), london.reasons());
        assertEquals("https://provider.example/contact", london.nextStepUrl());
    }

    @Test
    void facilityQuestionIsAnsweredAtMetroLevel() {
        CapacityRequirement requirement = CapacityRequirement.define()
                .addLocation(Location.facility("FR-B")).addLocation(Location.campus("FR-CAMPUS-1"))
                .cabinets(10).density(DensityRange.kw().max(10).create()).create();
        CapacityAnswer answer = capacity.queries().ask(requirement);
        assertEquals(Location.metro("FR"), answer.results().get(0).location());
        assertEquals(Location.facility("FR-B"), answer.results().get(0).requested());
        assertEquals(Location.campus("FR-CAMPUS-1"), answer.results().get(1).requested());
    }

    @Test
    void regionCountryStandardCodeAndAreaAreResolved() {
        CapacityRequirement requirement = CapacityRequirement.define()
                .addLocation(Location.region("EMEA"))
                .addLocation(Location.country("NL"))
                .addLocation(Location.metroByUnLocode("GBLON"))
                .addLocation(Location.within(50.11, 8.68, 400))
                .addLocation(Location.metroByUnLocode("USNYC"))
                .power(Power.kw(100)).create();
        provider.disclosure(ReferenceProvider.Disclosure.FULL);
        List<LocationResult> results = capacity.queries().ask(requirement).results();
        assertEquals(6, results.size(), "the area holds two metros");
        assertEquals(Location.region("EMEA"), results.get(0).location());
        assertEquals(Grade.AVAILABLE, results.get(1).grade());
        assertEquals(Grade.AVAILABLE, results.get(2).grade());
        assertEquals(List.of(Location.metro("FR"), Location.metro("AM")),
                List.of(results.get(3).location(), results.get(4).location()));
        assertTrue(results.get(3).requested().isArea());
        assertEquals(Grade.UNAVAILABLE, results.get(5).grade());
        assertEquals(List.of(Reason.LOCATION_NOT_SERVED), results.get(5).reasons());
    }

    @Test
    void constraintsThatNoFacilityMeetsAreUnavailableWithTheReason() {
        provider.disclosure(ReferenceProvider.Disclosure.FULL);
        CapacityRequirement.Builder base = CapacityRequirement.define().addLocation(Location.metro("LD")).cabinets(2);
        assertEquals(List.of(Reason.DENSITY), capacity.queries()
                .ask(base.density(DensityRange.kw().target(60).create()).create()).results().get(0).reasons());
        base.density(DensityRange.kw().target(5).create());
        assertEquals(List.of(Reason.REDUNDANCY), capacity.queries()
                .ask(base.redundancy(RedundancyLevel.TWO_N, null).create()).results().get(0).reasons());
        assertEquals(List.of(Reason.POWER_FEED), capacity.queries()
                .ask(base.redundancy(RedundancyLevel.N, null).powerFeed(new PowerFeed(PowerCurrent.DC, null, 400)).create())
                .results().get(0).reasons());
        CapacityAnswer feature = capacity.queries().ask(CapacityRequirement.define().addLocation(Location.metro("LD"))
                .power(Power.kw(10)).addFeature(Feature.LIQUID_COOLING, true).create());
        assertEquals(List.of(Reason.FEATURE), feature.results().get(0).reasons());
        assertEquals(Grade.UNAVAILABLE, feature.results().get(0).supportedFeature(Feature.LIQUID_COOLING).support());
        CapacityAnswer unknown = capacity.queries().ask(CapacityRequirement.define().addLocation(Location.metro("LD"))
                .power(Power.kw(10)).addFeature(Feature.of("X_EXAMPLE_FEATURE"), true).create());
        assertEquals(Grade.NOT_DISCLOSED, unknown.results().get(0).grade());
    }

    @Test
    void largeDemandIsPossibleWithReviewAndPartialFitReportsABand() {
        provider.disclosure(ReferenceProvider.Disclosure.FULL);
        CapacityAnswer large = capacity.queries().ask(CapacityRequirement.define()
                .addLocation(Location.region("EMEA")).power(Power.mw(12)).create());
        assertEquals(Grade.POSSIBLE, large.grade());
        assertEquals(List.of(Reason.REVIEW_REQUIRED), large.results().get(0).reasons());

        CapacityAnswer partial = capacity.queries().ask(CapacityRequirement.define()
                .addLocation(Location.metro("LD")).cabinets(120, 10).density(DensityRange.kw().target(10).create()).create());
        LocationResult result = partial.results().get(0);
        assertEquals(Grade.POSSIBLE, result.grade());
        assertEquals(10, result.cabinets().min());
        assertEquals(15, result.cabinets().max());
        assertEquals(List.of(Reason.SPACE), result.reasons());

        CapacityAnswer none = capacity.queries().ask(CapacityRequirement.define()
                .addLocation(Location.metro("LD")).power(Power.mva(4)).create());
        assertEquals(List.of(Reason.POWER), none.results().get(0).reasons());
    }

    @Test
    void providerErrorsCarryTheProblemDocument() {
        provider.areaLocations(false);
        CapacityRequirement area = CapacityRequirement.define()
                .addLocation(Location.within(50.11, 8.68, 50)).power(Power.mw(1)).create();
        OpenCapacityException unsupported = assertThrows(OpenCapacityException.class, () -> capacity.queries().ask(area));
        assertEquals(422, unsupported.status());
        assertEquals("UNSUPPORTED_LOCATION_LEVEL", unsupported.code().orElseThrow());
        assertEquals("/requirement/locations/0", unsupported.problem().orElseThrow().errors().get(0).pointer());

        CapacityRequirement.Builder many = CapacityRequirement.define().power(Power.kw(1));
        for (int i = 0; i < 11; i++) {
            many.addLocation(Location.metro("FR"));
        }
        assertEquals("TOO_MANY_LOCATIONS", assertThrows(OpenCapacityException.class,
                () -> capacity.queries().ask(many.create())).code().orElseThrow());

        provider.queryQuota(1);
        CapacityRequirement small = many.clearLocations().addLocation(Location.metro("AM")).create();
        capacity.queries().ask(small);
        OpenCapacityException quota = assertThrows(OpenCapacityException.class, () -> capacity.queries().ask(small));
        assertEquals(429, quota.status());
        assertTrue(quota.getMessage().contains("Retry after 3600 seconds"), quota.getMessage());
    }

    // ------------------------------------------------------------------ signals

    @Test
    void signalIsRegisteredAmendedWithdrawnReactivatedAndDeleted() {
        Signal signal = capacity.signals().define(liquidCooledCabinets(40).neededForService(FlexibleDate.quarter(2027, 2)).create())
                .reference("example-signal-1").confidence(Confidence.PLANNED)
                .contact("Network Operations", "noc@example.com").create();
        assertEquals(SignalState.RECEIVED, signal.state());
        assertEquals(1, signal.revision());
        assertEquals(NOW, signal.createdAt());
        assertEquals(NOW.plus(Duration.ofDays(365)), signal.expiresAt());
        assertTrue(signal.confidentiality().contactPermitted());
        assertEquals("noc@example.com", signal.contact().email());
        assertTrue(signal.answer().isEmpty());

        provider.acknowledge(signal.id());
        assertEquals(SignalState.ACKNOWLEDGED, signal.refresh().state());
        assertEquals(Grade.AVAILABLE, signal.answer().orElseThrow().grade());
        assertTrue(signal.isActive());

        Signal other = capacity.signals().get(signal.id());
        signal.update().requirement(signal.requirement().toBuilder().cabinets(60).create())
                .confidence(Confidence.FUNDED).sharing(Sharing.AGGREGATED).noContact().save();
        assertEquals(2, signal.revision());
        assertEquals(SignalState.RECEIVED, signal.state());
        assertEquals(60, signal.requirement().demand().cabinets().count());
        assertEquals(Sharing.AGGREGATED, signal.confidentiality().sharing());
        assertNull(signal.contact());
        assertEquals(NOW.plus(Duration.ofDays(365)), signal.expiresAt(), "an amend keeps the expiry");

        OpenCapacityException stale = assertThrows(OpenCapacityException.class,
                () -> other.update().reference("late").save());
        assertEquals(412, stale.status());
        assertEquals("PRECONDITION_FAILED", stale.code().orElseThrow());

        assertEquals(SignalState.WITHDRAWN, signal.withdraw().state());
        assertFalse(signal.isActive());
        assertEquals(List.of(signal.id()), capacity.signals().list(SignalState.WITHDRAWN).stream().map(Signal::id).toList());
        assertTrue(capacity.signals().list(SignalState.RECEIVED).isEmpty());

        signal.update().reference("back").expiresAt(NOW.plus(Duration.ofDays(30))).save();
        assertEquals(SignalState.RECEIVED, signal.state());
        assertEquals(3, signal.revision());
        assertEquals("back", signal.reference());

        signal.delete();
        assertTrue(signal.isDeleted());
        assertEquals(404, assertThrows(OpenCapacityException.class, signal::refresh).status());
        assertTrue(capacity.signals().list().isEmpty());
    }

    @Test
    void closedSignalCannotBeAmendedOrWithdrawn() {
        Signal signal = capacity.signals().define(liquidCooledCabinets(10).create()).create();
        provider.close(signal.id(), "FULFILLED");
        Signal listed = capacity.signals().list().get(0);
        assertEquals(SignalState.CLOSED, listed.state());
        assertEquals(CloseReason.FULFILLED, listed.closeReason().orElseThrow());
        OpenCapacityException amend = assertThrows(OpenCapacityException.class, () -> listed.update().reference("x").save());
        assertEquals(409, amend.status());
        assertEquals("STATE_CONFLICT", amend.code().orElseThrow());
        assertEquals(409, assertThrows(OpenCapacityException.class, listed::withdraw).status());
    }

    // ------------------------------------------------------------------ subscriptions

    @Test
    void pullSubscriptionReportsGradeChangesAndCanBeStreamed() {
        CapacityRequirement requirement = CapacityRequirement.define().addLocation(Location.metro("LD"))
                .cabinets(20).density(DensityRange.kw().target(10).create()).create();
        Subscription subscription = capacity.subscriptions().define(requirement).name("London").pull().create();
        assertEquals(SubscriptionState.ACTIVE, subscription.state());
        assertEquals(NotifyOn.GRADE_CHANGE, subscription.notifyOn());
        assertTrue(subscription.signingSecret().isEmpty());
        assertEquals(requirement, subscription.requirement().orElseThrow());

        List<Event> first = subscription.events(0, 10);
        assertEquals(1, first.size());
        assertTrue(first.get(0).isAnswerUpdate());
        assertEquals(EventReason.INITIAL, first.get(0).data().reason());
        assertEquals(Grade.POSSIBLE, first.get(0).data().answer().grade());

        provider.inventory().setAvailableKw("LD-A", 500);
        provider.reevaluate();
        provider.reevaluate();
        List<Event> next = subscription.events(1, 10);
        assertEquals(1, next.size(), "an unchanged answer sends no event");
        assertEquals(EventReason.GRADE_CHANGE, next.get(0).data().reason());
        assertEquals(Grade.AVAILABLE, next.get(0).data().answer().grade());
        assertEquals(2, subscription.refresh().lastSequence());
        assertEquals(1, subscription.events(0, 1).size());

        List<Event> streamed = new ArrayList<>();
        long last = subscription.stream(0, event -> {
            streamed.add(event);
            return true;
        });
        assertEquals(2, last);
        assertEquals(List.of(1L, 2L), streamed.stream().map(Event::sequence).toList());
        assertEquals(2, subscription.stream(1, event -> false), "the handler stops the stream");

        assertEquals(SubscriptionState.PAUSED, subscription.pause().state());
        assertTrue(subscription.isPaused());
        provider.inventory().setAvailableKw("LD-A", 0);
        provider.reevaluate();
        assertEquals(2, subscription.refresh().lastSequence(), "a paused subscription gets no events");
        assertEquals(SubscriptionState.ACTIVE, subscription.resume().state());

        assertEquals(1, capacity.subscriptions().list().size());
        subscription.delete();
        assertTrue(subscription.isDeleted());
        assertEquals(404, assertThrows(OpenCapacityException.class,
                () -> capacity.subscriptions().get(subscription.id())).status());
    }

    @Test
    void anyChangeSubscriptionReportsADetailChange() {
        CapacityRequirement requirement = CapacityRequirement.define().addLocation(Location.metro("AM"))
                .power(Power.kw(700)).neededForService(FlexibleDate.year(2028)).create();
        Subscription subscription = capacity.subscriptions().define(requirement)
                .notifyOn(NotifyOn.ANY_CHANGE).expiresAt(NOW.plus(Duration.ofDays(90))).pull().create();
        assertEquals(NOW.plus(Duration.ofDays(90)), subscription.expiresAt());
        provider.inventory().setAvailableKw("AM-A", 900);
        provider.reevaluate();
        Event change = subscription.events(1, 10).get(0);
        assertEquals(EventReason.DETAIL_CHANGE, change.data().reason());
        assertTrue(change.data().answer().results().get(0).timing().availableForService().isNow());
    }

    @Test
    void subscriptionThatFollowsASignalHearsAboutTheSignal() {
        Signal signal = capacity.signals().define(liquidCooledCabinets(10).create()).create();
        Subscription subscription = capacity.subscriptions().define(signal).pull().create();
        assertEquals(signal.id(), subscription.signalId().orElseThrow());
        assertTrue(subscription.requirement().isEmpty());
        assertThrows(IllegalStateException.class, () -> subscription.update().requirement(signal.requirement()));

        signal.withdraw();
        List<Event> events = subscription.events(1, 10);
        assertEquals(Event.SIGNAL_UPDATED, events.get(0).type());
        assertEquals(SignalState.WITHDRAWN, events.get(0).data().signalState());

        signal.delete();
        assertEquals(SubscriptionState.EXPIRED, subscription.refresh().state());
        Event expired = subscription.events(2, 10).get(0);
        assertEquals(Event.SUBSCRIPTION_UPDATED, expired.type());
        assertEquals(SubscriptionState.EXPIRED, expired.data().subscriptionState());
    }

    @Test
    void webhookSubscriptionIsVerifiedSignedAndReverifiedOnANewUrl() throws IOException {
        List<String> verified = new CopyOnWriteArrayList<>();
        String[] secret = new String[1];
        HttpServer endpoint = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        endpoint.createContext("/", exchange -> {
            String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Event event = Events.parse(raw);
            String header = exchange.getRequestHeaders().getFirst(WebhookSignature.HEADER);
            byte[] reply = new byte[0];
            if (Event.SUBSCRIPTION_VERIFICATION.equals(event.type())) {
                reply = Events.challengeResponse(event).getBytes(StandardCharsets.UTF_8);
            } else if (secret[0] != null && WebhookSignature.verify(secret[0], header, raw, NOW, Duration.ofMinutes(5))) {
                verified.add(event.type() + " " + exchange.getRequestURI().getPath());
            }
            exchange.sendResponseHeaders(200, reply.length == 0 ? -1 : reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        endpoint.start();
        String url = "http://127.0.0.1:" + endpoint.getAddress().getPort();
        try {
            // The provider posts to the endpoint while it creates the subscription, so the secret
            // is not known to the endpoint until create() returns. The first answer is checked below.
            Subscription subscription = capacity.subscriptions()
                    .define(CapacityRequirement.define().addLocation(Location.metro("LD")).power(Power.kw(100)).create())
                    .webhook(url + "/hooks/a").create();
            secret[0] = subscription.signingSecret().orElseThrow();
            assertTrue(secret[0].startsWith("whsec_"));
            assertEquals(SubscriptionState.ACTIVE, subscription.refresh().state());
            assertNull(subscription.delivery().signingSecret(), "the secret is returned once");
            assertEquals(List.of(Event.SUBSCRIPTION_VERIFICATION, Event.ANSWER_UPDATED),
                    subscription.events(0, 10).stream().map(Event::type).toList());

            provider.inventory().setAvailableKw("LD-A", 10);
            provider.reevaluate();
            assertEquals(List.of(Event.ANSWER_UPDATED + " /hooks/a"), verified);

            subscription.update().name("renamed").webhook(url + "/hooks/b").save();
            assertEquals(SubscriptionState.ACTIVE, subscription.refresh().state());
            assertEquals("renamed", subscription.name());
            provider.inventory().setAvailableKw("LD-A", 500);
            provider.reevaluate();
            assertEquals(Event.ANSWER_UPDATED + " /hooks/b", verified.get(verified.size() - 1));

            assertEquals(DeliveryType.PULL, subscription.update().pull().save().delivery().type());
        } finally {
            endpoint.stop(0);
        }
    }

    @Test
    void webhookThatDoesNotAnswerStaysPendingAndBuilderNeedsADeliveryType() {
        Subscription pending = capacity.subscriptions()
                .define(CapacityRequirement.define().addLocation(Location.metro("LD")).power(Power.kw(100)).create())
                .webhook("http://127.0.0.1:1/unreachable").create();
        assertEquals(SubscriptionState.PENDING_VERIFICATION, pending.state());
        assertNotNull(pending.createdAt());
        assertNotNull(pending.updatedAt());
        assertThrows(IllegalStateException.class, () -> capacity.subscriptions()
                .define(CapacityRequirement.define().addLocation(Location.metro("LD")).power(Power.kw(1)).create()).create());
    }
}
