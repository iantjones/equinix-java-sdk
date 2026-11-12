package com.eqixiac.equinix.design.optimizer.wizard.model;

import com.eqixiac.equinix.FabricGateway;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.PlanValidator;
import com.eqixiac.equinix.design.plan.CheckpointStore;
import com.eqixiac.equinix.design.plan.DriftDetector;
import com.eqixiac.equinix.design.plan.ExecutionOptions;
import com.eqixiac.equinix.design.plan.LiveStateReader;
import com.eqixiac.equinix.design.plan.PlanDiff;
import com.eqixiac.equinix.design.plan.PlanExecutor;
import com.eqixiac.equinix.design.plan.PlanJson;
import com.eqixiac.equinix.design.plan.RollbackReport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * A deployment plan produced by the Deployment Wizard: the Cloud Routers, provider connections,
 * backbone links and routing protocols to create, the native multicloud links and Fabric
 * Interconnects the design relies on but the SDK does not create, pricing, and the layered
 * validation state.
 *
 * <h2>Plan as data</h2>
 * <p>A plan is an immutable value. Every planned resource carries a stable logical key derived from
 * the design (for example {@code router/DC}, {@code connection/DC/aws}, {@code backbone/DC-NY},
 * {@code connection/DC/aws/rp/BGP}), so the same design produces the same keys whatever the name
 * prefix. The plan records the {@code DesignSpec} fingerprint it was derived from
 * ({@code getSpecFingerprint()}), and {@link #planId()} is a hash of what it would build. It
 * round-trips through JSON ({@link #toJson()}, {@link #fromJson(String)}, {@link #save(Path)},
 * {@link #load(Path)}); a loaded plan has no Fabric client until {@link #withFabric} binds one.</p>
 *
 * <h2>Lifecycle</h2>
 * <pre>{@code
 * DeploymentPlan plan = fabric.deploymentWizard(result).projectId(project).notifications("noc@example.com").plan();
 * plan.save(Path.of("plan.json"));
 *
 * PlanDiff drift = plan.diff();                                       // read-only
 * DeploymentOutcome out = plan.reconcile(inputs, ExecutionOptions.defaults(),
 *         new FileCheckpointStore(Path.of(".checkpoints")));          // creates only what is missing
 * }</pre>
 * <p>{@link #execute(ExecutionInputs, ExecutionOptions)} provisions the whole plan in one run;
 * {@link #reconcile} reads live state first, adopts what exists, creates the rest and checkpoints
 * after every step, so it can be re-run after a crash or abort without creating duplicates. Both
 * follow one failure policy ({@code ExecutionOptions.failurePolicy}): abort and roll back by default,
 * or continue and report partial state. See {@link PlanExecutor}.</p>
 *
 * <p><b>Beta.</b> {@code multicloudLinks} are native provider-to-provider links between two clouds,
 * which use no Equinix resource. The plan carries and prices them; execution never provisions them,
 * {@link #totalResourceCount()} excludes them, and their cost is never part of the Equinix totals.</p>
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonIgnoreProperties(value = {"planId", "schemaVersion"}, allowGetters = true)
public class DeploymentPlan {

    /**
     * The name that identifies this deployment across edits of the plan: checkpoints are keyed by it
     * and every connection the plan creates carries it as a marker ({@code PlanMarker}). Defaults to
     * the design spec's name, else the router name prefix.
     */
    String deploymentName;

    /** The prefix every generated resource name starts with ({@code DeploymentWizard.Builder.routerNamePrefix}). */
    String namePrefix;

    /** The Equinix project the plan's resources are created in, or {@code null} for the default project. */
    String projectId;

    /** The {@code DesignSpec.fingerprint()} the plan was derived from, or {@code null} when not supplied. */
    String specFingerprint;

    /** When the source optimization was computed, or {@code null}. */
    Instant sourceComputedAt;

    /**
     * The optimization the plan was generated from. In memory only: it is not written to the plan
     * document, so a loaded plan's {@link #dryRun()} skips the checks that need the optimizer's
     * recommendations.
     */
    @JsonIgnore
    OptimizationResult sourceOptimization;

    List<PlannedCloudRouter> cloudRouters;

    List<PlannedConnection> providerConnections;

    List<PlannedBackboneLink> backboneLinks;

    List<PlannedRoutingProtocol> routingProtocols;

    /**
     * <b>Beta.</b> The native multicloud links planned for the cloud-to-cloud flows of this
     * deployment; {@code null} or empty when there is none. Read through
     * {@link #multicloudLinksOrEmpty()} to avoid the {@code null} case.
     */
    List<PlannedMulticloudInterconnect> multicloudLinks;

    /**
     * Fabric Interconnects the deployment includes. Planned and rendered, priced as unknown, and never
     * executable through the public API; see {@link PlannedInterconnect}. {@code null} or empty when none.
     */
    List<PlannedInterconnect> interconnects;

    PlanPricing pricing;

    boolean valid;

    List<String> validationErrors;

    /**
     * Validations that could NOT be completed at plan time and will run at provisioning - chiefly the
     * live connection endpoint dry-run, which needs the target Cloud Router (which does not exist yet)
     * and the customer's cloud authorization keys. These are <em>not</em> errors.
     */
    List<String> deferredValidations;

    /**
     * Validations that could NOT be attempted at plan time (offline, 401/403/429/5xx, transport). Each
     * entry is a reason. These are <em>not</em> errors: a structurally-fine plan stays {@link #valid}.
     */
    List<String> skippedValidations;

    /**
     * The per-connection authorization a customer must gather before each provider connection can be
     * provisioned (the cloud-specific key, the VLAN tag, and - for Azure - the peering type).
     */
    List<ConnectionInputRequirement> requiredInputs;

    @JsonIgnore
    FabricGateway fabric;

    //  Data

    /**
     * The plan's content identity: a hash of what it would build (resources, Interconnects, native
     * links, deployment name, prefix, project, spec fingerprint), not of pricing or validation state.
     *
     * @return 16 lowercase hex characters
     * @see PlanJson#planId(DeploymentPlan)
     */
    public String planId() {
        return PlanJson.planId(this);
    }

    /**
     * The plan document format version.
     *
     * @return {@value PlanJson#SCHEMA_VERSION}
     */
    public String getSchemaVersion() {
        return PlanJson.SCHEMA_VERSION;
    }

    /**
     * The id as a bean property, so it is written into the plan document.
     *
     * @return {@link #planId()}
     */
    public String getPlanId() {
        return planId();
    }

    /**
     * The deployment name, or {@code plan-<planId>} for a hand-built plan that has none. Checkpoints
     * and connection markers use this value.
     *
     * @return a non-null deployment identity
     */
    public String effectiveDeploymentName() {
        return deploymentName != null && !deploymentName.isBlank() ? deploymentName : "plan-" + planId();
    }

    /**
     * The native multicloud links on the plan, never {@code null}.
     *
     * @return {@code getMulticloudLinks()}, or an empty list when that is {@code null}
     */
    public List<PlannedMulticloudInterconnect> multicloudLinksOrEmpty() {
        return multicloudLinks == null ? Collections.emptyList() : multicloudLinks;
    }

    /**
     * The Fabric Interconnects on the plan, never {@code null}.
     *
     * @return {@code getInterconnects()}, or an empty list when that is {@code null}
     */
    public List<PlannedInterconnect> interconnectsOrEmpty() {
        return interconnects == null ? Collections.emptyList() : interconnects;
    }

    /**
     * Total number of resources execution creates: Cloud Routers, provider connections, backbone links
     * and routing protocols. Native multicloud links and Interconnects are excluded.
     *
     * @return the count
     */
    public int totalResourceCount() {
        int count = 0;
        if (cloudRouters != null) count += cloudRouters.size();
        if (providerConnections != null) count += providerConnections.size();
        if (backboneLinks != null) count += backboneLinks.size();
        if (routingProtocols != null) count += routingProtocols.size();
        return count;
    }

    /**
     * A concise plain-text summary.
     *
     * @return the summary
     */
    public String toSummary() {
        return PlanMarkdown.summary(this);
    }

    /**
     * A full Markdown report: identity, validation state, required inputs, every planned resource,
     * native links, Interconnects, cost estimate and execution order.
     *
     * @return Markdown
     */
    public String toMarkdown() {
        return PlanMarkdown.markdown(this);
    }

    /**
     * This plan as an indented JSON document, including {@code planId} and {@code schemaVersion}.
     *
     * @return JSON
     */
    public String toJson() {
        return PlanJson.toJson(this);
    }

    /**
     * Parses a plan document written by {@link #toJson()}.
     *
     * @param json the document
     * @return the plan, with no Fabric client bound
     * @throws IllegalArgumentException when unreadable or when its {@code planId} does not match its content
     */
    public static DeploymentPlan fromJson(String json) {
        return PlanJson.fromJson(json);
    }

    /**
     * Writes this plan to a file (atomically).
     *
     * @param file the destination
     */
    public void save(Path file) {
        PlanJson.save(this, file);
    }

    /**
     * Reads a plan from a file written by {@link #save(Path)}.
     *
     * @param file the file
     * @return the plan, with no Fabric client bound
     */
    public static DeploymentPlan load(Path file) {
        return PlanJson.load(file);
    }

    /**
     * This plan with a Fabric client bound (needed after {@link #load} before any live call).
     *
     * @param fabric the Fabric client
     * @return a copy bound to {@code fabric}
     */
    public DeploymentPlan withFabric(FabricGateway fabric) {
        return toBuilder().fabric(fabric).build();
    }

    //  Lifecycle

    /**
     * Begins a value-realization assessment of this plan: declare your per-provider monthly egress
     * volumes and get back the egress savings the plan's private interconnects produce.
     *
     * @return a {@link PlanValueAssessment} builder for declaring egress volumes
     */
    public PlanValueAssessment valueRealization() {
        return new PlanValueAssessment(this);
    }

    /**
     * Re-runs the layered plan-time validation (structural and catalog checks, live Cloud Router
     * dry-run, connection endpoint dispatch) and returns a new plan with the refreshed status. Nothing
     * is provisioned. A live step that cannot be attempted is recorded as a skipped validation with a
     * reason; only a genuine rejection is an error. On a loaded plan (no source optimization) the
     * new-market checks that need the optimizer's recommendations are not run.
     *
     * @return a new plan with refreshed validity, deferred and skipped validations, and required inputs
     */
    public DeploymentPlan dryRun() {
        PlanValidator.Result result = PlanValidator.validate(
                sourceOptimization != null ? sourceOptimization.getRecommendations() : null,
                sourceOptimization != null ? sourceOptimization.getRequest() : null,
                sourceOptimization != null ? sourceOptimization.getTopology() : null,
                cloudRouters, providerConnections, backboneLinks, routingProtocols, multicloudLinks,
                deriveCustomerAsn(), fabric);
        return toBuilder()
                .valid(result.errors.isEmpty())
                .validationErrors(result.errors)
                .deferredValidations(result.deferred)
                .skippedValidations(result.skipped)
                .requiredInputs(result.requiredInputs)
                .build();
    }

    private Long deriveCustomerAsn() {
        if (routingProtocols == null) {
            return null;
        }
        return routingProtocols.stream().map(PlannedRoutingProtocol::getCustomerAsn)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * Compares the plan with live Fabric state (read-only): one verdict per planned resource
     * ({@code MISSING}, {@code PRESENT_MATCHING}, {@code PRESENT_DIFFERENT} with the differing fields,
     * {@code UNREADABLE}, {@code NOT_EXECUTABLE}) and the live resources that carry the plan's marker
     * but are not in it.
     *
     * @return the diff
     * @throws IllegalStateException when no Fabric client is bound
     */
    public PlanDiff diff() {
        return diff(null);
    }

    /**
     * As {@link #diff()}, also matching resources by the uuids recorded in a checkpoint store.
     *
     * @param store the checkpoint store, or {@code null}
     * @return the diff
     * @throws IllegalStateException when no Fabric client is bound
     */
    public PlanDiff diff(CheckpointStore store) {
        if (fabric == null) {
            throw new IllegalStateException("The plan has no Fabric client: bind one with withFabric(fabric).");
        }
        com.eqixiac.equinix.design.plan.Checkpoint checkpoint =
                store == null ? null : store.load(effectiveDeploymentName()).orElse(null);
        return DriftDetector.diff(this, LiveStateReader.read(fabric, this, checkpoint), checkpoint);
    }

    /**
     * Executes the plan with no customer inputs and default options. See
     * {@link #execute(ExecutionInputs, ExecutionOptions)}.
     *
     * @return the outcome
     */
    public DeploymentOutcome execute() {
        return execute(ExecutionInputs.none(), ExecutionOptions.defaults());
    }

    /**
     * Executes the plan with default options. See {@link #execute(ExecutionInputs, ExecutionOptions)}.
     *
     * @param inputs the per-connection authorization keys, VLAN tags and Azure peering types
     * @return the outcome
     */
    public DeploymentOutcome execute(ExecutionInputs inputs) {
        return execute(inputs, ExecutionOptions.defaults());
    }

    /**
     * Provisions the whole plan in one run (no live-state read; use {@link #reconcile} for an
     * idempotent, resumable run). Order, pre-flight, the failure policy and rollback are described on
     * {@link PlanExecutor}. A provider connection that needs a cloud authorization key the plan and the
     * inputs do not supply fails the run before anything is sent.
     *
     * @param inputs  the customer inputs; {@code null} means none
     * @param options execution options; {@code null} means defaults
     * @return the outcome, with every step, error and (after an abort) the rollback report
     * @throws IllegalStateException if the plan is not valid (the message lists the errors) or has no
     *         Fabric client
     */
    public DeploymentOutcome execute(ExecutionInputs inputs, ExecutionOptions options) {
        return PlanExecutor.execute(this, inputs, options);
    }

    /**
     * Brings live state to the plan, creating only what is missing and checkpointing after every step;
     * see {@link PlanExecutor#reconcile}.
     *
     * @param inputs  the customer inputs; {@code null} means none
     * @param options execution options (dry run, failure policy, opt-in updates); {@code null} means defaults
     * @param store   the checkpoint store; {@code null} means a fresh in-memory store
     * @return the outcome
     * @throws IllegalStateException if the plan is not valid or has no Fabric client
     */
    public DeploymentOutcome reconcile(ExecutionInputs inputs, ExecutionOptions options, CheckpointStore store) {
        return PlanExecutor.reconcile(this, inputs, options, store);
    }

    /**
     * Removes what an earlier run created: connections first, each polled to {@code DEPROVISIONED} or
     * 404 within the default delete timeout, then the Cloud Routers whose connections are all gone.
     * Everything that could not be removed is listed as an orphan with its uuid.
     *
     * @param outcome the outcome of {@link #execute} or {@link #reconcile}
     * @return the rollback report
     */
    public RollbackReport rollback(DeploymentOutcome outcome) {
        return rollback(outcome, ExecutionOptions.defaults());
    }

    /**
     * As {@link #rollback(DeploymentOutcome)} with explicit timeouts, poll interval and clock.
     *
     * @param outcome the outcome to undo; {@code null} is a no-op
     * @param options timeouts, poll interval and clock
     * @return the rollback report
     */
    public RollbackReport rollback(DeploymentOutcome outcome, ExecutionOptions options) {
        if (outcome == null || outcome.getResources() == null) {
            return RollbackReport.empty();
        }
        return PlanExecutor.rollback(this, outcome.getResources(), options);
    }
}
