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

package api.equinix.javasdk.mcp.server.broker;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.FabricGateway;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.fabric.enums.AccessPointType;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.InterfaceType;
import api.equinix.javasdk.fabric.enums.NetworkScope;
import api.equinix.javasdk.fabric.enums.NetworkType;
import api.equinix.javasdk.fabric.enums.NotificationType;
import api.equinix.javasdk.fabric.enums.ServiceTokenType;
import api.equinix.javasdk.fabric.enums.Side;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.ServiceToken;
import api.equinix.javasdk.fabric.model.implementation.LinkProtocol;
import api.equinix.javasdk.fabric.model.implementation.LocationCode;
import api.equinix.javasdk.fabric.model.json.creators.ConnectionOperator;
import api.equinix.javasdk.fabric.model.json.creators.NetworkOperator;
import api.equinix.javasdk.fabric.model.json.creators.ServiceTokenOperator;
import api.equinix.javasdk.mcp.server.ServerContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Compiles a broker change spec into the SDK's fluent creators and executes it — the same
 * compilation for both phases, differing only in whether {@code dryRun()} is applied. All
 * spec validation happens <em>before</em> any SDK client is touched, so a malformed spec
 * fails with a precise, LLM-correctable message and zero side effects (not even a facade
 * construction).
 *
 * <p>Unknown spec fields are rejected rather than silently dropped: a mutation broker must
 * never execute something subtly different from what the agent believes it proposed.</p>
 */
final class ChangeCompiler {

    private static final Set<String> CONNECTION_KEYS = Set.of("type", "name", "bandwidth_mbps",
            "notification_emails", "a_side", "z_side", "purchase_order_number", "project_id");
    private static final Set<String> SIDE_KEYS = Set.of("port_uuid", "service_profile_uuid",
            "cloud_router_uuid", "network_uuid", "service_token_uuid", "virtual_device_uuid",
            "interface_type", "interface_id", "link_protocol");
    private static final Set<String> LINK_PROTOCOL_KEYS = Set.of("type", "vlan_tag", "vlan_c_tag", "vlan_s_tag");
    private static final Set<String> NETWORK_KEYS = Set.of("type", "name", "scope",
            "notification_emails", "project_id", "metro_code");
    private static final Set<String> TOKEN_KEYS = Set.of("issuer_side", "type", "name", "description",
            "expiry_days", "project_id", "connection_type", "allow_remote_connection",
            "allow_custom_bandwidth", "bandwidth_limit_mbps", "supported_bandwidths_mbps",
            "access_point", "notification_emails");
    private static final Set<String> ACCESS_POINT_KEYS = Set.of("port_uuid", "virtual_device_uuid",
            "network_uuid", "interface_id", "link_protocol");

    private ChangeCompiler() {
    }

    /**
     * Validates the spec, assembles the matching SDK creator, and executes it — as a real
     * dry run when {@code dryRun} is {@code true} (the API validates, nothing is provisioned),
     * as the real create otherwise. Returns the resulting entity summarized for an agent
     * (dry-run echoes may carry no uuid/state; those fields are simply absent).
     */
    static ObjectNode execute(ChangeType changeType, JsonNode spec, ServerContext ctx, boolean dryRun) {
        return switch (changeType) {
            case CONNECTION_CREATE -> createConnection(spec, ctx, dryRun);
            case NETWORK_CREATE -> createNetwork(spec, ctx, dryRun);
            case SERVICE_TOKEN_CREATE -> createServiceToken(spec, ctx, dryRun);
        };
    }

    // ── connection_create ───────────────────────────────────────────────────

    /** The connection type of a validated spec, for the price context. */
    static ConnectionType connectionType(JsonNode spec) {
        return strictEnum(ConnectionType.class, requireString(spec, "type", "spec.type"), "spec.type");
    }

