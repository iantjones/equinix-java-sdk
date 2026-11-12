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

package com.eqixiac.equinix.mcp.server;

import com.eqixiac.equinix.design.export.TerraformExporter;
import com.eqixiac.equinix.design.optimizer.model.OptimizationResult;
import com.eqixiac.equinix.design.optimizer.wizard.DeploymentWizard;
import com.eqixiac.equinix.design.optimizer.wizard.enums.BackboneTopology;
import com.eqixiac.equinix.design.optimizer.wizard.enums.CloudToCloudStrategy;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlanPricing;
import com.eqixiac.equinix.design.optimizer.wizard.model.PlannedConnection;
import com.eqixiac.equinix.design.optimizer.wizard.model.ProfileCandidate;
import com.eqixiac.equinix.design.optimizer.wizard.model.ProfileSelection;
import com.eqixiac.equinix.design.plan.DriftStatus;
import com.eqixiac.equinix.design.plan.PlanDiff;
import com.eqixiac.equinix.design.plan.PlanJson;
import com.eqixiac.equinix.design.spec.DesignSpec;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.server.McpSyncServerExchange;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.eqixiac.equinix.mcp.server.Args.optEnum;
import static com.eqixiac.equinix.mcp.server.Args.optInt;
import static com.eqixiac.equinix.mcp.server.Args.optLong;
import static com.eqixiac.equinix.mcp.server.Args.optString;
import static com.eqixiac.equinix.mcp.server.Args.requireString;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.Detail;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.capped;
import static com.eqixiac.equinix.mcp.server.DesignPayloads.money;
import static com.eqixiac.equinix.mcp.server.Schemas.array;
import static com.eqixiac.equinix.mcp.server.Schemas.detail;
import static com.eqixiac.equinix.mcp.server.Schemas.integer;
import static com.eqixiac.equinix.mcp.server.Schemas.lowerNames;
import static com.eqixiac.equinix.mcp.server.Schemas.object;
import static com.eqixiac.equinix.mcp.server.Schemas.objectOrString;
import static com.eqixiac.equinix.mcp.server.Schemas.props;
import static com.eqixiac.equinix.mcp.server.Schemas.string;
import static com.eqixiac.equinix.mcp.server.Schemas.stringEnum;
import static com.eqixiac.equinix.mcp.server.Schemas.with;

/**
 * The deployment-plan lifecycle tools: {@code design_plan_deployment} (plan a design: dry-run
 * validation only), {@code design_get_plan} (the stored plan document), {@code design_plan_drift}
 * (read-only comparison of a stored plan with live state) and {@code design_export_terraform}. Plans
 * live in the {@link DesignStore} under their content-hash {@code plan_id}. Nothing here provisions
 * anything: the only writes this server can make are the Safe Mutation Broker's, in the opt-in
 * {@code mutate} toolset.
 */
final class PlanTools {

    /** Fields of the plan document that can carry text from a remote system (API errors, profile names). */
    private static final Set<String> REMOTE_TEXT_FIELDS = Set.of("validationErrors", "skippedValidations",
            "deferredValidations", "zSideProviderLabel", "providerLabel", "reasoning", "note", "disclaimer");

    private PlanTools() {
    }

    static List<ToolRegistration> tools() {
        return List.of(planDeployment(), getPlan(), planDrift(), exportTerraform());
    }

    static Map<String, Object> planIdSchema() {
        return with(string("A plan_id from design_plan_deployment in this server process (kept 60 minutes)."),
                "pattern", "^[0-9a-f]{16}$");
    }

    // design_plan_deployment

