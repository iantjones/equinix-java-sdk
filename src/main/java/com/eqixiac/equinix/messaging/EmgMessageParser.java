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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parses Equinix Messaging Gateway message bodies into typed {@link EmgMessage}s. Transport
 * agnostic: feed it the bytes of an AMQP message from any client. Thread-safe and stateless.
 *
 * <h2>Tolerance</h2>
 * <ul>
 *   <li>Attribute names are matched case-insensitively; unknown attributes are ignored.</li>
 *   <li>Unknown enum values read as {@code UNKNOWN}; timestamps without an offset are read as UTC;
 *       an unparseable timestamp reads as {@code null}; numbers sent as strings are accepted.</li>
 *   <li>Unescaped control characters inside strings are accepted (the documented IBX sample has a
 *       line break inside {@code Signature}).</li>
 *   <li>{@code Task} (and {@code Body}) may arrive as a JSON-encoded string rather than an object;
 *       Equinix's own Java template decodes {@code Task} that way, so both forms are accepted.</li>
 *   <li>An unrecognised {@code Task.Resource}, or a body that does not fit its resource's model,
 *       yields an {@link UnknownNotification} rather than an error.</li>
 * </ul>
 * Only input that is not an envelope at all raises {@link EmgParseException}.
 *
 * <h2>Routing</h2>
 * The message type is chosen from {@code Task.Resource} as in the AsyncAPI spec's conditional
 * schemas, refined by body shape where the spec and the documented samples disagree:
 * {@code DataCenter.Network.Port}/{@code .VirtualConnection} with a Fabric-shaped body
 * (a {@code data} attribute) is a {@link FabricNotification}, otherwise a
 * {@link NetworkNotification}; an order-family resource whose body has {@code LineDetails} is a
 * {@link BillingNotification}, one with {@code ActivityId} an {@link OrderNotification}.
 */
public final class EmgMessageParser {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            // The documented IBX sample carries a raw line break inside the Signature string.
            .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .addModule(new SimpleModule().addDeserializer(Instant.class, new LenientInstantDeserializer()))
            .build();

    private static final Set<String> IBX = Set.of("datacenter.incident", "datacenter.maintenance",
            "datacenter.securityincident", "datacenter.advisory");
    private static final Set<String> NETWORK = Set.of("network.incident", "network.maintenance");
    private static final Set<String> FABRIC_OR_NETWORK = Set.of("datacenter.network.port",
            "datacenter.network.virtualconnection");
    private static final Set<String> MANAGED = Set.of("troubleticket.managedservice",
            "servicerequest.managedservice");
    /** Spec routes these to BillingNotificationBody; "crossconnnect" is the documented sample's spelling. */
    private static final Set<String> BILLING_DEFAULT = Set.of("crossconnect", "crossconnnect");
    /** Spec routes these to OrderNotificationBody. */
    private static final Set<String> ORDER_DEFAULT = Set.of("shipping", "smarthands", "breakfix", "workvisit",
            "colo", "accessory", "ibxsmartview", "networkproduct", "power", "securityaccess");

    /** Creates a parser. */
    public EmgMessageParser() {
    }

    /**
     * Parses a UTF-8 encoded message body.
     *
     * @param body the AMQP message body
     * @return the typed message, never {@code null}
     * @throws EmgParseException if the input is not an EMG envelope
     */
    public EmgMessage parse(byte[] body) {
        Objects.requireNonNull(body, "body");
        return parse(new String(body, StandardCharsets.UTF_8));
    }

    /**
     * Parses a message body.
     *
     * @param json the message JSON
     * @return the typed message, never {@code null}
     * @throws EmgParseException if the input is not an EMG envelope
     */
    public EmgMessage parse(String json) {
        Objects.requireNonNull(json, "json");
        String raw = json.startsWith("﻿") ? json.substring(1) : json;
        if (raw.isBlank()) {
            throw new EmgParseException("EMG message is empty");
        }
        JsonNode root = readTree(raw, "message");
        if (!root.isObject()) {
            throw new EmgParseException("EMG message is not a JSON object");
        }
        JsonNode task = unwrap(field(root, "Task"), "Task");
        if (task == null || !task.isObject()) {
            throw new EmgParseException("EMG message has no Task object");
        }
        String signature = text(root, "Signature");
        EmgHeader header = new EmgHeader(text(task, "Id"), text(task, "Source"),
                EmgVerb.fromWire(text(task, "Verb")), text(task, "Resource"), text(task, "ContentType"),
                Wire.parseInstant(text(task, "CreationTimeUTC", "CreateTimeUTC")), text(task, "Version"),
                signature == null ? null : signature.trim());
        JsonNode body = unwrapBody(field(task, "Body"));
        return typed(header, body, raw);
    }

