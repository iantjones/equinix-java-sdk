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

import com.eqixiac.equinix.Fabric;
import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.design.estate.EstateInventory.CollectionStats;
import com.eqixiac.equinix.design.estate.EstateInventory.ConnectionRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.CoverageGap;
import com.eqixiac.equinix.design.estate.EstateInventory.DeviceRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.Endpoint;
import com.eqixiac.equinix.design.estate.EstateInventory.NetworkRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.PortRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.RouterRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.ServiceTokenRecord;
import com.eqixiac.equinix.design.estate.EstateInventory.TypeStats;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.CloudRouterPackage;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.Network;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.ServiceToken;
import com.eqixiac.equinix.fabric.model.implementation.AccessPoint;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.ConnectionSide;
import com.eqixiac.equinix.fabric.model.implementation.filter.Filter;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.networkedge.model.Device;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reads a scoped estate snapshot from the live APIs. Read-only: it issues {@code GET}s and
 * {@code POST .../search} requests and nothing else.
 *
 * <p>Request shapes (verified live 2026-11-12 against one project, read-only):</p>
 * <ul>
 *   <li>Ports: {@code GET /fabric/v4/ports} returns every port the credentials can see in one
 *       response (794 for the test account), so the scope is applied client-side.
 *       {@code POST /ports/search} with a {@code /project/projectId} filter answered HTTP 400
 *       {@code EQ-3046106 "Invalid virtual port search request"}, so it is not used.</li>
 *   <li>Connections, Cloud Routers, networks and service tokens: paged {@code POST .../search}
 *       with {@code /project/projectId =} once per project, or {@code /uuid IN [...]} for explicit
 *       resources (verified live), then the scope is <b>always re-applied client-side</b>. Live
 *       2026-11-12: the connection search with {@code /project/projectId = <id>} returned 86 rows, none
 *       of which belonged to that project (14 other projects), and the same filter with {@code IN}
 *       returned one unrelated row; the router and network searches honoured the filter. The
 *       server filter only narrows the read; it is never trusted for scoping. Account-only scopes search without a
 *       server filter and keep matching rows.</li>
 *   <li>Router package limits: {@code GET /fabric/v4/routerPackages}; the live response had no
 *       {@code vcCountMax}, so the limit map is usually empty.</li>
 *   <li>Network Edge devices: the device API has no project, so only devices referenced by an
 *       in-scope connection (or listed explicitly in the scope) are read, one {@code GET} each.</li>
 * </ul>
 * <p>Paging stops at {@link EstateScope#maxResourcesPerType()} per type; a stop is recorded as a
 * {@code TRUNCATED} coverage gap and a warning. An HTTP error on one type (403, 5xx...) is recorded
 * as a coverage gap and the rest of the snapshot is still collected.</p>
 */
public final class EstateCollector {

    private final Fabric fabric;
    private final NetworkEdge networkEdge;
    private final EstateScope scope;
    private final Clock clock;
    private final Consumer<String> progress;
    private final List<CoverageGap> gaps = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Map<String, TypeStats> stats = new TreeMap<>();
    private int requests;

    /**
     * Creates a collector.
     *
     * @param fabric      the Fabric client
     * @param networkEdge the Network Edge client, or {@code null} to skip devices
     * @param scope       the scope (an unscoped scope must come from {@link EstateScope#entireAccount(int)})
     * @param clock       the clock for the as-of time
     * @param progress    receives one line per resource type, or {@code null}
     */
    public EstateCollector(Fabric fabric, NetworkEdge networkEdge, EstateScope scope, Clock clock,
                           Consumer<String> progress) {
        this.fabric = Objects.requireNonNull(fabric, "fabric");
        this.networkEdge = networkEdge;
        this.scope = Objects.requireNonNull(scope, "scope");
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.progress = progress == null ? s -> { } : progress;
        if (scope.isUnscoped() && !scope.entireAccount()) {
            throw new IllegalArgumentException("Refusing an unscoped estate read: name projects, accounts or "
                    + "resources, or use EstateScope.entireAccount(max) to read everything explicitly");
        }
    }

    /**
     * Collects the snapshot.
     *
     * @return the snapshot
     */
    public EstateInventory collect() {
        Instant asOf = clock.instant();
        long t0 = System.nanoTime();
        if (scope.isUnscoped()) {
            warnings.add("UNSCOPED RUN: reading everything the credentials can see, at most "
                    + scope.maxResourcesPerType() + " resources per type");
        }
        List<PortRecord> ports = timed("ports", this::ports, List.of());
        List<ConnectionRecord> connections = timed("connections", () -> searched("connections",
                f -> f == null ? fabric.connections().search() : fabric.connections().search(f),
                EstateCollector::connection, c -> admits(c.projectId(), c.accountNumber(), c.id())), List.of());
        List<RouterRecord> routers = timed("cloudRouters", () -> searched("cloudRouters",
                f -> f == null ? fabric.cloudRouters().search() : fabric.cloudRouters().search(f),
                EstateCollector::router, r -> admits(r.projectId(), r.accountNumber(), r.id())), List.of());
        List<NetworkRecord> networks = timed("networks", () -> searched("networks",
                f -> f == null ? fabric.networks().search() : fabric.networks().search(f),
                EstateCollector::network, n -> admits(n.projectId(), null, n.id())), List.of());
        List<ServiceTokenRecord> tokens = timed("serviceTokens", () -> searched("serviceTokens",
                f -> f == null ? fabric.serviceTokens().search() : fabric.serviceTokens().search(f),
                EstateCollector::token, t -> admits(t.projectId(), null, t.id())), List.of());
        Map<String, Integer> limits = timed("routerPackages", this::routerPackageLimits, Map.of());
        List<DeviceRecord> devices = networkEdge == null ? List.of()
                : timed("networkEdgeDevices", () -> devices(connections), List.of());
        if (networkEdge == null) {
            gaps.add(new CoverageGap("networkEdgeDevices", "NOT_REQUESTED", null,
                    "no Network Edge client was supplied; devices were not read"));
        }
        long total = (System.nanoTime() - t0) / 1_000_000;
        return new EstateInventory(scope, asOf, "live Fabric v4 API", ports, connections, routers, networks, tokens,
                devices, limits, gaps, new CollectionStats(requests, total, stats, warnings));
    }

    private boolean admits(String projectId, String accountNumber, String id) {
        return scope.admits(projectId, accountNumber, id);
    }

    private List<PortRecord> ports() {
        long t = System.nanoTime();
        requests++;
        List<Port> all = fabric.ports().list();
        List<PortRecord> out = new ArrayList<>();
        for (Port p : all) {
            PortRecord r = port(p);
            if (admits(r.projectId(), r.accountNumber(), r.id())) {
                out.add(r);
            }
        }
        boolean truncated = out.size() > scope.maxResourcesPerType();
        if (truncated) {
            truncate("ports", out.size());
            out = new ArrayList<>(out.subList(0, scope.maxResourcesPerType()));
        }
        stats.put("ports", new TypeStats(all.size(), out.size(), 1, (System.nanoTime() - t) / 1_000_000, truncated));
        return out;
    }

    private <M, R> List<R> searched(String type, Function<FilterPropertyList, PaginatedFilteredList<M>> search,
                                    Function<M, R> map, java.util.function.Predicate<R> keep) {
        long t = System.nanoTime();
        int pages = 0;
        int read = 0;
        boolean truncated = false;
        Map<Object, R> kept = new java.util.LinkedHashMap<>();
        for (FilterPropertyList filter : serverFilters()) {
            requests++;
            pages++;
            PaginatedFilteredList<M> page = search.apply(filter);
            while (page.hasNextPage()) {
                if (read + page.size() >= scope.maxResourcesPerType()) {
                    truncated = true;
                    break;
                }
                requests++;
                pages++;
                page.next();
            }
            read += page.size();
            for (M m : page) {
                R r = map.apply(m);
                if (keep.test(r)) {
                    kept.putIfAbsent(idOf(r), r);
                }
            }
            if (truncated) {
                break;
            }
        }
        if (truncated) {
            truncate(type, read);
        }
        List<R> out = new ArrayList<>(kept.values());
        stats.put(type, new TypeStats(read, out.size(), pages, (System.nanoTime() - t) / 1_000_000, truncated));
        return out;
    }

    private static Object idOf(Object r) {
        if (r instanceof ConnectionRecord c) {
            return c.id();
        }
        if (r instanceof RouterRecord c) {
            return c.id();
        }
        if (r instanceof NetworkRecord c) {
            return c.id();
        }
        if (r instanceof ServiceTokenRecord c) {
            return c.id();
        }
        return r;
    }

    /**
     * One search per project with {@code /project/projectId =}, or one {@code /uuid IN [...]} search
     * for explicit resources (verified live), or one unfiltered search. See the class comment: the
     * results are always re-scoped client-side.
     */
    private List<FilterPropertyList> serverFilters() {
        List<FilterPropertyList> out = new ArrayList<>();
        if (!scope.projectIds().isEmpty()) {
            for (String p : scope.projectIds()) {
                out.add(Filter.filter().and().equals("/project/projectId", p));
            }
            if (!scope.resourceIds().isEmpty()) {
                out.add(Filter.filter().and().in("/uuid", new ArrayList<>(scope.resourceIds())));
            }
            return out;
        }
        if (!scope.resourceIds().isEmpty() && scope.accountNumbers().isEmpty()) {
            out.add(Filter.filter().and().in("/uuid", new ArrayList<>(scope.resourceIds())));
            return out;
        }
        out.add(null);
        return out;
    }

    private Map<String, Integer> routerPackageLimits() {
        long t = System.nanoTime();
        requests++;
        Map<String, Integer> out = new TreeMap<>();
        int read = 0;
        for (CloudRouterPackage p : fabric.cloudRouters().routerPackages()) {
            read++;
            if (p.getCode() != null && p.getVcCountMax() != null) {
                out.put(p.getCode().name(), p.getVcCountMax());
            }
        }
        stats.put("routerPackages", new TypeStats(read, out.size(), 1, (System.nanoTime() - t) / 1_000_000, false));
        return out;
    }

    private List<DeviceRecord> devices(Collection<ConnectionRecord> connections) {
        long t = System.nanoTime();
        Set<String> ids = new TreeSet<>();
        for (ConnectionRecord c : connections) {
            if (c.aSide().deviceId() != null) {
                ids.add(c.aSide().deviceId());
            }
            if (c.zSide().deviceId() != null) {
                ids.add(c.zSide().deviceId());
            }
        }
        if (!scope.projectIds().isEmpty() || !scope.accountNumbers().isEmpty() || scope.isUnscoped()) {
            gaps.add(new CoverageGap("networkEdgeDevices", "NOT_SCOPABLE", null,
                    "the Network Edge device API has no project; only devices referenced by in-scope connections "
                            + "(or listed explicitly) were read"));
        }
        List<DeviceRecord> out = new ArrayList<>();
        for (String id : ids) {
            requests++;
            try {
                out.add(device(networkEdge.devices().getByUuid(id)));
            } catch (EquinixServiceException e) {
                gaps.add(new CoverageGap("networkEdgeDevices", reason(e), e.getStatusCode(),
                        "device " + id + " referenced by a connection could not be read"));
            }
        }
        stats.put("networkEdgeDevices", new TypeStats(ids.size(), out.size(), ids.size(),
                (System.nanoTime() - t) / 1_000_000, false));
        return out;
    }

    private void truncate(String type, int read) {
        gaps.add(new CoverageGap(type, "TRUNCATED", null, "stopped after " + read + " " + type
                + " (per-type ceiling " + scope.maxResourcesPerType() + "); narrow the scope or raise the ceiling"));
        warnings.add("TRUNCATED: " + type + " reached the per-type ceiling of " + scope.maxResourcesPerType()
                + "; the analysis is incomplete");
    }

    private <T> T timed(String type, Supplier<T> body, T empty) {
        long t = System.nanoTime();
        try {
            T result = body.get();
            TypeStats s = stats.get(type);
            progress.accept(type + ": " + (s == null ? "?" : s.inScope() + " in scope of " + s.read() + " read, "
                    + s.pages() + " request(s)") + ", " + (System.nanoTime() - t) / 1_000_000 + " ms");
            return result;
        } catch (EquinixServiceException e) {
            gaps.add(new CoverageGap(type, reason(e), e.getStatusCode(), "could not be read: " + firstLine(e)));
            progress.accept(type + ": not readable (" + reason(e) + ")");
            return empty;
        } catch (RuntimeException e) {
            gaps.add(new CoverageGap(type, "ERROR", null, "could not be read: " + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : " " + firstLine(e))));
            progress.accept(type + ": not readable (" + e.getClass().getSimpleName() + ")");
            return empty;
        }
    }

    private static String firstLine(Exception e) {
        String m = String.valueOf(e.getMessage());
        int nl = m.indexOf('\n');
        m = nl > 0 ? m.substring(0, nl) : m;
        return m.length() > 200 ? m.substring(0, 200) : m;
    }

    static String reason(EquinixServiceException e) {
        Integer s = e.getStatusCode();
        if (s == null) {
            return "ERROR";
        }
        if (s == 401 || s == 403) {
            return "FORBIDDEN";
        }
        if (s == 404) {
            return "NOT_FOUND";
        }
        if (s == 429) {
            return "RATE_LIMITED";
        }
        return s >= 500 ? "SERVER_ERROR" : "CLIENT_ERROR";
    }

    // ---- mapping (null-safe; unknown stays null) ----

    static PortRecord port(Port p) {
        Integer count = p.getOperation() != null && p.getOperation().getConnectionCount() != null
                ? Integer.valueOf(p.getOperation().getConnectionCount().intValue()) : p.getConnectionsCount();
        Integer physical = p.getPhysicalPortsCount() != null ? p.getPhysicalPortsCount()
                : p.getPhysicalPorts() != null && !p.getPhysicalPorts().isEmpty() ? p.getPhysicalPorts().size() : null;
        String project = p.getProject() != null ? p.getProject().getProjectId() : p.getProjectId();
        return new PortRecord(p.getUuid(), p.getName(), metro(p.getLocation() == null ? null : p.getLocation().getMetroCode()),
                name(p.getState()), p.getBandwidth(), p.getUsedBandwidth(), p.getAvailableBandwidth(),
                p.getEncapsulation() == null ? null : name(p.getEncapsulation().getType()), p.getLagEnabled(), physical,
                p.getRedundancy() == null ? null : name(p.getRedundancy().getPriority()),
                p.getDevice() == null || p.getDevice().getRedundancy() == null ? null
                        : p.getDevice().getRedundancy().getGroup(),
                p.getDevice() == null ? null : p.getDevice().getName(), count, project,
                p.getAccount() == null || p.getAccount().getAccountNumber() == null ? null
                        : String.valueOf(p.getAccount().getAccountNumber()),
                created(p.getChangeLog()), p.getNotifications() == null ? null : p.getNotifications().size());
    }

    static ConnectionRecord connection(Connection c) {
        return new ConnectionRecord(c.getUuid(), c.getName(), name(c.getType()), name(c.getState()),
                c.getOperation() == null ? null : name(c.getOperation().getEquinixStatus()),
                c.getOperation() == null ? null : name(c.getOperation().getProviderStatus()), c.getBandwidth(),
                endpoint(c.getASide()), endpoint(c.getZSide()),
                c.getOrder() == null ? null : c.getOrder().getTermLength(),
                c.getRedundancy() == null ? null : c.getRedundancy().getGroup(),
                c.getRedundancy() == null ? null : name(c.getRedundancy().getPriority()), created(c.getChangeLog()),
                c.getProject() == null ? null : c.getProject().getProjectId(),
                c.getAccount() == null || c.getAccount().getAccountNumber() == null ? null
                        : String.valueOf(c.getAccount().getAccountNumber()),
                c.getOrder() == null ? null : c.getOrder().getPurchaseOrderNumber(),
                c.getNotifications() == null ? null : c.getNotifications().size());
    }

    static Endpoint endpoint(ConnectionSide side) {
        if (side == null) {
            return Endpoint.unknown();
        }
        AccessPoint ap = side.getAccessPoint();
        String token = side.getServiceToken() == null ? null : side.getServiceToken().getUuid();
        if (ap == null) {
            return new Endpoint(token == null ? null : "SERVICE_TOKEN", null, null, null, null, null, null, null, token);
        }
        return new Endpoint(name(ap.getType()), metro(ap.getLocation() == null ? null : ap.getLocation().getMetroCode()),
                ap.getPort() == null ? null : ap.getPort().getUuid(),
                ap.getRouter() == null ? null : ap.getRouter().getUuid(),
                ap.getVirtualDevice() == null ? null : ap.getVirtualDevice().getUuid(),
                ap.getNetwork() == null ? null : ap.getNetwork().getUuid(),
                ap.getProfile() == null ? null : ap.getProfile().getUuid(),
                ap.getProfile() == null ? null : ap.getProfile().getName(), token);
    }

    static RouterRecord router(CloudRouter r) {
        return new RouterRecord(r.getUuid(), r.getName(),
                r.getRouterPackage() == null ? null : name(r.getRouterPackage().getCode()),
                metro(r.getLocation() == null ? null : r.getLocation().getMetroCode()), name(r.getState()),
                r.getConnectionCount(), r.getOrder() == null ? null : r.getOrder().getTermLength(),
                created(r.getChangeLog()), r.getProject() == null ? null : r.getProject().getProjectId(),
                r.getAccount() == null || r.getAccount().getAccountNumber() == null ? null
                        : String.valueOf(r.getAccount().getAccountNumber()),
                r.getNotifications() == null ? null : r.getNotifications().size());
    }

    static NetworkRecord network(Network n) {
        return new NetworkRecord(n.getUuid(), n.getName(), name(n.getType()), name(n.getScope()), name(n.getState()),
                n.getConnectionsCount(), created(n.getChangeLog()),
                n.getProject() == null ? null : n.getProject().getProjectId(),
                n.getNotifications() == null ? null : n.getNotifications().size());
    }

    static ServiceTokenRecord token(ServiceToken t) {
        return new ServiceTokenRecord(t.getUuid(), t.getName(), name(t.getType()), name(t.getState()),
                instant(t.getExpirationDateTime()), created(t.getChangeLog()),
                t.getProject() == null ? null : t.getProject().getProjectId());
    }

    static DeviceRecord device(Device d) {
        return new DeviceRecord(d.getUuid(), d.getName(), d.getDeviceTypeCode(), metro(d.getMetroCode()),
                name(d.getStatus()), name(d.getRedundancyType()), d.getRedundantUuid(), d.getTermLength(),
                instant(d.getCreatedDate()), d.getAccountNumber());
    }

    private static String metro(MetroCode m) {
        return m == null || m == MetroCode.UNKNOWN ? null : m.name();
    }

    private static String name(Enum<?> e) {
        return e == null ? null : e.name();
    }

    private static Instant created(ChangeLog log) {
        return log == null ? null : instant(log.getCreatedDateTime());
    }

    private static Instant instant(LocalDateTime t) {
        // The SDK normalises wire timestamps to UTC (see CHANGELOG "Timestamps").
        return t == null ? null : t.toInstant(ZoneOffset.UTC);
    }
}