    private static ToolRegistration planDeployment() {
        Map<String, Object> deployment = object(props(
                "deployment_name", string("Deployment identity (checkpoints and the additionalInfo marker on every "
                        + "connection use it). Default: the spec's name."),
                "project_id", string("Fabric project the Cloud Routers are planned in. Needed later by "
                        + "design_plan_drift, which compares only inside the plan's project."),
                "account_number", with(integer("Billing account number stamped on each planned Cloud Router. "
                        + "Optional: when omitted it is resolved from the credentials (one visible account is used; "
                        + "several prompt the user, or return status=choice_required naming them). Never "
                        + "fabricated."), "minimum", 1),
                "notifications", with(array("Email address(es) for provisioning notifications. Fabric requires at "
                        + "least one on every Cloud Router (HTTP 400 EQ-3040013 without); a plan without one is "
                        + "invalid.", string("Email address.")), "minItems", 1),
                "customer_asn", with(integer("Your BGP ASN for the planned routing protocols. Default 65100."),
                        "minimum", 1, "maximum", 4_294_967_295L),
                "router_package", stringEnum("Fabric Cloud Router package. Default STANDARD.",
                        "LAB", "BASIC", "STANDARD", "ADVANCED", "PREMIUM"),
                "router_name_prefix", string("Name prefix of the planned routers and connections "
                        + "('{prefix}-{metro}'). Default 'FCR'."),
                "backbone_topology", stringEnum("Inter-metro backbone shape (only with two or more metros).",
                        lowerNames(BackboneTopology.class)),
                "backbone_bandwidth_mbps", with(integer("Bandwidth of each backbone link, Mbps. Default 10000."),
                        "minimum", 1),
                "cloud_to_cloud_strategy", stringEnum("Beta. How a workload reaching two clouds is planned: "
                        + "equinix_only, compare (default: Equinix path plus a priced native provider-to-provider "
                        + "ALTERNATIVE where the bundled catalog lists the region pair), native_when_available, "
                        + "native_only. Native links are planned and priced only; never provisioned.",
                        lowerNames(CloudToCloudStrategy.class)),
                "multicloud_path_tier", with(integer("Beta. AWS connectivity-scope tier pricing a native link's AWS "
                        + "side, 1 (local, default) to 5."), "minimum", 1, "maximum", 5)));
        return ToolRegistration.builder()
                .name("design_plan_deployment")
                .title("Plan a deployment (dry-run validation only; nothing provisioned)")
                .description("Turns a DesignSpec's recommended design into a deployment plan: Cloud Routers, cloud "
                        + "connections, backbone links and routing protocols, priced (in the plan's currencies, at "
                        + "commercial.term, on demand when unset) and validated in layers. Uses the stored "
                        + "design_optimize result for the spec (pass spec or spec_fingerprint); with none, the "
                        + "optimizer runs first. LIVE REQUESTS: reads live prices, metros, service profiles and billing "
                        + "accounts, and SENDS one Cloud Router create request per planned router with dryRun=true: "
                        + "the Fabric API validates it and provisions nothing. NOTHING is provisioned and this tool "
                        + "never executes a plan. validation.valid=false only for real errors; checks deferred to "
                        + "provisioning (the connection dry-run needs the router and your cloud authorization keys) "
                        + "and checks skipped (not entitled, throttled, offline) never invalidate a plan. "
                        + "required_inputs lists what you must gather per connection (authorization key, VLAN, "
                        + "Azure peering type). Returns plan_id (a content hash, stored 60 minutes in this server "
                        + "process) and the plan document; use design_get_plan, design_plan_drift and "
                        + "design_export_terraform with the plan_id. Cancellable.")
                .inputSchema(object(props(
                        SpecInput.ARG, objectOrString(SpecTools.SPEC_ARG_DESCRIPTION),
                        SpecInput.FINGERPRINT_ARG, DesignToolFactory.fingerprintSchema(),
                        "deployment", deployment,
                        "detail", detail("standard", "summary returns counts, pricing and validation only; "
                                + "standard adds the routers, connections, links, required inputs and the plan "
                                + "document; full adds the Markdown plan."))))
                .outputSchema(DesignPayloads.outputSchema("plan_id, spec_fingerprint, executed=false, summary, "
                        + "pricing, validation (valid, validated_now, deferred_to_provisioning, skipped), "
                        + "required_inputs, resources, plan_document, account_resolution; or status=choice_required "
                        + "with candidate account numbers."))
                .toolset(Toolset.DESIGN)
                .handler((ToolHandler.Cancellable) PlanTools::handlePlanDeployment)
                .build();
    }

