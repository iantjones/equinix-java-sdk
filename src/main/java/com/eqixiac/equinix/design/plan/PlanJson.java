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

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.core.model.multicloud.EnvironmentRef;
import com.eqixiac.equinix.core.model.multicloud.ProviderSite;
import com.eqixiac.equinix.design.optimizer.model.MulticloudEnvironment;
import com.eqixiac.equinix.design.optimizer.wizard.model.DeploymentPlan;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads and writes a {@link DeploymentPlan} as JSON and computes its content identity.
 *
 * <p>The document holds everything needed to diff, reconcile and resume the plan: every planned
 * resource with its logical key, the native multicloud links and Interconnects, pricing, the
 * validation state, the design-spec fingerprint, the deployment name, the name prefix and the
 * project. It does not hold the source {@code OptimizationResult} (a loaded plan's {@code dryRun()}
 * therefore skips the checks that need the optimizer's recommendations, and says so) nor the Fabric
 * client (bind one with {@code DeploymentPlan.withFabric}).</p>
 *
 * <p><b>Plan id.</b> {@link #planId(DeploymentPlan)} is the first 16 hex characters of the SHA-256 of
 * the plan's <em>desired state</em> in canonical JSON (properties and map entries sorted): the planned
 * resources, Interconnects, native links, deployment name, name prefix, project and spec fingerprint.
 * Pricing and validation results are not part of it, so repricing or re-validating keeps the id; any
 * change to what would be built changes it. The id is written into the document, and {@link #fromJson}
 * refuses a document whose stored id does not match its content (an edited or corrupt file).</p>
 */
public final class PlanJson {

    /** The document format version this release reads and writes. */
    public static final String SCHEMA_VERSION = "1";

    private static final ObjectMapper MAPPER = configure(JsonMapper.builder()).build();

    private static final ObjectMapper CANONICAL = configure(JsonMapper.builder())
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private PlanJson() {
    }

    private static JsonMapper.Builder configure(JsonMapper.Builder builder) {
        return builder
                .addModule(new Jdk8Module())
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // Derived read-only properties (for example a link's "roundedUp") are written for
                // readers and skipped on read; integrity is enforced by the plan id instead.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                // Money keeps its scale through the tree model (9000.90 stays 9000.90).
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .disable(com.fasterxml.jackson.databind.cfg.JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .addMixIn(MetroId.class, MetroIdMixin.class)
                .annotationIntrospector(new LombokBuilderIntrospector());
    }

    /**
     * The shared mapper for plan documents, checkpoints and live-state snapshots.
     *
     * @return the configured mapper (do not reconfigure it)
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Serializes a plan, including its {@code planId}.
     *
     * @param plan the plan
     * @return indented JSON
     */
    public static String toJson(DeploymentPlan plan) {
        try {
            ObjectNode node = MAPPER.valueToTree(plan);
            node.put("schemaVersion", SCHEMA_VERSION);
            node.put("planId", planId(plan));
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("A DeploymentPlan is always serializable: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a plan document and checks its integrity.
     *
     * @param json the document
     * @return the plan, without a Fabric client bound
     * @throws IllegalArgumentException when the document is unreadable, of another schema version, or
     *                                  its stored {@code planId} does not match its content
     */
    public static DeploymentPlan fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("The plan document is empty");
        }
        try {
            JsonNode tree = MAPPER.readTree(json);
            String version = tree.path("schemaVersion").asText(SCHEMA_VERSION);
            if (!SCHEMA_VERSION.equals(version)) {
                throw new IllegalArgumentException("Unsupported plan schemaVersion '" + version + "'; this release reads '"
                        + SCHEMA_VERSION + "'");
            }
            DeploymentPlan plan = MAPPER.treeToValue(tree, DeploymentPlan.class);
            String stored = tree.path("planId").asText(null);
            String actual = planId(plan);
            if (stored != null && !stored.equals(actual)) {
                throw new IllegalArgumentException("The plan document's planId " + stored + " does not match its "
                        + "content (" + actual + "): the file was edited or is corrupt. Re-plan, or remove the "
                        + "planId field to accept the edited content as a new plan.");
            }
            return plan;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Not a readable plan document: " + e.getOriginalMessage(), e);
        }
    }

    /**
     * Writes a plan document atomically (a temporary file in the same directory, then a rename).
     *
     * @param plan the plan
     * @param file the destination, replaced if present
     * @throws UncheckedIOException when the file cannot be written
     */
    public static void save(DeploymentPlan plan, Path file) {
        writeAtomically(file, toJson(plan));
    }

    /**
     * Reads a plan document.
     *
     * @param file the file (UTF-8 JSON)
     * @return the plan, without a Fabric client bound
     * @throws UncheckedIOException     when the file cannot be read
     * @throws IllegalArgumentException when the content is not a valid plan document
     */
    public static DeploymentPlan load(Path file) {
        try {
            return fromJson(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read plan " + file, e);
        }
    }

    /**
     * The plan's content identity; see the class description.
     *
     * @param plan the plan
     * @return 16 lowercase hex characters, equal for plans that would build the same thing
     */
    public static String planId(DeploymentPlan plan) {
        Map<String, Object> desired = new java.util.LinkedHashMap<>();
        desired.put("deploymentName", plan.getDeploymentName());
        desired.put("namePrefix", plan.getNamePrefix());
        desired.put("projectId", plan.getProjectId());
        desired.put("specFingerprint", plan.getSpecFingerprint());
        desired.put("cloudRouters", plan.getCloudRouters());
        desired.put("providerConnections", plan.getProviderConnections());
        desired.put("backboneLinks", plan.getBackboneLinks());
        desired.put("routingProtocols", plan.getRoutingProtocols());
        desired.put("interconnects", plan.getInterconnects());
        desired.put("multicloudLinks", plan.multicloudLinksOrEmpty().stream()
                .map(l -> List.of(String.valueOf(l.getName()), String.valueOf(l.getRole()),
                        String.valueOf(l.getProviderA()), String.valueOf(l.getRegionA()),
                        String.valueOf(l.getProviderZ()), String.valueOf(l.getRegionZ()),
                        String.valueOf(l.getCoveringTierMbps())))
                .toList());
        try {
            byte[] canonical = CANONICAL.writeValueAsBytes(desired);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical);
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("A plan's desired state is always serializable", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every Java runtime", e);
        }
    }

    static void writeAtomically(Path file, String content) {
        try {
            Path dir = file.toAbsolutePath().getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            Path tmp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    // Mix-ins for types outside this package that carry no Jackson creator

    abstract static class MetroIdMixin {
        @JsonCreator
        static MetroId of(String code) {
            return MetroId.of(code);
        }

        @JsonValue
        abstract String code();
    }

    /**
     * Types outside this package that are built only through a Lombok builder and carry no Jackson
     * creator: Jackson is pointed at their builder ({@code XBuilder}, setters without a prefix).
     */
    private static final Set<Class<?>> BUILDER_TYPES = Set.of(MulticloudEnvironment.class, EnvironmentRef.class,
            ProviderSite.class, MulticloudLinkQuote.class, PriceQuote.class);

    private static final class LombokBuilderIntrospector extends JacksonAnnotationIntrospector {

        private static final long serialVersionUID = 1L;

        @Override
        public Class<?> findPOJOBuilder(AnnotatedClass ac) {
            Class<?> raw = ac.getRawType();
            if (BUILDER_TYPES.contains(raw)) {
                for (Class<?> nested : raw.getDeclaredClasses()) {
                    if (nested.getSimpleName().equals(raw.getSimpleName() + "Builder")) {
                        return nested;
                    }
                }
            }
            return super.findPOJOBuilder(ac);
        }

        @Override
        public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
            Class<?> enclosing = ac.getRawType().getEnclosingClass();
            if (enclosing != null && BUILDER_TYPES.contains(enclosing)) {
                return new JsonPOJOBuilder.Value("build", "");
            }
            return super.findPOJOBuilderConfig(ac);
        }
    }
}
