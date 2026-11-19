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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eqixiac.opencapacity.model.Capabilities;
import com.eqixiac.opencapacity.model.CapacityAnswer;
import com.eqixiac.opencapacity.model.CapacityRequirement;
import com.eqixiac.opencapacity.model.DensityRange;
import com.eqixiac.opencapacity.model.Event;
import com.eqixiac.opencapacity.model.Feature;
import com.eqixiac.opencapacity.model.FlexibleDate;
import com.eqixiac.opencapacity.model.InterconnectionScope;
import com.eqixiac.opencapacity.model.Location;
import com.eqixiac.opencapacity.model.LocationInfo;
import com.eqixiac.opencapacity.model.Party;
import com.eqixiac.opencapacity.model.PartyType;
import com.eqixiac.opencapacity.model.Problem;
import com.eqixiac.opencapacity.model.RedundancyLevel;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Reads every example of the bundled OpenCapacity specification with the client's types and checks
 * that writing it back gives the same JSON. The examples come from the specification, never from
 * the client's own output.
 */
class SpecExamplesTest {

    private static JsonNode spec;

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = SpecExamplesTest.class.getResourceAsStream("/opencapacity/opencapacity-1.0.json")) {
            spec = Transport.MAPPER.readTree(in);
        }
    }

    static Stream<String> exampleNames() throws IOException {
        if (spec == null) {
            load();
        }
        List<String> names = new ArrayList<>();
        spec.path("components").path("examples").fieldNames().forEachRemaining(names::add);
        return names.stream();
    }

    private static JavaType typeOf(String name) {
        if (name.equals("LocationsPage")) {
            return Transport.type(Wire.Page.class, LocationInfo.class);
        }
        if (name.equals("SignalsPage")) {
            return Transport.type(Wire.Page.class, Wire.SignalBody.class);
        }
        if (name.equals("EventsPage")) {
            return Transport.type(Wire.Page.class, Event.class);
        }
        if (name.startsWith("Capabilities")) {
            return Transport.type(Capabilities.class);
        }
        if (name.startsWith("Query")) {
            return Transport.type(Wire.Query.class);
        }
        if (name.startsWith("Answer")) {
            return Transport.type(CapacityAnswer.class);
        }
        if (name.startsWith("Problem")) {
            return Transport.type(Problem.class);
        }
        if (name.startsWith("Event")) {
            return Transport.type(Event.class);
        }
        if (name.startsWith("Subscription")) {
            return Transport.type(Wire.SubscriptionBody.class);
        }
        if (name.startsWith("Signal")) {
            return Transport.type(Wire.SignalBody.class);
        }
        throw new IllegalStateException("No client type is mapped for the example " + name);
    }

    /** Numbers compare by value, and an empty array equals an absent one. */
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

    @ParameterizedTest
    @MethodSource("exampleNames")
    void exampleSurvivesARoundTrip(String name) throws IOException {
        JsonNode value = spec.path("components").path("examples").path(name).path("value");
        Object read = Transport.MAPPER.readValue(Transport.MAPPER.treeAsTokens(value), typeOf(name));
        JsonNode written = Transport.MAPPER.valueToTree(read);
        assertEquals(normalize(value), normalize(written), name);
    }

    @Test
    void builderProducesTheFirstQueryExample() throws IOException {
        CapacityRequirement built = CapacityRequirement.define()
                .addLocation(Location.metro("FR"))
                .addLocation(Location.country("NL"))
                .cabinets(40)
                .density(DensityRange.kva().min(5).target(8).max(100).create())
                .contiguous(true)
                .neededForSale(FlexibleDate.quarter(2026, 4))
                .neededForService(FlexibleDate.month(2027, 6))
                .latestAcceptable(FlexibleDate.quarter(2027, 3))
                .termMonths(60)
                .addInterconnection(new Party("Example Cloud", 64500L, PartyType.CLOUD), InterconnectionScope.METRO, true)
                .addFeature(Feature.LIQUID_COOLING, true)
                .addFeature(Feature.PRIVATE_CAGE, false)
                .redundancy(RedundancyLevel.TWO_N, null)
                .create();
        JsonNode example = spec.path("components").path("examples").path("QueryCabinetsAndDensity").path("value");
        assertEquals(normalize(example.path("requirement")), normalize(Transport.MAPPER.valueToTree(built)));
        assertEquals(built, built.toBuilder().create());
    }

    @Test
    void workedSignatureOfTheSpecificationVerifies() {
        JsonNode step = null;
        for (JsonNode exchange : spec.path("x-exchanges")) {
            for (JsonNode candidate : exchange.path("steps")) {
                if (candidate.path("request").has("signature")) {
                    step = candidate.path("request");
                }
            }
        }
        JsonNode signature = step.path("signature");
        String secret = signature.path("signingSecret").asText();
        String header = step.path("headers").path(WebhookSignature.HEADER).asText();
        String raw = step.path("rawBody").asText();
        Instant signedAt = Instant.ofEpochSecond(signature.path("timestamp").asLong());

        assertEquals(header, WebhookSignature.sign(secret, signature.path("timestamp").asLong(), raw));
        assertTrue(WebhookSignature.verify(secret, header, raw, signedAt.plusSeconds(60), Duration.ofMinutes(5)));
        assertFalse(WebhookSignature.verify(secret, header, raw + " ", signedAt, Duration.ofMinutes(5)), "changed body");
        assertFalse(WebhookSignature.verify("other", header, raw, signedAt, Duration.ofMinutes(5)), "wrong secret");
        assertFalse(WebhookSignature.verify(secret, header, raw, signedAt.plusSeconds(3600), Duration.ofMinutes(5)), "too old");
        assertFalse(WebhookSignature.verify(secret, null, raw, signedAt, Duration.ofMinutes(5)), "no header");
        assertFalse(WebhookSignature.verify(secret, "t=abc,v1=00", raw, signedAt, Duration.ofMinutes(5)), "bad timestamp");
        assertFalse(WebhookSignature.verify(secret, "v1=00", raw, signedAt, Duration.ofMinutes(5)), "no timestamp");
        assertEquals(Event.ANSWER_UPDATED, Events.parse(raw).type());
    }
}