    private static ObjectNode handlePlanDeployment(JsonNode args, ServerContext ctx, CancellationSignal cancellation)
            throws Exception {
        DesignToolFactory.SpecAndRun input = DesignToolFactory.specOf(args, ctx);
        DesignSpec spec = input.spec();
        Detail detail = Detail.of(args, Detail.STANDARD);
        PricingBudget budget = ctx.pricingBudget();
        List<TimeoutRateCard> guards = new ArrayList<>();
        OptimizationResult optimized = input.optimized();
        String optimizationSource = "stored design_optimize result";
        List<String> evicted = new ArrayList<>();
        if (optimized == null) {
            DesignToolFactory.Optimized run = DesignToolFactory.optimize(spec, ctx, budget, guards, cancellation);
            optimized = run.result();
            evicted.addAll(run.evicted());
            optimizationSource = "design_optimize run by this call (now stored)";
        }
        if (optimized.getRecommended() == null) {
            throw new IllegalArgumentException("The optimizer found no feasible design for this spec, so there is "
                    + "nothing to plan. Relax the constraints named in design_optimize findings and try again.");
        }

        McpSyncServerExchange exchange = ctx.currentExchange();
        DeploymentWizard.Builder wizard = ctx.design().deploymentWizard(optimized);
        wizard.designSpec(spec);
        wizard.term(spec.commercial().term() == null ? Term.MONTH_1 : spec.commercial().term());
        DesignToolFactory.fabricIfAvailable(ctx).ifPresent(fabric ->
                wizard.rateCard(TimeoutRateCard.equinixLive(fabric, budget, guards)));
        wizard.multicloudEnvironments(ctx.multicloudEnvironments());
        JsonNode d = args.path("deployment");
        Optional<Long> explicitAccount = Optional.empty();
        if (d.isObject()) {
            optString(d, "deployment_name").ifPresent(wizard::deploymentName);
            optLong(d, "customer_asn").ifPresent(wizard::customerAsn);
            optString(d, "router_package").ifPresent(wizard::routerPackage);
            optString(d, "router_name_prefix").ifPresent(wizard::routerNamePrefix);
            optEnum(d, "backbone_topology", BackboneTopology.class).ifPresent(wizard::backboneTopology);
            optInt(d, "backbone_bandwidth_mbps").ifPresent(wizard::backboneBandwidthMbps);
            optString(d, "project_id").ifPresent(wizard::projectId);
            optEnum(d, "cloud_to_cloud_strategy", CloudToCloudStrategy.class).ifPresent(wizard::cloudToCloudStrategy);
            Optional<Integer> tier = optInt(d, "multicloud_path_tier");
            if (tier.isPresent()) {
                if (tier.get() < 1 || tier.get() > 5) {
                    throw new IllegalArgumentException("'deployment.multicloud_path_tier' must be between 1 and 5: "
                            + tier.get() + ".");
                }
                wizard.multicloudPathTier(tier.get());
            }
            explicitAccount = optLong(d, "account_number");
            List<String> notifications = Args.stringList(d, "notifications");
            if (!notifications.isEmpty()) {
                wizard.notifications(notifications.toArray(new String[0]));
            }
        }

        ObjectNode accountBlock;
        if (explicitAccount.isPresent()) {
            wizard.accountNumber(explicitAccount.get());
            accountBlock = ctx.objectMapper().createObjectNode();
            accountBlock.put("resolution", "explicit");
            accountBlock.put("account_number", String.valueOf(explicitAccount.get()));
            accountBlock.put("source", "the account_number supplied in the request: auto-resolution was skipped");
        } else {
            AccountResolver.Resolution resolution = AccountResolver.resolve(ctx, exchange);
            if (resolution.kind() == AccountResolver.Kind.CHOICE_REQUIRED) {
                return accountChoiceRequiredPayload(ctx, resolution, spec.fingerprint());
            }
            if (resolution.kind() == AccountResolver.Kind.RESOLVED) {
                wizard.accountNumber(resolution.accountNumber());
            }
            accountBlock = accountResolutionBlock(ctx, resolution);
        }

        DeploymentPlan plan = EngineRunner.run("the deployment planning (catalogue reads and dryRun=true router "
                + "validation; nothing provisioned)", cancellation, wizard::plan);
        ProfileChoices choices = applyProfileChoices(plan, ctx, exchange);
        plan = choices.plan();
        if (choices.changed()) {
            plan = wizard.reprice(plan);
        }

        DesignStore.Put put = ctx.designStore().putPlan(plan);
        evicted.addAll(put.evicted());
        ObjectNode payload = planPayload(plan, ctx, choices, detail);
        payload.put("optimization_source", optimizationSource);
        payload.set("account_resolution", accountBlock);
        DesignToolFactory.reportEvicted(payload, evicted, DesignStore.Kind.PLAN);
        TimeoutRateCard.reportEquinix(payload, guards, budget);
        return payload;
    }

    private static ObjectNode accountResolutionBlock(ServerContext ctx, AccountResolver.Resolution resolution) {
        ObjectNode block = ctx.objectMapper().createObjectNode();
        if (resolution.kind() == AccountResolver.Kind.RESOLVED) {
            block.put("resolution", "auto_resolved");
            block.put("account_number", resolution.accountNumberText());
            block.put("source", resolution.source());
        } else {
            block.put("resolution", "unresolved");
            block.putNull("account_number");
            UntrustedText.put(block, "message", resolution.message());
        }
        return block;
    }

    private static ObjectNode accountChoiceRequiredPayload(ServerContext ctx, AccountResolver.Resolution resolution,
                                                           String fingerprint) {
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("status", "choice_required");
        payload.put("executed", false);
        payload.put("spec_fingerprint", fingerprint);
        UntrustedText.put(payload, "reason", resolution.message());
        ObjectNode account = payload.putObject("account_number");
        account.put("resolution", "choice_required");
        UntrustedText.put(account, "message", resolution.message());
        ArrayNode candidates = account.putArray("candidates");
        resolution.candidates().forEach(candidate -> {
            ObjectNode c = candidates.addObject();
            c.put("account_number", candidate.number());
            UntrustedText.put(c, "account_name", candidate.name());
        });
        payload.put("next_step", "Re-call design_plan_deployment with deployment.account_number chosen from the "
                + "candidates above.");
        DesignPayloads.provenance(payload, "live", null, fingerprint,
                List.of("Billing accounts visible to the credentials (live, read-only)"));
        return payload;
    }

    // payload

