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
import com.eqixiac.equinix.core.waiter.ResourceWaiter;
import com.eqixiac.equinix.core.waiter.WaiterFailedException;
import com.eqixiac.equinix.design.optimizer.wizard.PlanValidator;
import com.eqixiac.equinix.design.optimizer.wizard.model.ConnectionBodies;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentOutcome;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.ExecutionInputs;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedBackboneLink;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedCloudRouter;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedMulticloudInterconnect;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedRoutingProtocol;
import com.eqixiac.equinix.design.optimizer.wizard.model.ProvisionedResource;
import com.eqixiac.equinix.design.optimizer.wizard.model.ProvisioningError;
import com.eqixiac.equinix.design.optimizer.wizard.model.RouterBodies;
import com.eqixiac.equinix.fabric.enums.CloudRouterState;
import com.eqixiac.equinix.fabric.enums.ConnectionState;
import com.eqixiac.equinix.fabric.enums.GatewayPackageCode;
import com.eqixiac.equinix.fabric.enums.NotificationType;
import com.eqixiac.equinix.fabric.enums.PeeringType;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolState;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;
import com.eqixiac.equinix.fabric.model.CloudRouter;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.RoutingProtocol;
import com.eqixiac.equinix.fabric.model.implementation.Notification;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.eqixiac.equinix.fabric.model.json.creators.ConnectionOperator;
import com.eqixiac.equinix.core.http.request.PatchOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Executes and reconciles deployment plans. Every path that creates Fabric resources goes through
 * this class, so one failure policy and one checkpoint discipline apply to all of them.
 *
 * <h2>Order and pre-flight</h2>
 * <p>Steps run in {@link PlanSteps} order: Cloud Routers, provider connections (PRIMARY before
 * SECONDARY), backbone links, routing protocols (DIRECT before BGP). Before anything is created, every
 * connection that will be created must have its cloud authorization key (from the plan or the
 * {@link ExecutionInputs}); otherwise nothing is sent. Each connection is dry-run with its complete body
 * (real A-side uuid, customer key, VLAN, marker) immediately before its real create: a rejection
 * (HTTP 4xx validation) is a step failure; an infeasibility (401/403/429/5xx/transport, classified by
 * {@link PlanValidator#classifyLiveFailure}) is recorded as a recoverable warning and the create
 * proceeds.</p>
 *
 * <h2>Failure policy</h2>
 * <p>A step fails when its create (or pre-flight) is rejected, when the resource lands in a terminal
 * failure state, when it does not reach {@code PROVISIONED} within {@code provisionTimeout}, or when it
 * cannot be attempted at all (a hand-built router with no package code). Every kind of step follows
 * {@code ExecutionOptions.getFailurePolicy()}: {@link FailurePolicy#ABORT_AND_ROLLBACK} (default) stops
 * and removes what this run created via {@link PlanRollback}; {@link FailurePolicy#CONTINUE_ON_ERROR}
 * records the failure, skips the steps that depend on it (each reported {@code SKIPPED} with the
 * reason) and carries on.</p>
 *
 * <h2>Checkpoints and resume</h2>
 * <p>After every step the deployment's {@link Checkpoint} is saved to the {@link CheckpointStore}. A
 * create is recorded the moment the API accepts it, before the state wait, so a run that dies while
 * waiting resumes by waiting again instead of creating a duplicate. {@link #reconcile} also reads live
 * state first: a resource whose create was accepted but never recorded (the process died between the
 * response and the checkpoint write) is found by its marker or name and adopted. Re-running a completed
 * plan creates nothing.</p>
 */
public final class PlanExecutor {

    private static final Set<ConnectionState> CONNECTION_TERMINAL = Set.of(
            ConnectionState.FAILED, ConnectionState.CANCELLED, ConnectionState.DEPROVISIONED,
            ConnectionState.DEPROVISIONING);

    private static final Set<CloudRouterState> ROUTER_TERMINAL = Set.of(
            CloudRouterState.NOT_PROVISIONED, CloudRouterState.DEPROVISIONED, CloudRouterState.DEPROVISIONING);

    private static final Set<RoutingProtocolState> PROTOCOL_TERMINAL = Set.of(
            RoutingProtocolState.FAILED, RoutingProtocolState.DEPROVISIONED, RoutingProtocolState.DEPROVISIONING);

    private final DeploymentPlan plan;
    private final FabricGateway fabric;
    private final ExecutionInputs inputs;
    private final ExecutionOptions options;
    private final CheckpointStore store;
    private final PlanDiff diff;
    private final String planId;

    private Checkpoint checkpoint;
    private final Map<String, String> uuidByKey = new HashMap<>();
    private final Map<String, String> liveGroupByPlannedGroup = new HashMap<>();
    private final Set<String> dryRunPending = new HashSet<>();
    private final List<CompletedStep> createdThisRun = new ArrayList<>();
    private final List<ProvisionedResource> resources = new ArrayList<>();
    private final List<ProvisioningError> errors = new ArrayList<>();
    private final List<StepResult> results = new ArrayList<>();
    private boolean failed;

    private PlanExecutor(DeploymentPlan plan, ExecutionInputs inputs, ExecutionOptions options,
                         CheckpointStore store, PlanDiff diff) {
        this.plan = plan;
        this.fabric = plan.getFabric();
        this.inputs = inputs != null ? inputs : ExecutionInputs.none();
        this.options = options != null ? options : ExecutionOptions.defaults();
        this.store = store != null ? store : new InMemoryCheckpointStore();
        this.diff = diff;
        this.planId = plan.planId();
    }

    /**
     * Executes a plan from scratch: no live-state read, a fresh in-memory checkpoint. Use
     * {@link #reconcile} to make a run idempotent and resumable.
     *
     * @param plan    a valid plan with a Fabric client bound
     * @param inputs  the customer inputs; {@code null} means none
     * @param options execution options; {@code null} means defaults
     * @return the outcome
     * @throws IllegalStateException when the plan is invalid or has no Fabric client
     */
    public static DeploymentOutcome execute(DeploymentPlan plan, ExecutionInputs inputs, ExecutionOptions options) {
        requireExecutable(plan);
        return new PlanExecutor(plan, inputs, options, new InMemoryCheckpointStore(), null).run();
    }

    /**
     * Brings live state to the plan: reads live state, diffs it against the plan and the deployment's
     * checkpoint, adopts what exists, creates only what is missing (in dependency order, checkpointing
     * after every step) and, when {@code options.isApplyUpdates()}, patches the patchable fields of
     * resources that differ. A re-run of a completed plan creates nothing.
     *
     * @param plan    a valid plan with a Fabric client bound
     * @param inputs  the customer inputs; {@code null} means none
     * @param options execution options; {@code null} means defaults
     * @param store   where checkpoints are kept; {@code null} means a fresh in-memory store
     * @return the outcome, including the step-by-step results
     * @throws IllegalStateException when the plan is invalid or has no Fabric client
     */
    public static DeploymentOutcome reconcile(DeploymentPlan plan, ExecutionInputs inputs, ExecutionOptions options,
                                              CheckpointStore store) {
        requireExecutable(plan);
        CheckpointStore s = store != null ? store : new InMemoryCheckpointStore();
        Checkpoint cp = s.load(plan.effectiveDeploymentName()).orElse(null);
        PlanDiff diff = DriftDetector.diff(plan, LiveStateReader.read(plan.getFabric(), plan, cp), cp);
        return new PlanExecutor(plan, inputs, options, s, diff).run();
    }

    /**
     * Rolls back the resources an earlier outcome created, using the plan to learn which connections
     * attach to which Cloud Router.
     *
     * @param plan    the plan the outcome came from
     * @param created the outcome's resources, in creation order
     * @param options timeouts, poll interval and clock; {@code null} means defaults
     * @return what was removed and what was left
     */
    public static RollbackReport rollback(DeploymentPlan plan, List<ProvisionedResource> created,
                                          ExecutionOptions options) {
        List<CompletedStep> steps = new ArrayList<>();
        boolean keyed = true;
        for (ProvisionedResource r : created == null ? List.<ProvisionedResource>of() : created) {
            if (r.getUuid() == null) {
                continue;
            }
            keyed &= r.getKey() != null;
            steps.add(new CompletedStep(r.getKey() != null ? r.getKey() : r.getResourceType() + "/" + r.getName(),
                    kindOf(r.getResourceType()), r.getName(), r.getUuid(), r.getParentUuid(), r.getStatus(), true));
        }
        return PlanRollback.rollback(plan.getFabric(), steps, keyed ? attachments(plan) : null, options);
    }

    private static void requireExecutable(DeploymentPlan plan) {
        if (!plan.isValid()) {
            String detail = plan.getValidationErrors() == null || plan.getValidationErrors().isEmpty()
                    ? "(no error detail was recorded on the plan)"
                    : String.join("; ", plan.getValidationErrors());
            throw new IllegalStateException("This deployment plan failed validation and cannot be "
                    + "executed - executing it would provision billable resources that are known to "
                    + "fail. Fix the following and re-plan: " + detail);
        }
        if (plan.getFabric() == null && plan.totalResourceCount() > 0) {
            throw new IllegalStateException("The plan has no Fabric client: bind one with plan.withFabric(fabric) "
                    + "(a loaded plan carries none).");
        }
    }

    //  The run

    private DeploymentOutcome run() {
        long start = System.currentTimeMillis();
        checkpoint = store.load(plan.effectiveDeploymentName())
                .orElse(Checkpoint.empty(planId, plan.effectiveDeploymentName()));
        List<PlanStep> steps = PlanSteps.of(plan);
        Map<String, PlanStep> byKey = new LinkedHashMap<>();
        steps.forEach(s -> byKey.put(s.key(), s));

        // What already exists: the checkpoint, corrected and extended by the live diff when there is one.
        Map<String, StepAction> preexisting = new HashMap<>();
        for (CompletedStep done : checkpoint.steps().values()) {
            if (!byKey.containsKey(done.key())) {
                continue;
            }
            DriftStatus live = diff == null ? null : diff.drift(done.key()).map(ResourceDrift::status).orElse(null);
            if (live == DriftStatus.MISSING) {
                continue; // recorded, but gone since: it will be created again
            }
            uuidByKey.put(done.key(), done.uuid());
            preexisting.put(done.key(), StepAction.ALREADY_DONE);
        }
        if (diff != null) {
            for (ResourceDrift d : diff.resources()) {
                if (d.liveUuid() != null && !uuidByKey.containsKey(d.key())
                        && (d.status() == DriftStatus.PRESENT_MATCHING || d.status() == DriftStatus.PRESENT_DIFFERENT)) {
                    uuidByKey.put(d.key(), d.liveUuid());
                    preexisting.put(d.key(), StepAction.ADOPTED);
                    if (!options.isDryRun()) {
                        PlanStep step = byKey.get(d.key());
                        save(new CompletedStep(d.key(), step.kind(), step.name(), d.liveUuid(),
                                parentUuidOf(step), "ADOPTED", false));
                    }
                }
            }
        }

        List<ProvisioningError> missing = missingRequiredInputs();
        if (!missing.isEmpty()) {
            errors.addAll(missing);
            return outcome(start, null);
        }

        for (PlanStep step : steps) {
            // A dry run creates nothing, so it never aborts: every step is attempted and reported.
            if (failed && options.getFailurePolicy() == FailurePolicy.ABORT_AND_ROLLBACK && !options.isDryRun()) {
                break;
            }
            if (!step.executable()) {
                results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.NOT_EXECUTABLE, null,
                        step.notExecutableReason()));
                continue;
            }
            if (preexisting.containsKey(step.key())) {
                handleExisting(step, preexisting.get(step.key()));
                continue;
            }
            String blocked = blockedBy(step);
            if (blocked != null) {
                if (options.isDryRun() && dryRunPending.contains(blocked)) {
                    dryRunPending.add(step.key());
                    results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.DRY_RUN_DEFERRED, null,
                            "depends on " + blocked + ", which does not exist yet; its live dry-run runs when that exists"));
                } else {
                    skip(step, "depends on " + blocked + ", which was not provisioned");
                }
                continue;
            }
            switch (step.kind()) {
                case CLOUD_ROUTER -> createRouter(step, router(step.key()));
                case PROVIDER_CONNECTION, BACKBONE_LINK -> createConnection(step, connection(step.key()));
                case ROUTING_PROTOCOL -> createProtocol(step, protocol(step.key()));
                default -> results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.NOT_EXECUTABLE,
                        null, "not an executable kind"));
            }
        }

        RollbackReport rollback = null;
        if (failed && options.getFailurePolicy() == FailurePolicy.ABORT_AND_ROLLBACK && !options.isDryRun()) {
            rollback = PlanRollback.rollback(fabric, createdThisRun, attachments(plan), options);
            for (RemovedResource removed : rollback.removed()) {
                checkpoint = checkpoint.without(removed.key(), options.getClock().instant());
            }
            store.save(checkpoint);
            for (OrphanedResource orphan : rollback.orphans()) {
                errors.add(ProvisioningError.builder()
                        .resourceType(typeOf(orphan.kind())).resourceName(orphan.name())
                        .reason("ORPHANED after rollback (uuid " + orphan.uuid() + "): " + orphan.reason())
                        .recoverable(false).build());
            }
        }
        return outcome(start, rollback);
    }

    private DeploymentOutcome outcome(long start, RollbackReport rollback) {
        return DeploymentOutcome.builder()
                .plan(plan)
                .planId(planId)
                .dryRun(options.isDryRun())
                .resources(List.copyOf(resources))
                .steps(List.copyOf(results))
                .fullySuccessful(!failed && errors.isEmpty())
                .errors(List.copyOf(errors))
                .rollback(rollback)
                .informational(multicloudInformational())
                .executionTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    //  Existing resources: adopt, report drift, optionally patch

    private void handleExisting(PlanStep step, StepAction how) {
        String uuid = uuidByKey.get(step.key());
        ResourceDrift drift = diff == null ? null : diff.drift(step.key()).orElse(null);
        if (drift == null || drift.status() != DriftStatus.PRESENT_DIFFERENT) {
            results.add(new StepResult(step.key(), step.kind(), step.name(), how, uuid,
                    how == StepAction.ADOPTED ? "found live by " + (drift != null ? drift.matchedBy() : "lookup") : null));
            return;
        }
        List<FieldDifference> patchable = drift.differences().stream().filter(FieldDifference::patchable).toList();
        List<FieldDifference> reportOnly = drift.differences().stream().filter(d -> !d.patchable()).toList();
        String reportNote = reportOnly.isEmpty() ? "" : " Not patchable (reported, not forced): "
                + reportOnly.stream().map(FieldDifference::field).toList() + ".";
        if (!options.isApplyUpdates() || options.isDryRun() || patchable.isEmpty()) {
            results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.DRIFT_REPORTED, uuid,
                    "differs in " + drift.differences().stream().map(FieldDifference::field).toList()
                            + (options.isApplyUpdates() ? "" : "; updates not enabled") + "." + reportNote));
            return;
        }
        try {
            if (step.kind() == ResourceKind.CLOUD_ROUTER) {
                patchRouter(uuid, router(step.key()), patchable);
            } else {
                patchConnection(uuid, connection(step.key()), patchable);
            }
            results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.UPDATED, uuid,
                    "patched " + patchable.stream().map(FieldDifference::field).toList() + "." + reportNote));
        } catch (RuntimeException e) {
            fail(step, "update failed: " + e.getMessage(), true);
        }
    }

    private void patchRouter(String uuid, PlannedCloudRouter planned, List<FieldDifference> fields) {
        var updater = fabric.cloudRouters().getByUuid(uuid).update();
        for (FieldDifference d : fields) {
            switch (d.field()) {
                case "name" -> updater.name(planned.getName());
                case "packageCode" -> updater.changePackage(planned.getPackageCode());
                case "notificationEmails" -> updater.patch(PatchOperation.replace("/notifications",
                        List.of(new Notification(NotificationType.ALL, planned.getNotificationEmails()))));
                default -> { }
            }
        }
        updater.save();
    }

    private void patchConnection(String uuid, PlannedConnection planned, List<FieldDifference> fields) {
        var updater = fabric.connections().getByUuid(uuid).update();
        for (FieldDifference d : fields) {
            switch (d.field()) {
                case "name" -> updater.name(planned.getName());
                case "bandwidthMbps" -> updater.bandwidth(planned.getBandwidthMbps());
                case "notificationEmails" -> updater.patch(PatchOperation.replace("/notifications",
                        List.of(new Notification(NotificationType.ALL, planned.getNotificationEmails()))));
                default -> { }
            }
        }
        updater.save();
    }

    //  Creates

    private void createRouter(PlanStep step, PlannedCloudRouter planned) {
        GatewayPackageCode pkg = planned.getPackageCode();
        if (pkg == null || pkg == GatewayPackageCode.UNKNOWN) {
            fail(step, "missing or unknown package code (" + pkg + ") - nothing was sent to the API", false);
            return;
        }
        if (options.isDryRun()) {
            dryRun(step, () -> RouterBodies.routerBody(fabric.cloudRouters(), planned).dryRun().create());
            return;
        }
        CloudRouter cr;
        try {
            cr = RouterBodies.routerBody(fabric.cloudRouters(), planned).create();
        } catch (RuntimeException e) {
            fail(step, e.getMessage(), true);
            return;
        }
        accepted(step, cr.getUuid(), null, planned.getMetroId());
        Await wait = await(() -> fabric.cloudRouters().getByUuid(cr.getUuid()), CloudRouter::getState,
                CloudRouterState.PROVISIONED, ROUTER_TERMINAL);
        settle(step, cr.getUuid(), null, wait);
    }

    private void createConnection(PlanStep step, PlannedConnection planned) {
        boolean onPort = planned.getASidePortUuid() != null;
        String aSide = planned.getASideExistingRouterUuid() != null ? planned.getASideExistingRouterUuid()
                : uuidByKey.get(routerKeyByName(planned.getASideRouterName()));
        String zSide = planned.isBackboneLink() ? uuidByKey.get(routerKeyByName(planned.getZSideRouterName())) : null;
        if (!onPort && aSide == null) {
            skip(step, "A-side Cloud Router '" + planned.getASideRouterName() + "' was not provisioned");
            return;
        }
        if (planned.isBackboneLink() && zSide == null) {
            skip(step, "Z-side Cloud Router '" + planned.getZSideRouterName() + "' was not provisioned");
            return;
        }
        PlannedConnection resolved = withInputs(planned, inputs);
        String liveGroup = null;
        if (PlanSteps.isSecondary(resolved)) {
            liveGroup = liveGroupFor(resolved.getZSideRedundancyGroup());
            if (liveGroup == null && !options.isDryRun()) {
                fail(step, "the PRIMARY of redundancy group '" + resolved.getZSideRedundancyGroup()
                        + "' has no Fabric group id, so the SECONDARY cannot join it", false);
                return;
            }
        }
        final String group = liveGroup;
        Supplier<ConnectionOperator.ConnectionBuilder> body = () -> marked(step, planned.isBackboneLink()
                ? ConnectionBodies.backboneBody(fabric.connections(), resolved, aSide, zSide)
                : onPort ? ConnectionBodies.providerBodyOnPort(fabric.connections(), resolved, resolved.getASidePortUuid(), group)
                : ConnectionBodies.providerBody(fabric.connections(), resolved, aSide, group));

        if (options.isDryRun()) {
            dryRun(step, () -> body.get().dryRun().create());
            return;
        }
        // Pre-flight with the complete body.
        try {
            body.get().dryRun().create();
        } catch (RuntimeException e) {
            PlanValidator.LiveFailure failure = PlanValidator.classifyLiveFailure(e);
            if (!failure.isInfeasible()) {
                fail(step, "pre-flight dry-run rejected: " + e.getMessage(), true);
                return;
            }
            errors.add(ProvisioningError.builder().resourceType(typeOf(step.kind())).resourceName(step.name())
                    .reason("pre-flight dry-run skipped (" + failure.reason()
                            + ") - proceeding to create, which surfaces any true error")
                    .recoverable(true).build());
        }
        Connection conn;
        try {
            conn = body.get().create();
        } catch (RuntimeException e) {
            fail(step, e.getMessage(), true);
            return;
        }
        accepted(step, conn.getUuid(), null, planned.isBackboneLink() ? planned.getASideMetro() : planned.getASideMetro());
        if (PlanSteps.hasGroup(resolved) && !PlanSteps.isSecondary(resolved)
                && conn.getRedundancy() != null && conn.getRedundancy().getGroup() != null) {
            liveGroupByPlannedGroup.put(resolved.getZSideRedundancyGroup(), conn.getRedundancy().getGroup());
        }
        Await wait = await(() -> fabric.connections().getByUuid(conn.getUuid()), Connection::getState,
                ConnectionState.PROVISIONED, CONNECTION_TERMINAL);
        settle(step, conn.getUuid(), null, wait);
    }

    private void createProtocol(PlanStep step, PlannedRoutingProtocol planned) {
        String connectionUuid = uuidByKey.get(connectionKeyByName(planned.getConnectionName()));
        if (connectionUuid == null) {
            skip(step, "parent connection '" + planned.getConnectionName() + "' was not provisioned");
            return;
        }
        if (options.isDryRun()) {
            dryRunPending.add(step.key());
            results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.DRY_RUN_DEFERRED, null,
                    "the Fabric API offers no dry-run for routing protocols"));
            return;
        }
        RoutingProtocol rp;
        try {
            var builder = fabric.routingProtocols().define().ofType(planned.getType()).withName(planned.getName());
            if (planned.getType() == RoutingProtocolType.BGP) {
                builder.withCustomerAsn(planned.getCustomerAsn())
                        .withBGPIpv4(planned.getCustomerPeerIpv4(), planned.getEquinixPeerIpv4(), true)
                        .withBFD(planned.isBfdEnabled(), planned.getBfdInterval());
            } else if (planned.getType() == RoutingProtocolType.DIRECT) {
                builder.withDirectIpv4(planned.getEquinixIfaceIpv4());
            }
            rp = builder.create(connectionUuid);
        } catch (RuntimeException e) {
            fail(step, e.getMessage(), true);
            return;
        }
        accepted(step, rp.getUuid(), connectionUuid, null);
        Await wait = await(() -> fabric.routingProtocols().getByUuid(connectionUuid, rp.getUuid()),
                RoutingProtocol::getState, RoutingProtocolState.PROVISIONED, PROTOCOL_TERMINAL);
        settle(step, rp.getUuid(), connectionUuid, wait);
    }

    private ConnectionOperator.ConnectionBuilder marked(PlanStep step, ConnectionOperator.ConnectionBuilder builder) {
        PlanMarker.connectionMarker(plan.effectiveDeploymentName(), step.key(), planId).forEach(builder::additionalInfo);
        return builder;
    }

    private void dryRun(PlanStep step, Runnable call) {
        try {
            call.run();
            dryRunPending.add(step.key());
            results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.DRY_RUN_PASSED, null,
                    "the API accepted the dry-run request; nothing was created"));
        } catch (RuntimeException e) {
            PlanValidator.LiveFailure failure = PlanValidator.classifyLiveFailure(e);
            if (failure.isInfeasible()) {
                dryRunPending.add(step.key());
                results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.DRY_RUN_DEFERRED, null,
                        "dry-run could not be attempted: " + failure.reason()));
            } else {
                fail(step, "dry-run rejected: " + e.getMessage(), true);
            }
        }
    }

    /** Records an accepted create: audit entry, run list and checkpoint, before any state wait. */
    private void accepted(PlanStep step, String uuid, String parentUuid,
                          com.eqixiac.equinix.core.model.MetroId metro) {
        uuidByKey.put(step.key(), uuid);
        CompletedStep done = new CompletedStep(step.key(), step.kind(), step.name(), uuid, parentUuid,
                "CREATE_ACCEPTED", true);
        createdThisRun.add(done);
        save(done);
        resources.add(ProvisionedResource.builder()
                .resourceType(typeOf(step.kind())).key(step.key()).name(step.name()).uuid(uuid)
                .parentUuid(parentUuid).metroId(metro).status("PROVISIONING").build());
    }

    /** Applies the state-wait verdict: success updates the checkpoint, anything else fails the step. */
    private void settle(PlanStep step, String uuid, String parentUuid, Await wait) {
        String status = wait.statusOr("PROVISIONING");
        resources.replaceAll(r -> uuid.equals(r.getUuid()) ? r.toBuilder().status(status).build() : r);
        save(new CompletedStep(step.key(), step.kind(), step.name(), uuid, parentUuid, status, true));
        if (wait.ready) {
            results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.CREATED, uuid, null));
        } else if (wait.terminallyFailed) {
            fail(step, "reached terminal state " + wait.statusOr("(unobserved)")
                    + " and will never reach PROVISIONED", true);
        } else {
            fail(step, "did not reach PROVISIONED within " + options.getProvisionTimeout(), true);
        }
    }

    private void save(CompletedStep step) {
        checkpoint = checkpoint.with(step, options.getClock().instant());
        store.save(checkpoint);
    }

    private void fail(PlanStep step, String reason, boolean recoverable) {
        failed = true;
        // A failed resource is not a usable dependency, even when its create was accepted: its
        // dependents are skipped (CONTINUE_ON_ERROR) and rollback finds it in createdThisRun.
        String uuid = uuidByKey.remove(step.key());
        errors.add(ProvisioningError.builder().resourceType(typeOf(step.kind())).resourceName(step.name())
                .reason(reason).recoverable(recoverable).build());
        results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.FAILED, uuid, reason));
    }

    private void skip(PlanStep step, String reason) {
        errors.add(ProvisioningError.builder().resourceType(typeOf(step.kind())).resourceName(step.name())
                .reason(reason + " - skipped").recoverable(false).build());
        results.add(new StepResult(step.key(), step.kind(), step.name(), StepAction.SKIPPED, null, reason));
    }

    private String blockedBy(PlanStep step) {
        for (String dep : step.dependsOn()) {
            if (!uuidByKey.containsKey(dep)) {
                return dep;
            }
        }
        return null;
    }

    private String liveGroupFor(String plannedGroup) {
        String known = liveGroupByPlannedGroup.get(plannedGroup);
        if (known != null) {
            return known;
        }
        for (PlannedConnection c : nz(plan.getProviderConnections())) {
            if (plannedGroup.equals(c.getZSideRedundancyGroup()) && !PlanSteps.isSecondary(c)) {
                String uuid = uuidByKey.get(PlanSteps.keyOf(c));
                if (uuid != null) {
                    try {
                        Connection primary = fabric.connections().getByUuid(uuid);
                        if (primary.getRedundancy() != null && primary.getRedundancy().getGroup() != null) {
                            liveGroupByPlannedGroup.put(plannedGroup, primary.getRedundancy().getGroup());
                            return primary.getRedundancy().getGroup();
                        }
                    } catch (RuntimeException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    //  Inputs

    private List<ProvisioningError> missingRequiredInputs() {
        List<ProvisioningError> missing = new ArrayList<>();
        if (options.isDryRun()) {
            return missing;
        }
        for (PlannedConnection conn : nz(plan.getProviderConnections())) {
            if (uuidByKey.containsKey(PlanSteps.keyOf(conn))) {
                continue;
            }
            CloudProviderType type = conn.getZSideCloudType() != null ? conn.getZSideCloudType()
                    : ConnectionBodies.resolveCloudType(conn.getZSideProviderLabel());
            if (type == CloudProviderType.OTHER) {
                continue;
            }
            String key = conn.getZSideAuthenticationKey() != null ? conn.getZSideAuthenticationKey()
                    : inputs.authenticationKeyFor(conn.getName());
            if (key == null || key.isBlank()) {
                missing.add(ProvisioningError.builder().resourceType("Connection").resourceName(conn.getName())
                        .reason("missing required authorization key: " + ConnectionBodies.authenticationKeyLabel(type)
                                + " - supply it via ExecutionInputs before executing")
                        .recoverable(false).build());
            }
        }
        return missing;
    }

    /**
     * The planned connection with the customer inputs merged in; a value already on the plan wins.
     *
     * @param planned the planned connection
     * @param inputs  the customer inputs
     * @return a copy with authorization key, VLAN tag and peering type filled in where supplied
     */
    static PlannedConnection withInputs(PlannedConnection planned, ExecutionInputs inputs) {
        String key = planned.getZSideAuthenticationKey() != null ? planned.getZSideAuthenticationKey()
                : inputs.authenticationKeyFor(planned.getName());
        Integer vlan = planned.getZSideVlanTag() != null ? planned.getZSideVlanTag() : inputs.vlanTagFor(planned.getName());
        PeeringType peering = planned.getZSidePeeringType() != null ? planned.getZSidePeeringType()
                : inputs.peeringTypeFor(planned.getName());
        return planned.toBuilder().zSideAuthenticationKey(key).zSideVlanTag(vlan).zSidePeeringType(peering).build();
    }

    //  Lookups

    private PlannedCloudRouter router(String key) {
        return nz(plan.getCloudRouters()).stream().filter(r -> PlanSteps.keyOf(r).equals(key)).findFirst().orElseThrow();
    }

    private PlannedConnection connection(String key) {
        for (PlannedConnection c : nz(plan.getProviderConnections())) {
            if (PlanSteps.keyOf(c).equals(key)) {
                return c;
            }
        }
        for (PlannedBackboneLink l : nz(plan.getBackboneLinks())) {
            if (l.getConnection() != null && PlanSteps.keyOf(l.getConnection()).equals(key)) {
                return l.getConnection();
            }
        }
        throw new IllegalStateException("no connection with key " + key);
    }

    private PlannedRoutingProtocol protocol(String key) {
        return nz(plan.getRoutingProtocols()).stream().filter(p -> PlanSteps.keyOf(p).equals(key)).findFirst().orElseThrow();
    }

    private String routerKeyByName(String name) {
        for (PlannedCloudRouter r : nz(plan.getCloudRouters())) {
            if (r.getName().equals(name)) {
                return PlanSteps.keyOf(r);
            }
        }
        return null;
    }

    private String connectionKeyByName(String name) {
        for (PlannedConnection c : nz(plan.getProviderConnections())) {
            if (c.getName().equals(name)) {
                return PlanSteps.keyOf(c);
            }
        }
        for (PlannedBackboneLink l : nz(plan.getBackboneLinks())) {
            if (l.getConnection() != null && l.getConnection().getName().equals(name)) {
                return PlanSteps.keyOf(l.getConnection());
            }
        }
        return null;
    }

    private String parentUuidOf(PlanStep step) {
        if (step.kind() != ResourceKind.ROUTING_PROTOCOL) {
            return null;
        }
        return step.dependsOn().stream().filter(k -> !k.contains("/rp/")).map(uuidByKey::get)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    /** For each router key, the keys of the planned connections that attach to it. */
    static Map<String, List<String>> attachments(DeploymentPlan plan) {
        Map<String, List<String>> attached = new HashMap<>();
        Map<String, String> routerKeys = new HashMap<>();
        for (PlannedCloudRouter r : nz(plan.getCloudRouters())) {
            routerKeys.put(r.getName(), PlanSteps.keyOf(r));
        }
        List<PlannedConnection> all = new ArrayList<>(nz(plan.getProviderConnections()));
        nz(plan.getBackboneLinks()).forEach(l -> {
            if (l.getConnection() != null) {
                all.add(l.getConnection());
            }
        });
        for (PlannedConnection c : all) {
            for (String routerName : new String[]{c.getASideRouterName(), c.getZSideRouterName()}) {
                String rk = routerName == null ? null : routerKeys.get(routerName);
                if (rk != null) {
                    attached.computeIfAbsent(rk, k -> new ArrayList<>()).add(PlanSteps.keyOf(c));
                }
            }
        }
        return attached;
    }

    private List<ProvisioningError> multicloudInformational() {
        List<PlannedMulticloudInterconnect> links = plan.multicloudLinksOrEmpty();
        if (links.isEmpty()) {
            return null;
        }
        List<ProvisioningError> entries = new ArrayList<>();
        for (PlannedMulticloudInterconnect link : links) {
            String dependency = link.replacesEquinixConnections()
                    ? " The plan omits " + link.getReplacedConnectionNames() + " and depends on this link."
                    : " The plan does not depend on this link.";
            entries.add(ProvisioningError.builder()
                    .resourceType("MulticloudInterconnect")
                    .resourceName(link.getName())
                    .reason("created outside Fabric: follow the create-then-accept recipe with the two cloud "
                            + "providers (" + link.describe() + "). This SDK calls no cloud-provider API and "
                            + "sent no request for it." + dependency)
                    .recoverable(false)
                    .build());
        }
        return entries;
    }

    static String typeOf(ResourceKind kind) {
        return switch (kind) {
            case CLOUD_ROUTER -> "CloudRouter";
            case PROVIDER_CONNECTION -> "Connection";
            case BACKBONE_LINK -> "BackboneLink";
            case ROUTING_PROTOCOL -> "RoutingProtocol";
            case INTERCONNECT -> "Interconnect";
            case MULTICLOUD_LINK -> "MulticloudInterconnect";
            default -> "Unknown";
        };
    }

    static ResourceKind kindOf(String type) {
        if (type == null) {
            return ResourceKind.UNKNOWN;
        }
        return switch (type) {
            case "CloudRouter" -> ResourceKind.CLOUD_ROUTER;
            case "Connection" -> ResourceKind.PROVIDER_CONNECTION;
            case "BackboneLink" -> ResourceKind.BACKBONE_LINK;
            case "RoutingProtocol" -> ResourceKind.ROUTING_PROTOCOL;
            default -> ResourceKind.UNKNOWN;
        };
    }

    //  State waits

    private record Await(boolean ready, boolean terminallyFailed, String status) {
        String statusOr(String fallback) {
            return status != null ? status : fallback;
        }
    }

    private <T, S extends Enum<S>> Await await(Supplier<T> fetch, Function<T, S> state, S ready, Set<S> terminal) {
        try {
            T resource = ResourceWaiter.forResource(fetch)
                    .until(r -> state.apply(r) == ready)
                    .failWhen(r -> terminal.contains(state.apply(r)))
                    .timeout(options.getProvisionTimeout())
                    .pollInterval(options.getPollInterval())
                    .clock(options.getClock())
                    .await();
            return new Await(true, false, state.apply(resource).name());
        } catch (WaiterFailedException e) {
            String observed = null;
            try {
                @SuppressWarnings("unchecked")
                T failedResource = (T) e.getResource();
                if (failedResource != null && state.apply(failedResource) != null) {
                    observed = state.apply(failedResource).name();
                }
            } catch (RuntimeException ignored) {
                // A cast/accessor problem must not mask the terminal-failure verdict itself.
            }
            return new Await(false, true, observed);
        } catch (RuntimeException e) {
            return new Await(false, false, null);
        }
    }

    private static <T> List<T> nz(List<T> list) {
        return list == null ? List.of() : list;
    }
}