    private static ObjectNode createConnection(JsonNode spec, ServerContext ctx, boolean dryRun) {
        rejectUnknownKeys(spec, CONNECTION_KEYS, "spec");
        ConnectionType type = connectionType(spec);
        String name = requireString(spec, "name", "spec.name");
        int bandwidth = requireInt(spec, "bandwidth_mbps", "spec.bandwidth_mbps");
        List<String> emails = stringList(spec, "notification_emails");
        if (emails.isEmpty()) {
            throw new IllegalArgumentException("'spec.notification_emails' is required: at least one "
                    + "email address for provisioning notifications.");
        }
        Optional<String> purchaseOrder = optString(spec, "purchase_order_number");
        Optional<String> projectId = optString(spec, "project_id");
        SideSpec aSide = parseSide(spec, "a_side");
        SideSpec zSide = parseSide(spec, "z_side");

        ConnectionOperator.ConnectionBuilder builder = ctx.fabric().connections().define(type)
                .name(name)
                .bandwidth(bandwidth);
        purchaseOrder.ifPresent(builder::purchaseOrderNumber);
        projectId.ifPresent(builder::project);
        emails.forEach(builder::notification);
        applySide(builder, true, aSide);
        applySide(builder, false, zSide);
        if (dryRun) {
            builder.dryRun();
        }
        Connection connection = builder.create();

        ObjectNode node = ctx.objectMapper().createObjectNode();
        node.put("resource", "connection");
        putIfPresent(node, "uuid", connection.getUuid());
        putIfPresent(node, "name", connection.getName());
        putEnum(node, "type", connection.getType());
        putEnum(node, "state", connection.getState());
        if (connection.getBandwidth() != null) {
            node.put("bandwidth_mbps", connection.getBandwidth());
        }
        return node;
    }

    private enum SideKind { PORT, SERVICE_PROFILE, CLOUD_ROUTER, NETWORK, SERVICE_TOKEN, VIRTUAL_DEVICE }

    private record SideSpec(SideKind kind, String uuid, LinkProtocol linkProtocol,
                            InterfaceType interfaceType, Integer interfaceId) {
    }

    private static SideSpec parseSide(JsonNode spec, String field) {
        JsonNode node = spec.get(field);
        String where = "spec." + field;
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("'" + where + "' is required: an object identifying "
                    + "that side's endpoint (exactly one of port_uuid, service_profile_uuid, "
                    + "cloud_router_uuid, network_uuid, service_token_uuid, virtual_device_uuid).");
        }
        rejectUnknownKeys(node, SIDE_KEYS, where);

        List<SideKind> present = new ArrayList<>();
        String uuid = null;
        for (SideKind kind : SideKind.values()) {
            Optional<String> value = optString(node, sideKey(kind));
            if (value.isPresent()) {
                present.add(kind);
                uuid = value.get();
            }
        }
        if (present.size() != 1) {
            throw new IllegalArgumentException("'" + where + "' must identify exactly one endpoint: give "
                    + "exactly one of port_uuid, service_profile_uuid, cloud_router_uuid, network_uuid, "
                    + "service_token_uuid, virtual_device_uuid (found " + present.size() + ").");
        }
        SideKind kind = present.get(0);

        boolean needsProtocol = kind == SideKind.PORT || kind == SideKind.SERVICE_PROFILE
                || kind == SideKind.VIRTUAL_DEVICE;
        LinkProtocol protocol = null;
        if (needsProtocol) {
            JsonNode lp = node.get("link_protocol");
            if (lp == null || !lp.isObject()) {
                throw new IllegalArgumentException("'" + where + ".link_protocol' is required for a "
                        + kind.name().toLowerCase(Locale.ROOT) + " endpoint: {type: dot1q|qinq|untagged, "
                        + "vlan_tag / vlan_c_tag + vlan_s_tag}.");
            }
            protocol = parseLinkProtocol(lp, where + ".link_protocol", true);
        }
        else if (node.has("link_protocol")) {
            throw new IllegalArgumentException("'" + where + ".link_protocol' does not apply to a "
                    + kind.name().toLowerCase(Locale.ROOT) + " endpoint; remove it.");
        }