    /**
     * The plan result: identity, summary, pricing, layered validation and, from {@code standard}, the
     * planned resources, required inputs and the plan document.
     *
     * @param plan    the plan
     * @param ctx     the context
     * @param choices how profile choices were resolved, or {@code null}
     * @param detail  how much to return
     * @return the payload
     */
    static ObjectNode planPayload(DeploymentPlan plan, ServerContext ctx, ProfileChoices choices, Detail detail) {
        ObjectMapper mapper = ctx.objectMapper();
        ObjectNode payload = mapper.createObjectNode();
        String planId = plan.planId();
        payload.put("plan_id", planId);
        payload.put("plan_id_scope", "A content hash of the plan, resolvable in this server process for "
                + ctx.designStore().ttl().toMinutes() + " minutes (design_get_plan, design_plan_drift, "
                + "design_export_terraform) and as the resource " + DesignResources.planUri(planId) + ".");
        payload.put("spec_fingerprint", plan.getSpecFingerprint());
        payload.put("deployment_name", plan.effectiveDeploymentName());
        payload.put("project_id", plan.getProjectId());
        payload.put("executed", false);
        payload.set("summary", UntrustedText.document(plan.toSummary()));
        ObjectNode counts = payload.putObject("resource_counts");
        counts.put("cloud_routers", size(plan.getCloudRouters()));
        counts.put("provider_connections", size(plan.getProviderConnections()));
        counts.put("backbone_links", size(plan.getBackboneLinks()));
        counts.put("routing_protocols", size(plan.getRoutingProtocols()));
        if (!plan.multicloudLinksOrEmpty().isEmpty()) {
            counts.put("native_multicloud_links", plan.multicloudLinksOrEmpty().size());
        }
        if (!plan.interconnectsOrEmpty().isEmpty()) {
            counts.put("interconnects_not_executable", plan.interconnectsOrEmpty().size());
        }

        PlanPricing pricing = plan.getPricing();
        if (pricing != null) {
            ObjectNode p = payload.putObject("pricing");
            money(p, "monthly_total", pricing.getMonthlyTotal());
            money(p, "setup_total", pricing.getSetupTotal());
            p.put("currency", pricing.getCurrency());
            money(p, "router_monthly", pricing.getRouterMonthlyCost());
            money(p, "provider_connection_monthly", pricing.getProviderConnectionMonthlyCost());
            money(p, "backbone_monthly", pricing.getBackboneMonthlyCost());
            p.put("price_source", String.valueOf(pricing.getSource()));
            UntrustedText.put(p, "disclaimer", pricing.getDisclaimer());
        }
        validation(payload, plan, detail);

        if (detail.atLeast(Detail.STANDARD)) {
            resources(payload, plan, choices, detail);
            MulticloudDesignTools.serializePlanLinks(payload, plan, mapper);
            requiredInputs(payload, plan);
            payload.set("plan_document", planDocument(plan, mapper));
        }
        if (detail == Detail.FULL) {
            payload.set("markdown", UntrustedText.document(plan.toMarkdown()));
        }
        ArrayNode next = payload.putArray("next_steps");
        next.add("design_export_terraform with plan_id " + planId + " for infrastructure as code");
        next.add("design_plan_drift with plan_id " + planId + " to compare with live state (needs project_id)");
        DesignPayloads.provenance(payload, pricing == null ? "computed" : priceKind(pricing),
                plan.getSourceComputedAt() == null ? null : plan.getSourceComputedAt().toString(),
                plan.getSpecFingerprint(), List.of("Deployment Wizard over the design_optimize result",
                        "Fabric service profiles and prices (live, read-only)",
                        "Cloud Router validation: POST /fabric/v4/routers?dryRun=true (provisions nothing)"));
        return payload;
    }

    private static String priceKind(PlanPricing pricing) {
        String s = String.valueOf(pricing.getSource()).toUpperCase(Locale.ROOT);
        if (s.contains("LIVE")) {
            return "live";
        }
        if (s.contains("REFERENCE")) {
            return "reference";
        }
        if (s.contains("ESTIMATE")) {
            return "estimate";
        }
        return "mixed";
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    /**
     * The plan document ({@code PlanJson}) as JSON, with text that may come from a remote system
     * labelled as untrusted.
     *
     * @param plan   the plan
     * @param mapper the mapper
     * @return the document
     */
    static JsonNode planDocument(DeploymentPlan plan, ObjectMapper mapper) {
        try {
            JsonNode doc = mapper.readTree(PlanJson.toJson(plan));
            return markRemoteText(doc);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("The plan document could not be rendered: " + e.getMessage(), e);
        }
    }

    private static JsonNode markRemoteText(JsonNode node) {
        if (node.isObject()) {
            ObjectNode o = (ObjectNode) node;
            List<String> names = new ArrayList<>();
            o.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                JsonNode child = o.get(name);
                if (REMOTE_TEXT_FIELDS.contains(name) && child.isTextual()) {
                    o.set(name, UntrustedText.node(child.asText()));
                } else if (REMOTE_TEXT_FIELDS.contains(name) && child.isArray()) {
                    ArrayNode replaced = o.putArray(name);
                    child.forEach(item -> {
                        if (item.isTextual()) {
                            UntrustedText.add(replaced, item.asText());
                        } else {
                            replaced.add(markRemoteText(item));
                        }
                    });
                } else {
                    markRemoteText(child);
                }
            }
        } else if (node.isArray()) {
            node.forEach(PlanTools::markRemoteText);
        }
        return node;
    }