    private static EmgMessage typed(EmgHeader header, JsonNode body, String raw) {
        if (body == null || !body.isObject()) {
            return unknown(header, body, raw);
        }
        String resource = Wire.key(header.resource());
        try {
            if (IBX.contains(resource)) {
                return new IbxNotification(header, bind(body, TicketBody.class), raw);
            }
            if (NETWORK.contains(resource)) {
                return new NetworkNotification(header, bind(body, TicketBody.class), raw);
            }
            if (FABRIC_OR_NETWORK.contains(resource)) {
                return field(body, "data") != null
                        ? new FabricNotification(header, bind(body, FabricNotification.Body.class), raw)
                        : new NetworkNotification(header, bind(body, TicketBody.class), raw);
            }
            if (MANAGED.contains(resource)) {
                return new ManagedServiceNotification(header,
                        bind(body, ManagedServiceNotification.Body.class), raw);
            }
            if (BILLING_DEFAULT.contains(resource) || ORDER_DEFAULT.contains(resource)) {
                boolean billing = field(body, "LineDetails") != null
                        || (field(body, "ActivityId") == null && BILLING_DEFAULT.contains(resource));
                return billing
                        ? new BillingNotification(header, bind(body, BillingNotification.Body.class), raw)
                        : new OrderNotification(header, bind(body, OrderNotification.Body.class), raw);
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            // Body does not fit the model for its resource: keep it rather than fail the message.
            return unknown(header, body, raw);
        }
        return unknown(header, body, raw);
    }

    private static UnknownNotification unknown(EmgHeader header, JsonNode body, String raw) {
        return new UnknownNotification(header, body == null || body.isNull() ? null : body.toString(), raw);
    }

    private static <T> T bind(JsonNode body, Class<T> type) throws JsonProcessingException {
        return MAPPER.treeToValue(body, type);
    }

    private static JsonNode readTree(String json, String what) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            // getOriginalMessage() omits Jackson's source-location excerpt, so no payload leaks.
            throw new EmgParseException("EMG " + what + " is not valid JSON: " + e.getOriginalMessage());
        }
    }

    private static JsonNode unwrap(JsonNode node, String what) {
        if (node != null && node.isTextual()) {
            return readTree(node.textValue(), what);
        }
        return node;
    }

    private static JsonNode unwrapBody(JsonNode node) {
        if (node != null && node.isTextual()) {
            try {
                return MAPPER.readTree(node.textValue());
            } catch (JsonProcessingException e) {
                return node;
            }
        }
        return node;
    }

    /** Case-insensitive attribute lookup; the first name present wins. Null JSON values count as absent. */
    private static JsonNode field(JsonNode node, String... names) {
        for (String name : names) {
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                if (e.getKey().equalsIgnoreCase(name) && !e.getValue().isNull()) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    private static String text(JsonNode node, String... names) {
        JsonNode value = field(node, names);
        return value == null || !value.isValueNode() ? null : value.asText();
    }

    /** Lenient {@link Instant} reader: ISO-8601 with or without offset (UTC assumed), epoch millis. */
    private static final class LenientInstantDeserializer extends StdDeserializer<Instant> {

        private static final long serialVersionUID = 1L;

        LenientInstantDeserializer() {
            super(Instant.class);
        }

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken token = p.currentToken();
            if (token == JsonToken.VALUE_STRING) {
                return Wire.parseInstant(p.getText());
            }
            if (token == JsonToken.VALUE_NUMBER_INT) {
                return Instant.ofEpochMilli(p.getLongValue());
            }
            p.skipChildren();
            return null;
        }
    }
}
