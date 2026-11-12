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

package com.eqixiac.equinix.messaging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmgMessageParserTest {

    private final EmgMessageParser parser = new EmgMessageParser();

    @Nested
    class DocumentedSamples {

        @Test
        void ibxSample() {
            String json = Fixtures.read("docs-ibx-sample.json");
            IbxNotification n = assertInstanceOf(IbxNotification.class, parser.parse(json));
            EmgHeader h = n.header();
            assertEquals("370da978-b9bd-4030-b012-9dc97e2fdf98", h.id());
            assertEquals("370da978-b9bd-4030-b012-9dc97e2fdf98", n.id());
            assertEquals("3e095d30-40ff-11e9-8959-5be078353003", h.source());
            assertEquals(EmgVerb.UPDATE, h.verb());
            assertEquals("DataCenter.Maintenance", h.resource());
            assertEquals("application/json", h.contentType());
            assertEquals(Instant.parse("2021-06-09T07:42:45.396Z"), h.creationTime());
            assertEquals("1.0", h.version());
            // The sample's Signature contains a raw line break and trailing spaces: trimmed.
            assertEquals("RWNobwp7CiAgIklkIjogIjM1MGFlZjcwLTc4MWItMTFlOSogIH0KfQ==", h.signature());
            assertEquals(TicketCategory.MAINTENANCE, n.category());

            TicketBody b = n.body();
            assertEquals("97ae4427-70cb-4162-bf52-c3c7f9d7b2d8", b.requestorId());
            assertEquals("5-204454471282", b.servicerId());
            assertEquals(TicketState.CONFIRMED, b.state());
            assertEquals("Equinix Test Maintenance", b.description());
            assertEquals(2, b.sequence(), "string \"2\" in the sample is coerced to the spec's integer");
            assertEquals(Instant.parse("2021-03-29T17:33:00Z"), b.startDateTime(), "zone-less read as UTC");
            assertEquals(Instant.parse("2021-03-31T18:00:00Z"), b.endDateTime());
            assertEquals("123456", b.accountNumber());
            assertEquals(ServiceImpact.NON_SERVICE_IMPACTING, b.impact());
            assertFalse(b.isServiceImpacting());
            assertEquals("DataCenter Notification https://developer.equinix.com", b.prodId());
            assertTrue(b.organizer().startsWith("Equinix Customer Support"));
            assertEquals(List.of(), b.assets());
            assertEquals(json, n.rawJson());
        }

        @Test
        void networkSample() {
            NetworkNotification n = assertInstanceOf(NetworkNotification.class,
                    parser.parse(Fixtures.read("docs-network-sample.json")));
            assertEquals(TicketCategory.MAINTENANCE, n.category());
            assertEquals("Network.Maintenance", n.header().resource());
            assertEquals(ServiceImpact.NON_SERVICE_IMPACTING, n.body().impact());
            assertEquals("Network Notification https://developer.equinix.com", n.body().prodId());
        }

        @Test
        void fabricSampleIsFabricEvenThoughResourceIsDataCenterNetworkPort() {
            FabricNotification n = assertInstanceOf(FabricNotification.class,
                    parser.parse(Fixtures.read("docs-fabric-sample.json")));
            assertEquals("DataCenter.Network.Port", n.header().resource());
            assertNull(n.header().creationTime(), "the Fabric sample has no CreationTimeUTC");
            FabricNotification.Body b = n.body();
            assertEquals(Instant.parse("2020-05-21T10:20:00Z"), b.timestamp());
            assertEquals(FabricNotification.Type.PORT_OPSTATUS_NOTIFICATION, b.type());
            assertEquals("11c954fb-8e8b-4515-97d7-cc80129a0985", b.uuid());
            assertEquals(FabricNotification.AssetType.XF_PORT, b.data().type());
            assertEquals("a867f685-41b0-1b07-6de0-320a5c00abdd", b.data().uuid());
            assertEquals("L2-DEV-USER01-DC5-CX-10G-SECONDARY-NOKIA-02", b.data().name());
            assertEquals(FabricNotification.OperationalStatus.DOWN, b.data().operation().operationalStatus());
            assertEquals(Boolean.FALSE, b.data().operation().maintenanceMode());
            assertTrue(n.isDown());
            assertEquals(List.of(
                    "https://api.equinix.com/fabric/v4/connections/8822de74-805f-4f63-b52d-93bcbf2a1d1b",
                    "https://api.equinix.com/fabric/v4/connections/8f20bea9-a5b9-47e5-88e6-a899584ebb90"),
                    b.data().connectionHrefs());
        }

        @Test
        void billingSampleWithMisspeltResource() {
            BillingNotification n = assertInstanceOf(BillingNotification.class,
                    parser.parse(Fixtures.read("docs-billing-sample.json")));
            assertEquals("CrossConnnect", n.header().resource());
            assertEquals("1-205346710865", n.body().servicerId());
            assertEquals("Closed", n.body().state());
            BillingNotification.LineDetail line = n.body().lineDetails().get(0);
            assertEquals("1-3N5W5LYZ", line.id());
            assertEquals("DC1", line.ibx());
            assertEquals("Billed", line.status());
            assertEquals(List.of("1-2N5W4JAS", "1-2N5W4K87", "1-2N5W3M21", "1-2N5235N2"), line.billedLines());
        }

        @Test
        void orderSample() {
            OrderNotification n = assertInstanceOf(OrderNotification.class,
                    parser.parse(Fixtures.read("docs-order-sample.json")));
            assertEquals("SmartHands", n.header().resource());
            OrderNotification.Body b = n.body();
            assertEquals("CUSTOMER-REF-001", b.requestorId());
            assertEquals("1-3N5W5LYZ", b.activityId());
            assertEquals(OrderNotification.State.IN_PROGRESS, b.state());
            assertEquals("DC1", b.location());
            assertEquals("customer.contact@example.com", b.customerContact());
            assertEquals("Equipment installation and cable management", b.serviceDetails().get("scopeOfWork").asText());
            assertEquals(List.of(), b.attachments());
        }
    }

    @Nested
    class SpecExamples {

        @ParameterizedTest
        @ValueSource(strings = {"DataCenter.Maintenance", "DataCenter.Incident", "DataCenter.SecurityIncident",
                "DataCenter.Advisory"})
        void ibxV1AndV2(String resource) {
            IbxNotification v1 = assertInstanceOf(IbxNotification.class,
                    parser.parse(Fixtures.envelope(resource, "1.0", "spec-ibx-v1-body.json")));
            assertEquals(2, v1.body().sequence());
            assertEquals(Instant.parse("2021-03-29T17:33:00Z"), v1.body().startDateTime());
            IbxNotification v2 = assertInstanceOf(IbxNotification.class,
                    parser.parse(Fixtures.envelope(resource, "2.0", "spec-ibx-v2-body.json")));
            assertEquals("2.0", v2.header().version());
            assertEquals("Equinix Test Maintenance (v2)", v2.body().description());
            assertEquals(3, v2.body().sequence());
            assertEquals(Instant.parse("2025-09-09T12:00:00Z"), v2.body().endDateTime());
        }

        @Test
        void ibxCategories() {
            assertEquals(TicketCategory.INCIDENT, ibx("DataCenter.Incident").category());
            assertEquals(TicketCategory.SECURITY_INCIDENT, ibx("DataCenter.SecurityIncident").category());
            assertEquals(TicketCategory.ADVISORY, ibx("DataCenter.Advisory").category());
        }

        private IbxNotification ibx(String resource) {
            return (IbxNotification) parser.parse(Fixtures.envelope(resource, "1.0", "spec-ibx-v1-body.json"));
        }

        @Test
        void networkV1() {
            NetworkNotification n = assertInstanceOf(NetworkNotification.class,
                    parser.parse(Fixtures.envelope("Network.Incident", "1.0", "spec-network-v1-body.json")));
            assertEquals(TicketCategory.INCIDENT, n.category());
            assertEquals("5-204454471283", n.body().servicerId());
            assertEquals(1, n.body().sequence());
        }

        @ParameterizedTest
        @ValueSource(strings = {"DataCenter.Network.Port", "DataCenter.Network.VirtualConnection"})
        void networkV2TicketBodyOnPortResourcesIsNetwork(String resource) {
            NetworkNotification n = assertInstanceOf(NetworkNotification.class,
                    parser.parse(Fixtures.envelope(resource, "2.0", "spec-network-v2-body.json")));
            assertEquals("Equinix Network Maintenance (v2)", n.body().description());
            assertEquals(TicketCategory.OTHER, n.category());
        }

        @Test
        void fabric() {
            FabricNotification n = assertInstanceOf(FabricNotification.class,
                    parser.parse(Fixtures.envelope("DataCenter.Network.Port", "2.0", "spec-fabric-body.json")));
            assertEquals(Instant.parse("2020-05-21T10:20:00Z"), n.body().data().operation().opStatusChangedAt());
            assertEquals("https://api.equinix.com/fabric/v4/ports/a867f685-41b0-1b07-6de0-320a5c00abdd",
                    n.body().data().href());
            assertEquals(List.of(), n.body().data().connectionHrefs());
            assertTrue(n.isDown());
        }

        @ParameterizedTest
        @ValueSource(strings = {"CROSSCONNECT", "CrossConnect"})
        void billing(String resource) {
            BillingNotification n = assertInstanceOf(BillingNotification.class,
                    parser.parse(Fixtures.envelope(resource, "1.0", "spec-billing-body.json")));
            assertEquals("", n.body().requestorId());
            assertEquals("Order Number:1-205346710865 billing notification", n.body().description());
            assertNull(n.body().lineDetails().get(0).ibx());
            assertEquals(4, n.body().lineDetails().get(0).billedLines().size());
        }

        @ParameterizedTest
        @ValueSource(strings = {"Shipping", "SmartHands", "BreakFix", "WorkVisit", "COLO", "ACCESSORY",
                "IBXSMARTVIEW", "NETWORKPRODUCT", "POWER", "SECURITYACCESS"})
        void order(String resource) {
            OrderNotification n = assertInstanceOf(OrderNotification.class,
                    parser.parse(Fixtures.envelope(resource, "1.0", "spec-order-body.json")));
            assertEquals("ACTIVITY-78901", n.body().activityId());
            assertEquals(OrderNotification.State.IN_PROGRESS, n.body().state());
            assertEquals("SmartHands", n.body().serviceDetails().get("WorkType").asText());
        }

        @Test
        void crossConnectOrderWithActivityIdIsAnOrderNotBilling() {
            assertInstanceOf(OrderNotification.class,
                    parser.parse(Fixtures.envelope("CROSSCONNECT", "1.0", "spec-order-body.json")));
        }

        @Test
        void billingShapeOnOrderResourceIsBilling() {
            assertInstanceOf(BillingNotification.class,
                    parser.parse(Fixtures.envelope("COLO", "1.0", "spec-billing-body.json")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"TroubleTicket.ManagedService", "ServiceRequest.ManagedService"})
        void managedServices(String resource) {
            ManagedServiceNotification n = assertInstanceOf(ManagedServiceNotification.class,
                    parser.parse(Fixtures.envelope(resource, "1.0", "spec-managed-services-body.json")));
            ManagedServiceNotification.Body b = n.body();
            assertEquals("123456", b.accountNumber());
            assertEquals("TroubleTicket.ManagedService", b.resource());
            assertEquals(EmgVerb.UPDATE, b.verb());
            assertEquals("123456789", b.message().requestorId(), "wire RequestorID");
            assertEquals("demo@equinix.com", b.message().customerContact());
            assertEquals("Update case", b.message().shortDescription());
            assertEquals(ManagedServiceNotification.Level.MEDIUM, b.message().impact());
            assertEquals(ManagedServiceNotification.Level.MEDIUM, b.message().urgency());
            assertEquals("123456789", b.message().supplierTicketNumber());
        }
    }

    @Nested
    class Tolerance {

        @Test
        void unknownResourceIsPreservedAsUnknownNotification() {
            String json = "{\"Task\":{\"Id\":\"x1\",\"Resource\":\"Quantum.Teleport\",\"Version\":\"3.0\","
                    + "\"Body\":{\"Foo\":[1,2]}},\"Signature\":\"s\"}";
            UnknownNotification n = assertInstanceOf(UnknownNotification.class, parser.parse(json));
            assertEquals("x1", n.id());
            assertEquals("Quantum.Teleport", n.header().resource());
            assertEquals("{\"Foo\":[1,2]}", n.bodyJson());
            assertEquals(json, n.rawJson());
        }

        @Test
        void missingBodyIsUnknownWithNullBody() {
            UnknownNotification n = assertInstanceOf(UnknownNotification.class,
                    parser.parse("{\"Task\":{\"Id\":\"x\",\"Resource\":\"DataCenter.Maintenance\"}}"));
            assertNull(n.bodyJson());
            assertNull(n.header().signature());
        }

        @Test
        void bodyThatDoesNotFitItsModelIsUnknownNotAnError() {
            UnknownNotification n = assertInstanceOf(UnknownNotification.class, parser.parse(
                    "{\"Task\":{\"Resource\":\"DataCenter.Incident\",\"Body\":{\"Sequence\":{\"nested\":true}}}}"));
            assertEquals("{\"Sequence\":{\"nested\":true}}", n.bodyJson());
        }

        @Test
        void unknownFieldsAreIgnoredAndNamesAreCaseInsensitive() {
            IbxNotification n = assertInstanceOf(IbxNotification.class, parser.parse(
                    "{\"task\":{\"ID\":\"abc\",\"resource\":\"datacenter.incident\",\"verb\":\"update\","
                            + "\"CreateTimeUTC\":\"2024-01-01T00:00:00Z\",\"NewHeader\":1,"
                            + "\"body\":{\"servicerid\":\"5-1\",\"IMPACT\":\"service_impacting\",\"Brand\":\"new\","
                            + "\"assets\":[{\"ibx\":\"DC1\",\"Cage\":\"C1\",\"Unexpected\":true}]}}}"));
            assertEquals("abc", n.id());
            assertEquals(EmgVerb.UPDATE, n.header().verb());
            assertEquals(Instant.parse("2024-01-01T00:00:00Z"), n.header().creationTime(), "CreateTimeUTC alias");
            assertEquals("5-1", n.body().servicerId());
            assertTrue(n.body().isServiceImpacting());
            assertEquals(new TicketBody.Asset("DC1", "C1", null, null, null), n.body().assets().get(0));
        }

        @Test
        void unknownEnumValuesReadAsUnknown() {
            IbxNotification n = (IbxNotification) parser.parse("{\"Task\":{\"Resource\":\"DataCenter.Incident\","
                    + "\"Verb\":\"Merge\",\"Body\":{\"State\":\"ON-FIRE\",\"Impact\":\"OUTAGE\"}}}");
            assertEquals(EmgVerb.UNKNOWN, n.header().verb());
            assertEquals(TicketState.UNKNOWN, n.body().state());
            assertEquals(ServiceImpact.UNKNOWN, n.body().impact());
            assertFalse(n.body().isServiceImpacting());
        }

        @Test
        void inProcessStateWithHyphen() {
            IbxNotification n = (IbxNotification) parser.parse(
                    "{\"Task\":{\"Resource\":\"DataCenter.Incident\",\"Body\":{\"State\":\"IN-PROCESS\"}}}");
            assertEquals(TicketState.IN_PROCESS, n.body().state());
        }

        @Test
        void unparseableTimestampReadsAsNull() {
            IbxNotification n = (IbxNotification) parser.parse("{\"Task\":{\"Resource\":\"DataCenter.Incident\","
                    + "\"CreationTimeUTC\":\"yesterday\",\"Body\":{\"StartDateTime\":\"soon\"}}}");
            assertNull(n.header().creationTime());
            assertNull(n.body().startDateTime());
        }

        @Test
        void taskAndBodyEncodedAsJsonStrings() {
            String inner = "{\"Id\":\"s1\",\"Resource\":\"Network.Incident\",\"Body\":\"{\\\"ServicerId\\\":\\\"5-9\\\"}\"}";
            String json = "{\"Task\":" + Fixtures.JSON.valueToTree(inner) + ",\"Signature\":\"x\"}";
            NetworkNotification n = assertInstanceOf(NetworkNotification.class, parser.parse(json));
            assertEquals("s1", n.id());
            assertEquals("5-9", n.body().servicerId());
        }

        @Test
        void bytesWithUtf8BomAndNonAscii() {
            String json = "﻿{\"Task\":{\"Resource\":\"DataCenter.Advisory\",\"Body\":{\"Description\":\"Zürich ✓\"}}}";
            IbxNotification n = (IbxNotification) parser.parse(json.getBytes(StandardCharsets.UTF_8));
            assertEquals("Zürich ✓", n.body().description());
            assertEquals(json.substring(1), n.rawJson());
        }

        @Test
        void listsAreImmutableAndNullElementsDropped() {
            BillingNotification n = (BillingNotification) parser.parse("{\"Task\":{\"Resource\":\"CrossConnect\","
                    + "\"Body\":{\"LineDetails\":[null,{\"Id\":\"L1\",\"BilledLines\":\"single\"}]}}}");
            assertEquals(1, n.body().lineDetails().size());
            assertEquals(List.of("single"), n.body().lineDetails().get(0).billedLines(), "single value as array");
            assertThrows(UnsupportedOperationException.class, () -> n.body().lineDetails().clear());
            assertThrows(UnsupportedOperationException.class,
                    () -> n.body().lineDetails().get(0).billedLines().add("x"));
        }

        @Test
        void orderServiceDetailsCannotBeMutatedThroughTheModel() {
            OrderNotification n = (OrderNotification) parser.parse(
                    Fixtures.envelope("SmartHands", "1.0", "spec-order-body.json"));
            ((com.fasterxml.jackson.databind.node.ObjectNode) n.body().serviceDetails()).put("WorkType", "changed");
            assertEquals("SmartHands", n.body().serviceDetails().get("WorkType").asText());
        }
    }

    @Nested
    class Rejection {

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "[1,2]", "\"Task\"", "{\"Signature\":\"x\"}", "{\"Task\":42}",
                "{\"Task\":\"not json\"}"})
        void notAnEnvelope(String input) {
            assertThrows(EmgParseException.class, () -> parser.parse(input));
        }

        @Test
        void invalidJsonMessageDoesNotEchoThePayload() {
            EmgParseException e = assertThrows(EmgParseException.class,
                    () -> parser.parse("{\"Task\": {\"Id\": \"SECRET-PAYLOAD\", oops"));
            assertNotNull(e.getMessage());
            assertFalse(e.getMessage().contains("SECRET-PAYLOAD"), e.getMessage());
        }

        @Test
        void nullInput() {
            assertThrows(NullPointerException.class, () -> parser.parse((String) null));
            assertThrows(NullPointerException.class, () -> parser.parse((byte[]) null));
        }
    }
}