    private static void validation(ObjectNode payload, DeploymentPlan plan, Detail detail) {
        ObjectNode validation = payload.putObject("validation");
        validation.put("valid", plan.isValid());
        List<String> errors = plan.getValidationErrors() == null ? List.of() : plan.getValidationErrors();
        ObjectNode now = validation.putObject("validated_now");
        now.put("scope", "Structural and catalogue checks and the live Cloud Router dry-run (POST "
                + "/fabric/v4/routers?dryRun=true). A non-empty errors list makes the plan invalid.");
        DesignPayloads.strings(now, "errors", errors, detail.cap() * 2, true);
        now.put("error_count", errors.size());
        DesignPayloads.strings(now, "new_market_gaps", errors.stream()
                .filter(e -> e != null && e.toLowerCase(Locale.ROOT).contains("new-market")).toList(), detail.cap(), true);
        List<String> deferred = plan.getDeferredValidations() == null ? List.of() : plan.getDeferredValidations();
        ObjectNode def = validation.putObject("deferred_to_provisioning");
        def.put("scope", "The live connection dry-run: it needs the Cloud Router (created at provisioning) and your "
                + "cloud authorization keys. Not errors.");
        DesignPayloads.strings(def, "notes", deferred, detail.cap(), true);
        def.put("count", deferred.size());
        List<String> skipped = plan.getSkippedValidations() == null ? List.of() : plan.getSkippedValidations();
        ObjectNode sk = validation.putObject("skipped");
        sk.put("scope", "Live checks that could not be attempted (offline, not entitled 401/403, throttled 429, "
                + "server error, timeout). Not errors; each note says what was not checked and why.");
        DesignPayloads.strings(sk, "notes", skipped, detail.cap(), true);
        sk.put("count", skipped.size());
    }

    private static void resources(ObjectNode payload, DeploymentPlan plan, ProfileChoices choices, Detail detail) {
        int cap = detail == Detail.FULL ? 100 : 30;
        capped(payload, "cloud_routers", plan.getCloudRouters() == null ? List.of() : plan.getCloudRouters(), cap,
                (array, cr) -> {
                    ObjectNode r = array.addObject();
                    r.put("name", cr.getName());
                    r.put("metro", String.valueOf(cr.getMetroId()));
                    r.put("package", String.valueOf(cr.getPackageCode()));
                    r.put("project_id", cr.getProjectId());
                    if (cr.getAccountNumber() != null) {
                        r.put("account_number", cr.getAccountNumber());
                    } else {
                        r.putNull("account_number");
                    }
                });
        capped(payload, "provider_connections",
                plan.getProviderConnections() == null ? List.of() : plan.getProviderConnections(), cap, (array, pc) -> {
                    ObjectNode cn = array.addObject();
                    cn.put("name", pc.getName());
                    cn.put("bandwidth_mbps", pc.getBandwidthMbps());
                    cn.put("metro", String.valueOf(pc.getASideMetro()));
                    UntrustedText.put(cn, "provider", pc.getZSideProviderLabel());
                    cn.put("seller_region", pc.getZSideSellerRegion());
                    cn.put("router", pc.getASideRouterName());
                    serializeProfileSelection(cn, pc, choices);
                });
        capped(payload, "backbone_links", plan.getBackboneLinks() == null ? List.of() : plan.getBackboneLinks(), cap,
                (array, bl) -> {
                    ObjectNode ln = array.addObject();
                    ln.put("name", bl.getName());
                    ln.put("metro_a", String.valueOf(bl.getMetroA()));
                    ln.put("metro_z", String.valueOf(bl.getMetroZ()));
                    ln.put("bandwidth_mbps", bl.getBandwidthMbps());
                });
    }

    private static void requiredInputs(ObjectNode payload, DeploymentPlan plan) {
        ArrayNode requiredInputs = payload.putArray("required_inputs");
        if (plan.getRequiredInputs() != null) {
            plan.getRequiredInputs().forEach(req -> {
                ObjectNode r = requiredInputs.addObject();
                r.put("connection", req.getConnectionName());
                UntrustedText.put(r, "provider", req.getProviderLabel());
                r.put("cloud_type", String.valueOf(req.getCloudType()));
                r.put("authentication_key_required", req.isAuthenticationKeyRequired());
                if (req.isAuthenticationKeyRequired()) {
                    r.put("authentication_key_label", req.getAuthenticationKeyLabel());
                }
                r.put("authentication_key_provided", req.isAuthenticationKeyProvided());
                r.put("vlan_tag_required", req.isVlanTagRequired());
                r.put("peering_type_required", req.isPeeringTypeRequired());
                UntrustedText.put(r, "summary", req.describe());
            });
        }
    }

    private static void serializeProfileSelection(ObjectNode cn, PlannedConnection pc, ProfileChoices choices) {
        ProfileSelection selection = pc.getProfileSelection();
        if (selection == null) {
            return;
        }
        ObjectNode ps = cn.putObject("profile_selection");
        ps.put("requested_mbps", selection.getRequestedMbps());
        ps.put("selected_tier_mbps", selection.getSelectedTierMbps());
        ps.put("rounded_up", selection.isRoundedUp());
        ps.put("rounded_up_by_mbps", selection.roundedUpByMbps());
        ps.put("selected_profile_uuid", selection.getSelectedProfileUuid());
        ps.put("seller_region", selection.getSelectedSellerRegion());
        ps.put("has_choice", selection.hasChoice());
        if (selection.getReasoning() != null) {
            UntrustedText.put(ps, "reasoning", selection.getReasoning());
        }
        ArrayNode alternatives = ps.putArray("alternatives");
        if (selection.getAlternatives() != null) {
            for (ProfileCandidate candidate : selection.getAlternatives()) {
                ObjectNode a = alternatives.addObject();
                a.put("service_profile_uuid", candidate.getServiceProfileUuid());
                a.put("covering_tier_mbps", candidate.getCoveringTierMbps());
                a.put("allow_custom_bandwidth", candidate.isAllowCustomBandwidth());
                ArrayNode regions = a.putArray("seller_regions");
                if (candidate.getSellerRegions() != null) {
                    candidate.getSellerRegions().forEach(regions::add);
                }
                ArrayNode bandwidths = a.putArray("supported_bandwidths");
                if (candidate.getSupportedBandwidths() != null) {
                    for (Integer bandwidth : candidate.getSupportedBandwidths()) {
                        bandwidths.add(bandwidth);
                    }
                }
                if (candidate.getVcBandwidthMax() != null) {
                    a.put("vc_bandwidth_max_mbps", candidate.getVcBandwidthMax());
                }
            }
        }
        ProfileChoiceRecord record = choices == null ? null : choices.records().get(pc.getName());
        if (record != null) {
            ObjectNode choice = ps.putObject("choice");
            choice.put("status", record.status());
            choice.put("selected_profile_uuid", record.selectedProfileUuid());
            choice.put("detail", record.detail());
        }
    }

