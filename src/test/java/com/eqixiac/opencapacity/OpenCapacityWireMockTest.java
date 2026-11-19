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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eqixiac.equinix.core.WireMockTestBase;
import com.eqixiac.opencapacity.model.CapacityAnswer;
import com.eqixiac.opencapacity.model.CapacityRequirement;
import com.eqixiac.opencapacity.model.Confidence;
import com.eqixiac.opencapacity.model.DensityRange;
import com.eqixiac.opencapacity.model.Event;
import com.eqixiac.opencapacity.model.Feature;
import com.eqixiac.opencapacity.model.FlexibleDate;
import com.eqixiac.opencapacity.model.Grade;
import com.eqixiac.opencapacity.model.InterconnectionScope;
import com.eqixiac.opencapacity.model.Location;
import com.eqixiac.opencapacity.model.NotifyOn;
import com.eqixiac.opencapacity.model.Party;
import com.eqixiac.opencapacity.model.PartyType;
import com.eqixiac.opencapacity.model.Power;
import com.eqixiac.opencapacity.model.RedundancyLevel;
import com.eqixiac.opencapacity.model.Sharing;
import com.eqixiac.opencapacity.model.SignalState;
import com.eqixiac.opencapacity.model.SubscriptionState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Request-contract tests for the OpenCapacity client: what the client puts on the wire, checked
 * against the examples of the bundled specification. Every stubbed response is an example of the
 * specification.
 */
class OpenCapacityWireMockTest extends WireMockTestBase {

    private static final String BASE = "/capacity/v1";
    private static JsonNode examples;

    private OpenCapacity capacity;

    @BeforeAll
    static void loadSpecification() throws IOException {
        try (InputStream in = OpenCapacityWireMockTest.class.getResourceAsStream("/opencapacity/opencapacity-1.0.json")) {
            examples = Transport.MAPPER.readTree(in).path("components").path("examples");
        }
    }

