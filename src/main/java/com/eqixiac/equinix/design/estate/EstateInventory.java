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

package com.eqixiac.equinix.design.estate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * An immutable, serializable snapshot of the part of an Equinix estate an analysis covers.
 *
 * <p>The snapshot is plain data (no live SDK objects), so an analysis can be repeated offline,
 * stored next to its report, and built by hand in tests. Every list is sorted by resource id so
 * the same estate always produces the same snapshot. Values the API did not report are {@code null}
 * (unknown), never zero. Anything that could not be read is listed in {@link #coverageGaps()}.</p>
 *
 * @param scope                the scope the snapshot was taken for
 * @param asOf                 when the snapshot was taken
 * @param source               where the data came from, for example {@code live Fabric v4 API} or {@code fixture}
 * @param ports                Fabric ports in scope
 * @param connections          Fabric virtual connections in scope
 * @param routers              Fabric Cloud Routers in scope
 * @param networks             Fabric networks in scope
 * @param serviceTokens        Fabric service tokens in scope
 * @param devices              Network Edge devices in scope
 * @param routerPackageLimits  Cloud Router package code to the maximum number of virtual connections,
 *                             as the {@code /routerPackages} API reported it (empty when not reported)
 * @param coverageGaps         what could not be read, and why
 * @param stats                how the snapshot was collected (requests, timings)
 */
public record EstateInventory(EstateScope scope, Instant asOf, String source,
                              List<PortRecord> ports, List<ConnectionRecord> connections,
                              List<RouterRecord> routers, List<NetworkRecord> networks,
                              List<ServiceTokenRecord> serviceTokens, List<DeviceRecord> devices,
                              Map<String, Integer> routerPackageLimits, List<CoverageGap> coverageGaps,
                              CollectionStats stats) {

    /** Sorts every list by id and makes every collection immutable. */
    public EstateInventory {
        if (scope == null || asOf == null) {
            throw new IllegalArgumentException("An inventory needs a scope and an as-of time");
        }
        source = source == null ? "unspecified" : source;
        ports = sorted(ports, PortRecord::id);
        connections = sorted(connections, ConnectionRecord::id);
        routers = sorted(routers, RouterRecord::id);
        networks = sorted(networks, NetworkRecord::id);
        serviceTokens = sorted(serviceTokens, ServiceTokenRecord::id);
        devices = sorted(devices, DeviceRecord::id);
        routerPackageLimits = routerPackageLimits == null ? Map.of()
                : java.util.Collections.unmodifiableMap(new TreeMap<>(routerPackageLimits));
        coverageGaps = coverageGaps == null ? List.of() : List.copyOf(coverageGaps);
        stats = stats == null ? CollectionStats.empty() : stats;
    }

    /**
     * Starts a hand-built inventory (tests, imports from other tools).
     *
     * @param scope the scope
     * @param asOf  the as-of time
     * @return a builder
     */
    public static Builder builder(EstateScope scope, Instant asOf) {
        return new Builder(scope, asOf);
    }

    /**
     * Counts of each resource type, for report headers.
     *
     * @return resource type to count, in a fixed order
     */
    @JsonIgnore
    public Map<String, Integer> counts() {
        Map<String, Integer> m = new java.util.LinkedHashMap<>();
        m.put("ports", ports.size());
        m.put("connections", connections.size());
        m.put("cloudRouters", routers.size());
        m.put("networks", networks.size());
        m.put("serviceTokens", serviceTokens.size());
        m.put("networkEdgeDevices", devices.size());
        return m;
    }

    /**
     * Finds a port by id.
     *
     * @param id the port id
     * @return the port, if in the snapshot
     */
    public Optional<PortRecord> port(String id) {
        return id == null ? Optional.empty() : ports.stream().filter(p -> id.equals(p.id())).findFirst();
    }

    /**
     * Finds a Cloud Router by id.
     *
     * @param id the router id
     * @return the router, if in the snapshot
     */
    public Optional<RouterRecord> router(String id) {
        return id == null ? Optional.empty() : routers.stream().filter(r -> id.equals(r.id())).findFirst();
    }

    /**
     * The connections whose A or Z side terminates on the given port.
     *
     * @param portId the port id
     * @return the connections, in id order
     */
    public List<ConnectionRecord> connectionsOnPort(String portId) {
        return connections.stream().filter(c -> c.touchesPort(portId)).toList();
    }

    /**
     * The connections whose A or Z side terminates on the given Cloud Router.
     *
     * @param routerId the router id
     * @return the connections, in id order
     */
    public List<ConnectionRecord> connectionsOnRouter(String routerId) {
        return connections.stream().filter(c -> routerId != null
                && (routerId.equals(c.aSide().routerId()) || routerId.equals(c.zSide().routerId()))).toList();
    }

    /**
     * Serializes the snapshot to JSON (ISO-8601 timestamps).
     *
     * @return the JSON document
     */
    public String toJson() {
        return EstateJson.write(this);
    }

    /**
     * Reads a snapshot written by {@link #toJson()}.
     *
     * @param json the JSON document
     * @return the snapshot
     * @throws IllegalArgumentException when the document is not a snapshot
     */
    public static EstateInventory fromJson(String json) {
        return EstateJson.read(json, EstateInventory.class);
    }

    private static <T> List<T> sorted(List<T> in, Function<T, String> key) {
        if (in == null) {
            return List.of();
        }
        List<T> out = new ArrayList<>(in);
        out.sort(Comparator.comparing(key, Comparator.nullsLast(Comparator.naturalOrder())));
        return List.copyOf(out);
    }

    /**
     * A Fabric port.
     *
     * @param id                the port uuid
     * @param name              the port name
     * @param metro             the metro code
     * @param state             the port state as the API reported it
     * @param speedMbps         the port bandwidth in Mbps (for a LAG, the aggregate)
     * @param usedMbps          the bandwidth allocated to virtual connections, as reported; {@code null} when not reported
     * @param availableMbps     the bandwidth still allocatable, as reported; {@code null} when not reported
     * @param encapsulation     the encapsulation type ({@code DOT1Q}, {@code QINQ}...)
     * @param lagEnabled        whether the port is a LAG
     * @param physicalPortCount the number of physical ports, when reported
     * @param redundancyPriority {@code PRIMARY} or {@code SECONDARY}, when reported
     * @param redundancyGroup   the device redundancy group, when reported
     * @param deviceName        the Equinix switch the port is on, when reported
     * @param connectionCount   the number of virtual connections the API reports on the port
     * @param projectId         the project id
     * @param accountNumber     the account number
     * @param createdAt         when the port was created
     * @param notificationCount the number of notification entries
     */
    public record PortRecord(String id, String name, String metro, String state, Integer speedMbps,
                             Integer usedMbps, Integer availableMbps, String encapsulation, Boolean lagEnabled,
                             Integer physicalPortCount, String redundancyPriority, String redundancyGroup,
                             String deviceName, Integer connectionCount, String projectId, String accountNumber,
                             Instant createdAt, Integer notificationCount) {
    }

    /**
     * One side of a virtual connection.
     *
     * @param type               the access-point type ({@code COLO}, {@code SP}, {@code CLOUD_ROUTER}, {@code VD}, {@code NETWORK}...)
     * @param metro              the metro code, when reported
     * @param portId             the port uuid for a {@code COLO} side
     * @param routerId           the Cloud Router uuid for a {@code CLOUD_ROUTER} side
     * @param deviceId           the Network Edge device uuid for a {@code VD} side
     * @param networkId          the network uuid for a {@code NETWORK} side
     * @param serviceProfileId   the service profile uuid for an {@code SP} side
     * @param serviceProfileName the service profile name (for example the cloud on-ramp)
     * @param serviceTokenId     the service token uuid, when the side was ordered with one
     */
    public record Endpoint(String type, String metro, String portId, String routerId, String deviceId,
                           String networkId, String serviceProfileId, String serviceProfileName,
                           String serviceTokenId) {

        /**
         * An endpoint about which nothing is known.
         *
         * @return the empty endpoint
         */
        public static Endpoint unknown() {
            return new Endpoint(null, null, null, null, null, null, null, null, null);
        }
    }

    /**
     * A Fabric virtual connection.
     *
     * @param id                 the connection uuid
     * @param name               the connection name
     * @param type               the connection type ({@code EVPL_VC}, {@code IP_VC}...)
     * @param state              the connection state
     * @param equinixStatus      {@code operation.equinixStatus}, for example {@code PENDING_APPROVAL}
     * @param providerStatus     {@code operation.providerStatus}, when reported
     * @param bandwidthMbps      the provisioned bandwidth in Mbps
     * @param aSide              the A side
     * @param zSide              the Z side
     * @param termMonths         {@code order.termLength} in months; {@code 1} or {@code null} means on-demand
     * @param redundancyGroup    the redundancy group id
     * @param redundancyPriority {@code PRIMARY} or {@code SECONDARY}
     * @param createdAt          when the connection was created
     * @param projectId          the project id
     * @param accountNumber      the account number
     * @param purchaseOrder      the purchase-order number, when set
     * @param notificationCount  the number of notification entries
     */
    public record ConnectionRecord(String id, String name, String type, String state, String equinixStatus,
                                   String providerStatus, Integer bandwidthMbps, Endpoint aSide, Endpoint zSide,
                                   Integer termMonths, String redundancyGroup, String redundancyPriority,
                                   Instant createdAt, String projectId, String accountNumber, String purchaseOrder,
                                   Integer notificationCount) {

        /** Replaces missing sides with {@link Endpoint#unknown()}. */
        public ConnectionRecord {
            aSide = aSide == null ? Endpoint.unknown() : aSide;
            zSide = zSide == null ? Endpoint.unknown() : zSide;
        }

        /**
         * Whether either side terminates on the port.
         *
         * @param portId the port id
         * @return {@code true} when it does
         */
        public boolean touchesPort(String portId) {
            return portId != null && (portId.equals(aSide.portId()) || portId.equals(zSide.portId()));
        }

        /**
         * Whether the connection is live (provisioned and carrying, or able to carry, traffic).
         *
         * @return {@code true} for {@code ACTIVE} or {@code PROVISIONED}
         */
        @JsonIgnore
        public boolean isLive() {
            return "ACTIVE".equals(state) || "PROVISIONED".equals(state);
        }

        /**
         * Whether the connection is billed month to month (no term or a 1-month term).
         *
         * @return {@code true} when on demand; {@code false} when a longer term is recorded
         */
        @JsonIgnore
        public boolean isOnDemand() {
            return termMonths == null || termMonths <= 1;
        }

        /**
         * The metro the connection is priced in: the A side's metro, else the Z side's.
         *
         * @return the metro code, or {@code null}
         */
        @JsonIgnore
        public String pricingMetro() {
            return aSide.metro() != null ? aSide.metro() : zSide.metro();
        }
    }

    /**
     * A Fabric Cloud Router.
     *
     * @param id                the router uuid
     * @param name              the router name
     * @param packageCode       the package code ({@code LAB}, {@code STANDARD}...)
     * @param metro             the metro code
     * @param state             the router state
     * @param connectionCount   {@code connectionsCount} as reported
     * @param termMonths        {@code order.termLength}, when reported
     * @param createdAt         when the router was created
     * @param projectId         the project id
     * @param accountNumber     the account number
     * @param notificationCount the number of notification entries
     */
    public record RouterRecord(String id, String name, String packageCode, String metro, String state,
                               Integer connectionCount, Integer termMonths, Instant createdAt, String projectId,
                               String accountNumber, Integer notificationCount) {

        /**
         * Whether the router is provisioned.
         *
         * @return {@code true} for {@code PROVISIONED}
         */
        @JsonIgnore
        public boolean isLive() {
            return "PROVISIONED".equals(state);
        }
    }

    /**
     * A Fabric network (EVPLAN, EPLAN, IPWAN...).
     *
     * @param id                the network uuid
     * @param name              the network name
     * @param type              the network type
     * @param scopeLevel        the network scope ({@code REGIONAL}, {@code GLOBAL}, {@code LOCAL})
     * @param state             the network state
     * @param connectionCount   {@code connectionsCount} as reported
     * @param createdAt         when the network was created
     * @param projectId         the project id
     * @param notificationCount the number of notification entries
     */
    public record NetworkRecord(String id, String name, String type, String scopeLevel, String state,
                                Integer connectionCount, Instant createdAt, String projectId,
                                Integer notificationCount) {
    }

    /**
     * A Fabric service token.
     *
     * @param id        the token uuid
     * @param name      the token name
     * @param type      the token type
     * @param state     the token state ({@code ACTIVE}, {@code INACTIVE}, {@code EXPIRED}, {@code DELETED})
     * @param expiresAt the expiry time, when set
     * @param createdAt when the token was created
     * @param projectId the project id
     */
    public record ServiceTokenRecord(String id, String name, String type, String state, Instant expiresAt,
                                     Instant createdAt, String projectId) {
    }

    /**
     * A Network Edge virtual device.
     *
     * @param id             the device uuid
     * @param name           the device name
     * @param deviceType     the device type code
     * @param metro          the metro code
     * @param status         the device status
     * @param redundancyType {@code PRIMARY}, {@code SECONDARY} or {@code null} for a single device
     * @param redundantId    the uuid of the redundant peer, when there is one
     * @param termMonths     the term length, when reported
     * @param createdAt      when the device was created
     * @param accountNumber  the account number
     */
    public record DeviceRecord(String id, String name, String deviceType, String metro, String status,
                               String redundancyType, String redundantId, Integer termMonths, Instant createdAt,
                               String accountNumber) {
    }

    /**
     * Something the collector could not read, recorded instead of failing the run.
     *
     * @param resourceType the resource type ({@code ports}, {@code connections}, {@code metrics}...)
     * @param reason       a short machine-readable reason ({@code FORBIDDEN}, {@code SERVER_ERROR},
     *                     {@code TRUNCATED}, {@code NOT_SCOPABLE}, {@code ERROR}...)
     * @param httpStatus   the HTTP status, when the reason was an HTTP error
     * @param detail       what was missed, in words
     */
    public record CoverageGap(String resourceType, String reason, Integer httpStatus, String detail) {
    }

    /**
     * How the snapshot was collected.
     *
     * @param requests     the number of API requests made
     * @param totalMillis  the wall-clock time of the collection
     * @param perType      resource type to its collection figures
     * @param warnings     loud warnings (for example a truncated, oversized estate)
     */
    public record CollectionStats(int requests, long totalMillis, Map<String, TypeStats> perType,
                                  List<String> warnings) {

        /** Copies the collections. */
        public CollectionStats {
            perType = perType == null ? Map.of() : java.util.Collections.unmodifiableMap(new TreeMap<>(perType));
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }

        /**
         * Stats for a hand-built inventory.
         *
         * @return empty stats
         */
        public static CollectionStats empty() {
            return new CollectionStats(0, 0, Map.of(), List.of());
        }
    }

    /**
     * Collection figures for one resource type.
     *
     * @param read      resources returned by the API
     * @param inScope   resources kept after scoping
     * @param pages     pages (requests) used
     * @param millis    time spent
     * @param truncated whether the per-type ceiling stopped the read early
     */
    public record TypeStats(int read, int inScope, int pages, long millis, boolean truncated) {
    }

    /** Builds an inventory by hand. */
    public static final class Builder {
        private final EstateScope scope;
        private final Instant asOf;
        private String source = "hand-built";
        private final List<PortRecord> ports = new ArrayList<>();
        private final List<ConnectionRecord> connections = new ArrayList<>();
        private final List<RouterRecord> routers = new ArrayList<>();
        private final List<NetworkRecord> networks = new ArrayList<>();
        private final List<ServiceTokenRecord> tokens = new ArrayList<>();
        private final List<DeviceRecord> devices = new ArrayList<>();
        private final Map<String, Integer> limits = new TreeMap<>();
        private final List<CoverageGap> gaps = new ArrayList<>();
        private CollectionStats stats = CollectionStats.empty();

        private Builder(EstateScope scope, Instant asOf) {
            this.scope = scope;
            this.asOf = asOf;
        }

        /**
         * Sets the source label.
         *
         * @param source the label
         * @return this builder
         */
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        /**
         * Adds a port.
         *
         * @param p the port
         * @return this builder
         */
        public Builder port(PortRecord p) {
            ports.add(p);
            return this;
        }

        /**
         * Adds a connection.
         *
         * @param c the connection
         * @return this builder
         */
        public Builder connection(ConnectionRecord c) {
            connections.add(c);
            return this;
        }

        /**
         * Adds a Cloud Router.
         *
         * @param r the router
         * @return this builder
         */
        public Builder router(RouterRecord r) {
            routers.add(r);
            return this;
        }

        /**
         * Adds a network.
         *
         * @param n the network
         * @return this builder
         */
        public Builder network(NetworkRecord n) {
            networks.add(n);
            return this;
        }

        /**
         * Adds a service token.
         *
         * @param t the token
         * @return this builder
         */
        public Builder serviceToken(ServiceTokenRecord t) {
            tokens.add(t);
            return this;
        }

        /**
         * Adds a Network Edge device.
         *
         * @param d the device
         * @return this builder
         */
        public Builder device(DeviceRecord d) {
            devices.add(d);
            return this;
        }

        /**
         * Records a router package connection limit.
         *
         * @param packageCode the package code
         * @param maxConnections the limit
         * @return this builder
         */
        public Builder routerPackageLimit(String packageCode, int maxConnections) {
            limits.put(packageCode, maxConnections);
            return this;
        }

        /**
         * Records a coverage gap.
         *
         * @param gap the gap
         * @return this builder
         */
        public Builder gap(CoverageGap gap) {
            gaps.add(gap);
            return this;
        }

        /**
         * Sets the collection stats.
         *
         * @param stats the stats
         * @return this builder
         */
        public Builder stats(CollectionStats stats) {
            this.stats = stats;
            return this;
        }

        /**
         * Builds the inventory.
         *
         * @return the inventory
         */
        public EstateInventory build() {
            return new EstateInventory(scope, asOf, source, ports, connections, routers, networks, tokens, devices,
                    limits, gaps, stats);
        }
    }
}