    // profile-choice elicitation

    /**
     * For each provider connection with more than one covering service profile, prompts the user to pick
     * one; a pick that differs from the wizard's default rebuilds that connection (and the plan) onto the
     * chosen profile. When the client cannot elicit, or the user declines, the default is kept: it is
     * always valid. Never throws: a prompt failure keeps the default.
     *
     * @param plan     the plan
     * @param ctx      the context
     * @param exchange the client exchange, or {@code null}
     * @return the (possibly rebuilt) plan and the per-connection record
     */
    static ProfileChoices applyProfileChoices(DeploymentPlan plan, ServerContext ctx, McpSyncServerExchange exchange) {
        Map<String, ProfileChoiceRecord> records = new LinkedHashMap<>();
        List<PlannedConnection> connections = plan.getProviderConnections();
        if (connections == null || connections.isEmpty()) {
            return new ProfileChoices(plan, records, false);
        }
        boolean canElicit = ElicitationSupport.supportsForm(exchange);
        List<PlannedConnection> rebuilt = new ArrayList<>(connections.size());
        boolean changed = false;
        for (PlannedConnection conn : connections) {
            ProfileSelection selection = conn.getProfileSelection();
            if (selection == null || !selection.hasChoice()) {
                rebuilt.add(conn);
                continue;
            }
            if (!canElicit) {
                records.put(conn.getName(), ProfileChoiceRecord.keptDefault(selection.getSelectedProfileUuid(),
                        "the client did not declare elicitation support: kept the default (tightest-fit) profile"));
                rebuilt.add(conn);
                continue;
            }
            List<ElicitationSupport.Option> options = new ArrayList<>();
            for (ProfileCandidate candidate : selection.getAlternatives()) {
                String detail = candidate.getCoveringTierMbps() + " Mbps"
                        + (candidate.firstSellerRegion() != null ? " @ " + candidate.firstSellerRegion() : "")
                        + (candidate.getServiceProfileUuid().equals(selection.getSelectedProfileUuid())
                        ? " (default)" : "");
                options.add(new ElicitationSupport.Option(candidate.getServiceProfileUuid(),
                        "Profile " + candidate.getServiceProfileUuid(), detail));
            }
            ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(ctx, exchange,
                    "Connection '" + conn.getName() + "' can be carried by more than one service profile. "
                            + "Which profile should it use?", options);
            if (!outcome.picked()) {
                records.put(conn.getName(), ProfileChoiceRecord.keptDefault(selection.getSelectedProfileUuid(),
                        outcome.detail() + ": kept the default (tightest-fit) profile"));
                rebuilt.add(conn);
                continue;
            }
            String pickedUuid = outcome.option().id();
            if (pickedUuid.equals(selection.getSelectedProfileUuid())) {
                records.put(conn.getName(), ProfileChoiceRecord.picked(pickedUuid,
                        "the user confirmed the default profile"));
                rebuilt.add(conn);
                continue;
            }
            ProfileCandidate chosen = candidateByUuid(selection, pickedUuid);
            if (chosen == null) {
                records.put(conn.getName(), ProfileChoiceRecord.keptDefault(selection.getSelectedProfileUuid(),
                        "the selection matched no candidate: kept the default profile"));
                rebuilt.add(conn);
                continue;
            }
            rebuilt.add(withChosenProfile(conn, selection, chosen));
            records.put(conn.getName(), ProfileChoiceRecord.picked(pickedUuid,
                    "the user selected profile " + pickedUuid + " at " + chosen.getCoveringTierMbps() + " Mbps"));
            changed = true;
        }
        return new ProfileChoices(changed ? plan.toBuilder().providerConnections(rebuilt).build() : plan, records,
                changed);
    }

    private static ProfileCandidate candidateByUuid(ProfileSelection selection, String uuid) {
        if (selection.getAlternatives() == null) {
            return null;
        }
        for (ProfileCandidate candidate : selection.getAlternatives()) {
            if (candidate.getServiceProfileUuid().equals(uuid)) {
                return candidate;
            }
        }
        return null;
    }