    @BeforeEach
    void client() {
        wireMock.resetAll();
        wireMock.stubFor(post(urlPathEqualTo("/provider/token"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"provider-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));
        capacity = OpenCapacity.builder()
                .endpoint(wireMockUrl() + BASE)
                .tokenUrl(wireMockUrl() + "/provider/token")
                .credentials("CLIENT_ID", "CLIENT_SECRET")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    private static String example(String name) {
        JsonNode value = examples.path(name).path("value");
        assertTrue(value.isObject(), "the specification has no example " + name);
        return value.toString();
    }

    private static void stubJson(com.github.tomakehurst.wiremock.client.MappingBuilder mapping, int status,
            String exampleName, String etag) {
        com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder response = aResponse().withStatus(status)
                .withHeader("Content-Type", "application/json").withBody(example(exampleName));
        if (etag != null) {
            response.withHeader("ETag", etag);
        }
        wireMock.stubFor(mapping.willReturn(response));
    }

    private static Object normalize(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> map = new TreeMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> entry = it.next();
                if (!(entry.getValue().isArray() && entry.getValue().isEmpty())) {
                    map.put(entry.getKey(), normalize(entry.getValue()));
                }
            }
            return map;
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(item -> list.add(normalize(item)));
            return list;
        }
        return node.isNumber() ? (Object) node.asDouble() : node.asText();
    }

    /** The body of the one request the client sent with this method and path. */
    private static ObjectNode sentBody(String method, String path) throws IOException {
        List<LoggedRequest> requests = wireMock.findAll(anyRequestedFor(urlEqualTo(path)));
        requests.removeIf(request -> !request.getMethod().getName().equals(method));
        assertEquals(1, requests.size(), method + " " + path);
        return (ObjectNode) Transport.MAPPER.readTree(requests.get(0).getBodyAsString());
    }

    private static void assertSent(String exampleName, JsonNode sent) {
        assertEquals(normalize(examples.path(exampleName).path("value")), normalize(sent), exampleName);
    }

    private static CapacityRequirement firstQueryRequirement() {
        return CapacityRequirement.define()
                .addLocation(Location.metro("FR")).addLocation(Location.country("NL"))
                .cabinets(40).density(DensityRange.kva().min(5).target(8).max(100).create()).contiguous(true)
                .neededForSale(FlexibleDate.quarter(2026, 4)).neededForService(FlexibleDate.month(2027, 6))
                .latestAcceptable(FlexibleDate.quarter(2027, 3)).termMonths(60)
                .addInterconnection(new Party("Example Cloud", 64500L, PartyType.CLOUD), InterconnectionScope.METRO, true)
                .addFeature(Feature.LIQUID_COOLING, true).addFeature(Feature.PRIVATE_CAGE, false)
                .redundancy(RedundancyLevel.TWO_N, null).create();
    }

    private static CapacityRequirement signalRequirement() {
        return CapacityRequirement.define().addLocation(Location.metro("FR"))
                .power(Power.kva(800)).cabinets(40).density(DensityRange.kva().target(20).create())
                .neededForService(FlexibleDate.day(2027, 6, 1)).addFeature(Feature.LIQUID_COOLING, true).create();
    }

    @Test
    void queryRequestMatchesTheSpecificationExample() throws IOException {
        stubJson(post(urlEqualTo(BASE + "/queries")), 200, "AnswerBanded", null);

        CapacityAnswer answer = capacity.queries().ask(firstQueryRequirement(), "example-query-1");

        assertEquals(Grade.AVAILABLE, answer.grade());
        assertEquals(2, answer.results().size());
        wireMock.verify(postRequestedFor(urlEqualTo(BASE + "/queries"))
                .withHeader("Authorization", equalTo("Bearer provider-token"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json")));
        assertSent("QueryCabinetsAndDensity", sentBody("POST", BASE + "/queries"));
        wireMock.verify(1, postRequestedFor(urlEqualTo("/provider/token"))
                .withHeader("Authorization", equalTo("Basic Q0xJRU5UX0lEOkNMSUVOVF9TRUNSRVQ="))
                .withRequestBody(equalTo("grant_type=client_credentials")));
    }

    @Test
    void capabilitiesRequestCarriesNoToken() {
        stubJson(get(urlEqualTo(BASE + "/capabilities")), 200, "Capabilities", null);

        assertEquals("1.0.0", capacity.capabilities().specVersion());

        wireMock.verify(getRequestedFor(urlEqualTo(BASE + "/capabilities")).withHeader("Authorization", absent()));
        wireMock.verify(0, postRequestedFor(urlEqualTo("/provider/token")));
    }

    @Test
    void tokenEndpointIsReadFromCapabilitiesWhenNotConfigured() {
        ObjectNode capabilities = (ObjectNode) examples.path("Capabilities").path("value").deepCopy();
        ((ObjectNode) capabilities.path("authentication")).put("tokenUrl", wireMockUrl() + "/provider/token");
        wireMock.stubFor(get(urlEqualTo(BASE + "/capabilities")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody(capabilities.toString())));
        stubJson(get(urlPathEqualTo(BASE + "/locations")), 200, "LocationsPage", null);
        wireMock.stubFor(get(urlPathEqualTo(BASE + "/locations")).withQueryParam("cursor", equalTo("bG9jLTAwMDY"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[],\"pagination\":{\"limit\":200}}")));
        OpenCapacity discovered = OpenCapacity.builder().endpoint(wireMockUrl() + BASE)
                .credentials("CLIENT_ID", "CLIENT_SECRET").build();

        assertEquals(5, discovered.locations().list().size(), "the client follows the cursor to the last page");

        wireMock.verify(1, postRequestedFor(urlEqualTo("/provider/token")));
        wireMock.verify(getRequestedFor(urlPathEqualTo(BASE + "/locations"))
                .withQueryParam("cursor", equalTo("bG9jLTAwMDY")).withQueryParam("limit", equalTo("200")));
    }

    @Test
    void signalIsCreatedAmendedWithdrawnAndDeletedWithTheSpecifiedRequests() throws IOException {
        stubJson(post(urlEqualTo(BASE + "/signals")), 201, "SignalCreated", "\"sig-0001-1\"");
        stubJson(put(urlEqualTo(BASE + "/signals/sig-0001")), 200, "SignalAmended", "\"sig-0001-2\"");
        stubJson(post(urlEqualTo(BASE + "/signals/sig-0001/withdraw")), 200, "SignalWithdrawn", "\"sig-0001-3\"");
        wireMock.stubFor(delete(urlEqualTo(BASE + "/signals/sig-0001")).willReturn(aResponse().withStatus(204)));

        Signal signal = capacity.signals().define(signalRequirement())
                .reference("example-signal-1").confidence(Confidence.PLANNED).sharing(Sharing.NONE)
                .contact("Network Operations", "noc@example.com").create();
        assertEquals("sig-0001", signal.id());
        assertEquals(SignalState.RECEIVED, signal.state());
        wireMock.verify(postRequestedFor(urlEqualTo(BASE + "/signals"))
                .withHeader("Idempotency-Key", matching("[0-9a-f-]{36}")));
        assertSent("SignalRequest", sentBody("POST", BASE + "/signals"));

        signal.update()
                .requirement(signal.requirement().toBuilder().power(Power.kva(1200)).cabinets(60)
                        .neededForService(FlexibleDate.quarter(2027, 3)).create())
                .confidence(Confidence.FUNDED).save();
        assertEquals(2, signal.revision());
        wireMock.verify(putRequestedFor(urlEqualTo(BASE + "/signals/sig-0001"))
                .withHeader("If-Match", equalTo("\"sig-0001-1\"")));
        ObjectNode amend = sentBody("PUT", BASE + "/signals/sig-0001");
        assertEquals("2027-11-18T10:00:00Z", amend.remove("expiresAt").asText(), "an amend keeps the expiry");
        assertSent("SignalAmendRequest", amend);

        assertEquals(SignalState.WITHDRAWN, signal.withdraw().state());
        wireMock.verify(postRequestedFor(urlEqualTo(BASE + "/signals/sig-0001/withdraw")));
        assertEquals("", wireMock.findAll(postRequestedFor(urlEqualTo(BASE + "/signals/sig-0001/withdraw")))
                .get(0).getBodyAsString(), "withdraw has no body");

        signal.delete();
        wireMock.verify(deleteRequestedFor(urlEqualTo(BASE + "/signals/sig-0001"))
                .withHeader("Authorization", equalTo("Bearer provider-token")));
    }

    @Test
    void subscriptionIsCreatedReadAndStreamedWithTheSpecifiedRequests() throws IOException {
        stubJson(post(urlEqualTo(BASE + "/subscriptions")), 201, "SubscriptionPullActive", "\"sub-0003-1\"");
        stubJson(get(urlPathEqualTo(BASE + "/subscriptions/sub-0003/events")), 200, "EventsPage", null);
        String first = examples.path("EventsPage").path("value").path("data").get(0).toString();
        wireMock.stubFor(get(urlPathEqualTo(BASE + "/subscriptions/sub-0003/events"))
                .withHeader("Accept", equalTo("text/event-stream"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/event-stream")
                        .withBody("id: 1\nevent: org.opencapacity.answer.updated\ndata: " + first + "\n\n")));

        Subscription subscription = capacity.subscriptions()
                .define(CapacityRequirement.define().addLocation(Location.metro("AM")).cabinets(6)
                        .density(DensityRange.kva().max(10).create()).neededForService(FlexibleDate.month(2027, 2)).create())
                .name("Amsterdam small footprint").notifyOn(NotifyOn.GRADE_CHANGE).pull().create();
        assertEquals(SubscriptionState.ACTIVE, subscription.state());
        wireMock.verify(postRequestedFor(urlEqualTo(BASE + "/subscriptions"))
                .withHeader("Idempotency-Key", matching("[0-9a-f-]{36}")));
        assertSent("SubscriptionPullRequest", sentBody("POST", BASE + "/subscriptions"));

        List<Event> page = subscription.events(0, 2);
        assertEquals(2, page.size());
        wireMock.verify(getRequestedFor(urlPathEqualTo(BASE + "/subscriptions/sub-0003/events"))
                .withQueryParam("after", equalTo("0")).withQueryParam("limit", equalTo("2"))
                .withHeader("Accept", equalTo("application/json")));

        List<Event> streamed = new ArrayList<>();
        assertEquals(1, subscription.stream(0, streamed::add));
        assertEquals(1, streamed.size());
        subscription.stream(1, event -> true);
        wireMock.verify(getRequestedFor(urlPathEqualTo(BASE + "/subscriptions/sub-0003/events"))
                .withHeader("Accept", equalTo("text/event-stream")).withHeader("Last-Event-ID", equalTo("1")));
    }

    @Test
    void quotaErrorCarriesTheProblemAndTheRetryTime() {
        wireMock.stubFor(post(urlEqualTo(BASE + "/queries")).willReturn(aResponse().withStatus(429)
                .withHeader("Content-Type", "application/problem+json").withHeader("Retry-After", "3600")
                .withBody(example("ProblemQuotaExceeded"))));

        OpenCapacityException failure = assertThrows(OpenCapacityException.class,
                () -> capacity.queries().ask(firstQueryRequirement()));

        assertEquals(429, failure.status());
        assertEquals("QUOTA_EXCEEDED", failure.code().orElseThrow());
        assertTrue(failure.getMessage().contains("Retry after 3600 seconds"), failure.getMessage());
        wireMock.verify(1, postRequestedFor(urlEqualTo(BASE + "/queries")));
    }

    @Test
    void a401IsRetriedOnceWithANewToken() {
        wireMock.stubFor(post(urlEqualTo(BASE + "/queries")).willReturn(aResponse().withStatus(401)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("{\"type\":\"about:blank\",\"title\":\"unauthenticated\",\"status\":401,\"code\":\"UNAUTHENTICATED\"}")));

        OpenCapacityException failure = assertThrows(OpenCapacityException.class,
                () -> capacity.queries().ask(firstQueryRequirement()));

        assertEquals(401, failure.status());
        wireMock.verify(2, postRequestedFor(urlEqualTo(BASE + "/queries")));
        wireMock.verify(2, postRequestedFor(urlEqualTo("/provider/token")));
    }

    @Test
    void slowProviderFailsAtTheClientTimeout() {
        wireMock.stubFor(post(urlEqualTo(BASE + "/queries")).willReturn(aResponse().withStatus(200)
                .withFixedDelay(2000).withHeader("Content-Type", "application/json").withBody(example("AnswerBanded"))));
        OpenCapacity impatient = OpenCapacity.builder().endpoint(wireMockUrl() + BASE)
                .tokenSource(() -> "provider-token").timeout(Duration.ofMillis(300)).build();

        long started = System.nanoTime();
        OpenCapacityException failure = assertThrows(OpenCapacityException.class,
                () -> impatient.queries().ask(firstQueryRequirement()));

        assertEquals(0, failure.status(), "no response arrived");
        assertTrue(Duration.ofNanos(System.nanoTime() - started).toMillis() < 1900, "the client did not wait for the provider");
    }

    @Test
    void bodyThatIsNotTheSpecifiedShapeIsReported() {
        wireMock.stubFor(post(urlEqualTo(BASE + "/queries")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody("<html>gateway</html>")));
        wireMock.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(502).withBody("bad gateway")));

        assertThrows(OpenCapacityException.class, () -> capacity.queries().ask(firstQueryRequirement()));
        OpenCapacityException gateway = assertThrows(OpenCapacityException.class, () -> capacity.signals().get("sig-0001"));
        assertEquals(502, gateway.status());
        assertTrue(gateway.problem().isEmpty(), "a body that is not a problem document is not read as one");
    }
}
