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


package com.eqixiac.equinix.design.plan;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.core.exception.EquinixNotFoundException;
import com.eqixiac.equinix.core.waiter.ResourceWaiter;
import com.eqixiac.equinix.core.waiter.WaiterFailedException;
import com.eqixiac.equinix.core.waiter.WaiterTimeoutException;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.Connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Tears down resources a run created, in an order that cannot strand a billable Cloud Router.
 *
 * <ol>
 *   <li>Every connection (provider connections and backbone links) is deleted, newest first. A delete
 *       that answers HTTP 404 counts as already gone.</li>
 *   <li>Each connection delete is then polled (with the {@code core/waiter} machinery, on the
 *       options' clock and poll interval) until the connection reads {@code DEPROVISIONED} or HTTP 404,
 *       for at most {@code deleteTimeout}. Fabric deletes connections asynchronously; deleting the
 *       Cloud Router while a connection is still {@code DEPROVISIONING} fails or orphans the router.</li>
 *   <li>A Cloud Router is deleted only when every connection attached to it is confirmed gone, and its
 *       delete is polled the same way.</li>
 * </ol>
 * <p>Routing protocols are removed by Fabric with their connection: they are reported removed when
 * their connection is, and orphaned when it is not. Anything that could not be removed is an
 * {@link OrphanedResource} with its uuid and the reason.</p>
 */
public final class PlanRollback {

    private PlanRollback() {
    }

    /**
     * Rolls back the given resources.
     *
     * @param fabric   the Fabric client
     * @param created  the resources to remove, in creation order
     * @param attached for each router key, the keys of the connections attached to it; {@code null} when
     *                 unknown, in which case any orphaned connection keeps every router
     * @param options  timeouts, poll interval and clock
     * @return what was removed and what was left
     */
    public static RollbackReport rollback(FabricGateway fabric, List<CompletedStep> created,
                                          Map<String, List<String>> attached, ExecutionOptions options) {
        if (created == null || created.isEmpty()) {
            return RollbackReport.empty();
        }
        ExecutionOptions opts = options != null ? options : ExecutionOptions.defaults();
        List<CompletedStep> reversed = new ArrayList<>(created);
        Collections.reverse(reversed);
        List<RemovedResource> removed = new ArrayList<>();
        List<OrphanedResource> orphans = new ArrayList<>();
        Set<String> goneConnections = new HashSet<>();
        Set<String> orphanedConnections = new HashSet<>();

        // 1. Request every connection delete, newest first.
        List<CompletedStep> requested = new ArrayList<>();
        for (CompletedStep step : reversed) {
            if (!step.kind().isConnection() || step.uuid() == null) {
                continue;
            }
            try {
                fabric.connections().getByUuid(step.uuid()).delete();
                requested.add(step);
            } catch (EquinixNotFoundException e) {
                goneConnections.add(step.key());
                removed.add(new RemovedResource(step.key(), step.kind(), step.name(), step.uuid()));
            } catch (RuntimeException e) {
                orphanedConnections.add(step.key());
                orphans.add(new OrphanedResource(step.key(), step.kind(), step.name(), step.uuid(),
                        "connection delete request failed: " + e.getMessage()));
            }
        }

        // 2. Wait for each requested delete to finish.
        for (CompletedStep step : requested) {
            String failure = awaitGone(() -> {
                Connection c = fabric.connections().getByUuid(step.uuid());
                return c.getState() == null ? "UNKNOWN" : c.getState().name();
            }, Set.of("DEPROVISIONED"), Set.of("FAILED"), opts);
            if (failure == null) {
                goneConnections.add(step.key());
                removed.add(new RemovedResource(step.key(), step.kind(), step.name(), step.uuid()));
            } else {
                orphanedConnections.add(step.key());
                orphans.add(new OrphanedResource(step.key(), step.kind(), step.name(), step.uuid(), failure));
            }
        }

        // 3. Routing protocols follow their connection.
        Map<String, String> connectionKeyByUuid = new java.util.HashMap<>();
        for (CompletedStep step : created) {
            if (step.kind().isConnection()) {
                connectionKeyByUuid.put(step.uuid(), step.key());
            }
        }
        for (CompletedStep step : reversed) {
            if (step.kind() != ResourceKind.ROUTING_PROTOCOL) {
                continue;
            }
            String parentKey = connectionKeyByUuid.get(step.parentUuid());
            if (parentKey != null && goneConnections.contains(parentKey)) {
                removed.add(new RemovedResource(step.key(), step.kind(), step.name(), step.uuid()));
            } else {
                orphans.add(new OrphanedResource(step.key(), step.kind(), step.name(), step.uuid(),
                        parentKey == null
                                ? "its connection was not created by this run, so it was not removed"
                                : "its connection " + parentKey + " was not removed"));
            }
        }

        // 4. Routers, only once nothing attached remains.
        for (CompletedStep step : reversed) {
            if (step.kind() != ResourceKind.CLOUD_ROUTER || step.uuid() == null) {
                continue;
            }
            List<String> blocking = new ArrayList<>();
            if (attached == null) {
                blocking.addAll(orphanedConnections);
            } else {
                for (String conn : attached.getOrDefault(step.key(), List.of())) {
                    if (orphanedConnections.contains(conn)) {
                        blocking.add(conn);
                    }
                }
            }
            if (!blocking.isEmpty()) {
                orphans.add(new OrphanedResource(step.key(), step.kind(), step.name(), step.uuid(),
                        "not deleted: attached connection(s) " + blocking + " could not be removed"));
                continue;
            }
            try {
                fabric.cloudRouters().getByUuid(step.uuid()).delete();
            } catch (EquinixNotFoundException e) {
                removed.add(new RemovedResource(step.key(), step.kind(), step.name(), step.uuid()));
                continue;
            } catch (RuntimeException e) {
                orphans.add(new OrphanedResource(step.key(), step.kind(), step.name(), step.uuid(),
                        "router delete request failed: " + e.getMessage()));
                continue;
            }
            String failure = awaitGone(() -> {
                CloudRouter cr = fabric.cloudRouters().getByUuid(step.uuid());
                return cr.getState() == null ? "UNKNOWN" : cr.getState().name();
            }, Set.of("DEPROVISIONED"), Set.of("NOT_DEPROVISIONED"), opts);
            if (failure == null) {
                removed.add(new RemovedResource(step.key(), step.kind(), step.name(), step.uuid()));
            } else {
                orphans.add(new OrphanedResource(step.key(), step.kind(), step.name(), step.uuid(), failure));
            }
        }
        return new RollbackReport(removed, orphans);
    }

    /**
     * Polls a resource's state until it is gone (one of {@code goneStates} or HTTP 404).
     *
     * @return {@code null} when gone, otherwise why it is not confirmed gone
     */
    private static String awaitGone(Supplier<String> state, Set<String> goneStates, Set<String> failedStates,
                                    ExecutionOptions opts) {
        Supplier<String> fetch = () -> {
            try {
                return state.get();
            } catch (EquinixNotFoundException e) {
                return "GONE";
            }
        };
        try {
            ResourceWaiter.forResource(fetch)
                    .until(s -> "GONE".equals(s) || goneStates.contains(s))
                    .failWhen(failedStates::contains)
                    .timeout(opts.getDeleteTimeout())
                    .pollInterval(opts.getPollInterval())
                    .clock(opts.getClock())
                    .await();
            return null;
        } catch (WaiterFailedException e) {
            return "delete ended in state " + e.getResource() + " instead of DEPROVISIONED";
        } catch (WaiterTimeoutException e) {
            return "delete requested but not confirmed within " + opts.getDeleteTimeout() + " (last state "
                    + e.getLastObserved() + ")";
        } catch (RuntimeException e) {
            return "delete requested but its state could not be read: " + e.getMessage();
        }
    }
}