    private static PlannedConnection withChosenProfile(PlannedConnection conn, ProfileSelection selection,
                                                       ProfileCandidate chosen) {
        List<ProfileCandidate> reordered = new ArrayList<>();
        reordered.add(chosen);
        for (ProfileCandidate candidate : selection.getAlternatives()) {
            if (!candidate.getServiceProfileUuid().equals(chosen.getServiceProfileUuid())) {
                reordered.add(candidate);
            }
        }
        String reasoning = (selection.getReasoning() == null ? "" : selection.getReasoning() + " ")
                + "User selected profile " + chosen.getServiceProfileUuid() + " at "
                + chosen.getCoveringTierMbps() + " Mbps.";
        ProfileSelection updated = ProfileSelection.builder()
                .requestedMbps(selection.getRequestedMbps())
                .selectedProfileUuid(chosen.getServiceProfileUuid())
                .selectedSellerRegion(chosen.firstSellerRegion())
                .selectedTierMbps(chosen.getCoveringTierMbps())
                .roundedUp(chosen.getCoveringTierMbps() > selection.getRequestedMbps())
                .alternatives(reordered)
                .reasoning(reasoning)
                .build();
        return conn.toBuilder()
                .zSideServiceProfileUuid(chosen.getServiceProfileUuid())
                .zSideSellerRegion(chosen.firstSellerRegion())
                .bandwidthMbps(chosen.getCoveringTierMbps())
                .profileSelection(updated)
                .build();
    }

    /**
     * The result of resolving profile choices.
     *
     * @param plan    the plan, rebuilt onto any user-chosen profiles
     * @param records per connection name, how each genuine choice was resolved
     * @param changed whether a pick moved a connection to another profile (pricing must be recomputed)
     */
    record ProfileChoices(DeploymentPlan plan, Map<String, ProfileChoiceRecord> records, boolean changed) {
    }

    /**
     * How one connection's profile choice was resolved.
     *
     * @param status              {@code picked} or {@code kept_default}
     * @param selectedProfileUuid the profile the plan ended up on
     * @param detail              an explanation
     */
    record ProfileChoiceRecord(String status, String selectedProfileUuid, String detail) {

        static ProfileChoiceRecord picked(String profileUuid, String detail) {
            return new ProfileChoiceRecord("picked", profileUuid, detail);
        }

        static ProfileChoiceRecord keptDefault(String profileUuid, String detail) {
            return new ProfileChoiceRecord("kept_default", profileUuid, detail);
        }
    }

    // design_get_plan

    private static ToolRegistration getPlan() {
        return ToolRegistration.builder()
                .name("design_get_plan")
                .title("Get a stored deployment plan")
                .description("Returns a plan stored by design_plan_deployment in this server process: its identity "
                        + "(plan_id, spec_fingerprint, deployment name, project), pricing, validation and, from "
                        + "detail=standard, the planned resources, required inputs and the plan document (the JSON "
                        + "the SDK saves and loads with PlanJson); detail=full adds the Markdown plan. Reads only this "
                        + "process's memory.")
                .inputSchema(object(props(
                                "plan_id", planIdSchema(),
                                "detail", detail("standard", "summary returns counts, pricing and validation only.")),
                        "plan_id"))
                .outputSchema(DesignPayloads.outputSchema("The stored plan: plan_id, spec_fingerprint, pricing, "
                        + "validation, resources, plan_document.", "plan_id"))
                .toolset(Toolset.DESIGN)
                .openWorld(false)
                .handler(PlanTools::handleGetPlan)
                .build();
    }

    static DeploymentPlan storedPlan(JsonNode args, ServerContext ctx) {
        String planId = requireString(args, "plan_id");
        DesignStore.Lookup<DeploymentPlan> lookup = ctx.designStore().plan(planId);
        if (!lookup.found()) {
            throw new IllegalArgumentException(ctx.designStore().explainMissing(DesignStore.Kind.PLAN, planId,
                    lookup.status()));
        }
        return lookup.entry().value();
    }

    private static ObjectNode handleGetPlan(JsonNode args, ServerContext ctx) {
        DeploymentPlan plan = storedPlan(args, ctx);
        return planPayload(plan, ctx, null, Detail.of(args, Detail.STANDARD));
    }

    // design_plan_drift

    private static ToolRegistration planDrift() {
        return ToolRegistration.builder()
                .name("design_plan_drift")
                .title("Compare a stored plan with live Fabric state (read-only)")
                .description("Reads the live Cloud Routers, connections and routing protocols relevant to a stored "
                        + "plan and classifies every planned resource: MISSING, PRESENT_MATCHING, PRESENT_DIFFERENT "
                        + "(with the differing fields and whether each can be patched), UNREADABLE or NOT_EXECUTABLE; "
                        + "and lists live resources carrying the plan's deployment marker that the plan does not "
                        + "contain. READ-ONLY: searches and GETs only. Scoped to the plan's project: a plan without a "
                        + "project_id is refused, because a name-only search would compare against resources other "
                        + "owners happen to have named alike (re-plan with deployment.project_id). The connection "
                        + "search's project filter is not trusted (observed 2026-11-12 returning other projects' "
                        + "connections): every connection is re-checked client-side. Cancellable.")
                .inputSchema(object(props(
                                "plan_id", planIdSchema(),
                                "detail", detail("summary", "standard and full raise the list caps.")),
                        "plan_id"))
                .outputSchema(DesignPayloads.outputSchema("in_sync, counts per status, resources (key, kind, name, "
                        + "status, live_uuid, matched_by, differences), unmanaged, notes, read_at.", "plan_id",
                        "in_sync"))
                .toolset(Toolset.DESIGN)
                .handler((ToolHandler.Cancellable) PlanTools::handleDrift)
                .build();
    }