        InterfaceType interfaceType = null;
        Integer interfaceId = null;
        if (kind == SideKind.VIRTUAL_DEVICE) {
            interfaceType = optString(node, "interface_type")
                    .map(raw -> strictEnum(InterfaceType.class, raw, where + ".interface_type"))
                    .orElse(InterfaceType.NETWORK);
            interfaceId = optInt(node, "interface_id").orElse(null);
        }
        else if (node.has("interface_type") || node.has("interface_id")) {
            throw new IllegalArgumentException("'" + where + "' interface_type/interface_id apply only to "
                    + "a virtual_device endpoint; remove them.");
        }
        return new SideSpec(kind, uuid, protocol, interfaceType, interfaceId);
    }

    private static String sideKey(SideKind kind) {
        return switch (kind) {
            case PORT -> "port_uuid";
            case SERVICE_PROFILE -> "service_profile_uuid";
            case CLOUD_ROUTER -> "cloud_router_uuid";
            case NETWORK -> "network_uuid";
            case SERVICE_TOKEN -> "service_token_uuid";
            case VIRTUAL_DEVICE -> "virtual_device_uuid";
        };
    }

    private static void applySide(ConnectionOperator.ConnectionBuilder builder, boolean aSide, SideSpec side) {
        switch (side.kind()) {
            case PORT -> {
                if (aSide) {
                    builder.aSideAccessPointPort(side.uuid(), side.linkProtocol());
                }
                else {
                    builder.zSideAccessPointPort(side.uuid(), side.linkProtocol());
                }
            }
            case SERVICE_PROFILE -> {
                if (aSide) {
                    builder.aSideAccessPointServiceProfile(side.uuid(), side.linkProtocol());
                }
                else {
                    builder.zSideAccessPointServiceProfile(side.uuid(), side.linkProtocol());
                }
            }
            case CLOUD_ROUTER -> {
                if (aSide) {
                    builder.aSideAccessPointCloudRouter(side.uuid());
                }
                else {
                    builder.zSideAccessPointCloudRouter(side.uuid());
                }
            }
            case NETWORK -> {
                if (aSide) {
                    builder.aSideAccessPointNetwork(side.uuid());
                }
                else {
                    builder.zSideAccessPointNetwork(side.uuid());
                }
            }
            case SERVICE_TOKEN -> {
                if (aSide) {
                    builder.aSideServiceToken(side.uuid());
                }
                else {
                    builder.zSideServiceToken(side.uuid());
                }
            }
            case VIRTUAL_DEVICE -> {
                if (aSide) {
                    builder.aSideAccessPoint(side.uuid(), side.linkProtocol(),
                            side.interfaceType(), side.interfaceId());
                }
                else {
                    builder.zSideAccessPoint(side.uuid(), side.linkProtocol(),
                            side.interfaceType(), side.interfaceId());
                }
            }
        }
    }

    private static LinkProtocol parseLinkProtocol(JsonNode node, String where, boolean allowUntagged) {
        rejectUnknownKeys(node, LINK_PROTOCOL_KEYS, where);
        String type = requireString(node, "type", where + ".type").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "dot1q" -> LinkProtocol.dot1q()
                    .vlanTag(requireInt(node, "vlan_tag", where + ".vlan_tag"))
                    .create();
            case "qinq" -> LinkProtocol.qinq()
                    .vlanCTag(requireInt(node, "vlan_c_tag", where + ".vlan_c_tag"))
                    .vlanSTag(requireInt(node, "vlan_s_tag", where + ".vlan_s_tag"))
                    .create();
            case "untagged" -> {
                if (!allowUntagged) {
                    throw new IllegalArgumentException("'" + where + ".type' untagged is not supported "
                            + "here; use dot1q or qinq.");
                }
                yield LinkProtocol.untagged().create();
            }
            default -> throw new IllegalArgumentException("'" + where + ".type' value '" + type
                    + "' is not valid. Valid values: dot1q, qinq" + (allowUntagged ? ", untagged" : "") + ".");
        };
    }

    // ── network_create ──────────────────────────────────────────────────────

    private static ObjectNode createNetwork(JsonNode spec, ServerContext ctx, boolean dryRun) {
        rejectUnknownKeys(spec, NETWORK_KEYS, "spec");
        NetworkType type = strictEnum(NetworkType.class,
                requireString(spec, "type", "spec.type"), "spec.type");
        String name = requireString(spec, "name", "spec.name");
        NetworkScope scope = strictEnum(NetworkScope.class,
                requireString(spec, "scope", "spec.scope"), "spec.scope");
        List<String> emails = stringList(spec, "notification_emails");
        if (emails.isEmpty()) {
            throw new IllegalArgumentException("'spec.notification_emails' is required: at least one "
                    + "email address for provisioning notifications.");
        }
        Optional<String> projectId = optString(spec, "project_id");
        Optional<MetroCode> metro = optString(spec, "metro_code").map(ChangeCompiler::metroCode);

        NetworkOperator.NetworkBuilder builder = requireFullFabric(ctx, "network_create")
                .networks().define(type)
                .name(name)
                .scope(scope);
        projectId.ifPresent(id -> builder.withProject(new Project(id)));
        metro.ifPresent(code -> builder.withLocation(new LocationCode(code)));
        emails.forEach(email -> builder.notification(NotificationType.ALL, email));
        if (dryRun) {
            builder.dryRun();
        }
        Network network = builder.create();

        ObjectNode node = ctx.objectMapper().createObjectNode();
        node.put("resource", "network");
        putIfPresent(node, "uuid", network.getUuid());
        putIfPresent(node, "name", network.getName());
        putEnum(node, "type", network.getType());
        putEnum(node, "state", network.getState());
        putEnum(node, "scope", network.getScope());
        return node;
    }

    // ── service_token_create ────────────────────────────────────────────────

    private static ObjectNode createServiceToken(JsonNode spec, ServerContext ctx, boolean dryRun) {
        rejectUnknownKeys(spec, TOKEN_KEYS, "spec");
        Side issuerSide = parseIssuerSide(requireString(spec, "issuer_side", "spec.issuer_side"));
        ServiceTokenType type = strictEnum(ServiceTokenType.class,
                requireString(spec, "type", "spec.type"), "spec.type");
        String name = requireString(spec, "name", "spec.name");

        JsonNode accessPoint = spec.get("access_point");
        if (accessPoint == null || !accessPoint.isObject()) {
            throw new IllegalArgumentException("'spec.access_point' is required: an object with exactly one "
                    + "of port_uuid, virtual_device_uuid, network_uuid (plus interface_id for virtual "
                    + "devices and an optional link_protocol).");
        }
        rejectUnknownKeys(accessPoint, ACCESS_POINT_KEYS, "spec.access_point");
        Optional<String> portUuid = optString(accessPoint, "port_uuid");
        Optional<String> virtualDeviceUuid = optString(accessPoint, "virtual_device_uuid");
        Optional<String> networkUuid = optString(accessPoint, "network_uuid");
        int identifying = (portUuid.isPresent() ? 1 : 0)
                + (virtualDeviceUuid.isPresent() ? 1 : 0)
                + (networkUuid.isPresent() ? 1 : 0);
        if (identifying != 1) {
            throw new IllegalArgumentException("'spec.access_point' must identify exactly one asset: give "
                    + "exactly one of port_uuid, virtual_device_uuid, network_uuid (found "
                    + identifying + ").");
        }
        if (networkUuid.isPresent() && accessPoint.has("link_protocol")) {
            throw new IllegalArgumentException("'spec.access_point.link_protocol' does not apply to a "
                    + "network access point; remove it.");
        }
        Optional<ConnectionType> connectionType = optString(spec, "connection_type")
                .map(raw -> strictEnum(ConnectionType.class, raw, "spec.connection_type"));
        LinkProtocol protocol = accessPoint.has("link_protocol")
                ? parseLinkProtocol(accessPoint.get("link_protocol"), "spec.access_point.link_protocol", false)
                : null;

        ServiceTokenOperator.ServiceTokenBuilder builder = requireFullFabric(ctx, "service_token_create")
                .serviceTokens().define(issuerSide)
                .ofType(type)
                .withName(name);
        optString(spec, "description").ifPresent(builder::withDescription);
        optInt(spec, "expiry_days").ifPresent(builder::withExpiry);
        optString(spec, "project_id").ifPresent(builder::inProject);
        connectionType.ifPresent(builder::forConnectionType);
        optBool(spec, "allow_remote_connection").ifPresent(builder::allowRemoteConnection);
        optBool(spec, "allow_custom_bandwidth").ifPresent(builder::allowCustomBandwidth);
        optInt(spec, "bandwidth_limit_mbps").ifPresent(builder::withBandwidthLimit);
        List<Integer> supported = intList(spec, "supported_bandwidths_mbps");
        if (!supported.isEmpty()) {
            builder.withSupportedBandwidths(supported);
        }
        portUuid.ifPresent(uuid -> builder.forAccessPointType(AccessPointType.COLO).onPortUuid(uuid));
        virtualDeviceUuid.ifPresent(uuid -> {
            builder.forAccessPointType(AccessPointType.VD).onVirtualDeviceUuid(uuid);
            optInt(accessPoint, "interface_id").ifPresent(builder::withNetworkInterfaceId);
        });
        networkUuid.ifPresent(uuid -> builder.forAccessPointType(AccessPointType.NETWORK).onNetworkUuid(uuid));
        if (protocol != null) {
            if (protocol.getVlanTag() != null) {
                builder.usingProtocolDot1q(protocol.getVlanTag());
            }
            else {
                builder.usingProtocolQinQ(protocol.getVlanCTag(), protocol.getVlanSTag());
            }
        }
        stringList(spec, "notification_emails").forEach(builder::withNotificationEmail);
        if (dryRun) {
            builder.dryRun();
        }
        ServiceToken token = builder.create();

        ObjectNode node = ctx.objectMapper().createObjectNode();
        node.put("resource", "service_token");
        putIfPresent(node, "uuid", token.getUuid());
        putIfPresent(node, "name", token.getName());
        putEnum(node, "type", token.getType());
        putEnum(node, "state", token.getState());
        if (token.getExpiry() != null) {
            node.put("expiry_days", token.getExpiry());
        }
        return node;
    }

    private static Side parseIssuerSide(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "a_side" -> Side.A_Side;
            case "z_side" -> Side.Z_Side;
            default -> throw new IllegalArgumentException("'spec.issuer_side' value '" + raw
                    + "' is not valid. Valid values: a_side, z_side.");
        };
    }

    // ── shared plumbing ─────────────────────────────────────────────────────

    /**
     * Networks and service tokens are not on the narrow {@link FabricGateway} view the design
     * engines use, so those change types need the full {@link Fabric} client (which is what a
     * session-built context always provides — {@code Fabric} implements the gateway).
     */
    private static Fabric requireFullFabric(ServerContext ctx, String what) {
        FabricGateway gateway = ctx.fabric();
        if (gateway instanceof Fabric fabric) {
            return fabric;
        }
        throw new IllegalStateException(what + " needs the full Fabric client (networks and service "
                + "tokens are not part of the narrow FabricGateway view), but this server context was "
                + "built with an injected gateway of type " + gateway.getClass().getName() + ".");
    }

    private static MetroCode metroCode(String code) {
        try {
            return MetroCode.fromCode(code);
        }
        catch (RuntimeException e) {
            throw new IllegalArgumentException("'spec.metro_code' value '" + code + "' is not a known "
                    + "Equinix metro code. Use the two-letter code, e.g. 'DC' (Ashburn), 'LD' (London).");
        }
    }

    private static void rejectUnknownKeys(JsonNode node, Set<String> known, String where) {
        List<String> unknown = new ArrayList<>();
        node.fieldNames().forEachRemaining(field -> {
            if (!known.contains(field)) {
                unknown.add(field);
            }
        });
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("'" + where + "' has unknown field(s) " + unknown
                    + " — a mutation spec must be exact, nothing is silently dropped. Known fields: "
                    + String.join(", ", known.stream().sorted().toList()) + ".");
        }
    }

    private static <E extends Enum<E>> E strictEnum(Class<E> type, String raw, String field) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        E value;
        try {
            value = Enum.valueOf(type, normalized);
        }
        catch (IllegalArgumentException e) {
            throw invalidEnum(type, raw, field);
        }
        if ("UNKNOWN".equals(value.name())) {
            throw invalidEnum(type, raw, field);
        }
        return value;
    }

    private static <E extends Enum<E>> IllegalArgumentException invalidEnum(Class<E> type, String raw,
                                                                            String field) {
        List<String> valid = new ArrayList<>();
        for (E constant : type.getEnumConstants()) {
            if (!"UNKNOWN".equals(constant.name())) {
                valid.add(constant.name());
            }
        }
        return new IllegalArgumentException("'" + field + "' value '" + raw + "' is not valid. "
                + "Valid values: " + String.join(", ", valid) + ".");
    }

    private static String requireString(JsonNode node, String field, String where) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().trim().isEmpty()) {
            throw new IllegalArgumentException("'" + where + "' is required and must be a non-empty string.");
        }
        return value.asText().trim();
    }

    private static int requireInt(JsonNode node, String field, String where) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) {
            throw new IllegalArgumentException("'" + where + "' is required and must be an integer.");
        }
        return value.asInt();
    }

    private static Optional<String> optString(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(value.asText().trim());
    }

    private static Optional<Integer> optInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? Optional.of(value.asInt()) : Optional.empty();
    }

    private static Optional<Boolean> optBool(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isBoolean() ? Optional.of(value.asBoolean()) : Optional.empty();
    }

    private static List<String> stringList(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        JsonNode array = node.get(field);
        if (array != null && array.isArray()) {
            array.forEach(item -> {
                if (item.isTextual() && !item.asText().trim().isEmpty()) {
                    values.add(item.asText().trim());
                }
            });
        }
        return values;
    }

    private static List<Integer> intList(JsonNode node, String field) {
        List<Integer> values = new ArrayList<>();
        JsonNode array = node.get(field);
        if (array != null && array.isArray()) {
            array.forEach(item -> {
                if (item.isNumber()) {
                    values.add(item.asInt());
                }
            });
        }
        return values;
    }

    private static void putIfPresent(ObjectNode node, String field, String value) {
        if (value != null && !value.isEmpty()) {
            node.put(field, value);
        }
    }

    private static void putEnum(ObjectNode node, String field, Enum<?> value) {
        if (value != null) {
            node.put(field, value.name());
        }
    }
}