    private static ObjectNode handleDrift(JsonNode args, ServerContext ctx, CancellationSignal cancellation)
            throws Exception {
        DeploymentPlan plan = storedPlan(args, ctx);
        Detail detail = Detail.of(args, Detail.SUMMARY);
        if (plan.getProjectId() == null || plan.getProjectId().isBlank()) {
            throw new IllegalArgumentException("Plan " + plan.planId() + " names no Fabric project, so drift cannot "
                    + "be scoped: re-run design_plan_deployment with deployment.project_id and compare that plan.");
        }
        DeploymentPlan bound = plan.withFabric(ctx.fabric());
        PlanDiff diff = EngineRunner.run("the drift read", cancellation, bound::diff);
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("plan_id", diff.planId());
        payload.put("deployment_name", diff.deploymentName());
        payload.put("project_id", diff.projectId());
        payload.put("spec_fingerprint", plan.getSpecFingerprint());
        payload.put("in_sync", diff.isInSync());
        Map<DriftStatus, Integer> counts = new EnumMap<>(DriftStatus.class);
        diff.resources().forEach(r -> counts.merge(r.status(), 1, Integer::sum));
        ObjectNode c = payload.putObject("counts");
        counts.forEach((status, n) -> c.put(status.name().toLowerCase(Locale.ROOT), n));
        capped(payload, "resources", diff.resources(), detail == Detail.SUMMARY ? 20 : detail.cap() * 4,
                (array, r) -> {
                    ObjectNode n = array.addObject();
                    n.put("key", r.key());
                    n.put("kind", String.valueOf(r.kind()));
                    n.put("name", r.name());
                    n.put("status", r.status().name());
                    n.put("live_uuid", r.liveUuid());
                    n.put("matched_by", r.matchedBy());
                    ArrayNode diffs = n.putArray("differences");
                    r.differences().forEach(f -> {
                        ObjectNode fn = diffs.addObject();
                        fn.put("field", f.field());
                        fn.put("planned", f.planned());
                        UntrustedText.put(fn, "live", f.live());
                        fn.put("patchable", f.patchable());
                    });
                    UntrustedText.put(n, "note", r.note());
                });
        capped(payload, "unmanaged", diff.unmanaged(), detail.cap(), (array, u) -> {
            ObjectNode n = array.addObject();
            n.put("kind", String.valueOf(u.kind()));
            UntrustedText.put(n, "name", u.name());
            n.put("uuid", u.uuid());
            UntrustedText.put(n, "reason", u.reason());
        });
        DesignPayloads.strings(payload, "notes", diff.notes(), detail.cap(), true);
        payload.put("read_at", diff.readAt() == null ? null : diff.readAt().toString());
        DesignPayloads.provenance(payload, "live", payload.path("read_at").asText(null), plan.getSpecFingerprint(),
                List.of("Fabric Cloud Router and connection searches in project " + plan.getProjectId()
                        + " (live, read-only)"));
        return payload;
    }

    // design_export_terraform

    private static ToolRegistration exportTerraform() {
        return ToolRegistration.builder()
                .name("design_export_terraform")
                .title("Export a stored plan as Terraform")
                .description("Renders a stored plan (plan_id from design_plan_deployment in this server process) as "
                        + "Terraform HCL for the equinix/equinix provider: Cloud Routers, connections (the secondary of "
                        + "a redundant pair references the primary's group) and routing protocols. Cloud "
                        + "authorization keys are never in the plan: supply them as Terraform variables. Reads only "
                        + "this process's memory.")
                .inputSchema(object(props("plan_id", planIdSchema()), "plan_id"))
                .outputSchema(DesignPayloads.outputSchema("The Terraform HCL, plan_id, resource_count.", "plan_id",
                        "terraform"))
                .toolset(Toolset.DESIGN)
                .openWorld(false)
                .handler(PlanTools::handleExportTerraform)
                .build();
    }

    private static ObjectNode handleExportTerraform(JsonNode args, ServerContext ctx) {
        DeploymentPlan plan = storedPlan(args, ctx);
        String hcl = new TerraformExporter().export(plan);
        ObjectNode payload = ctx.objectMapper().createObjectNode();
        payload.put("plan_id", plan.planId());
        payload.put("spec_fingerprint", plan.getSpecFingerprint());
        payload.put("format", "terraform-hcl");
        payload.put("resource_count", plan.totalResourceCount());
        payload.put("terraform", hcl);
        payload.put("note", "Review before applying: nothing has been provisioned by this server.");
        DesignPayloads.provenance(payload, "computed", null, plan.getSpecFingerprint(),
                List.of("stored plan " + plan.planId() + " (this server process)"));
        return payload;
    }
}
