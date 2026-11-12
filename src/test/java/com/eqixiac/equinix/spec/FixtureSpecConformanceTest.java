package com.eqixiac.equinix.spec;

import com.eqixiac.equinix.spec.OpenApiSubsetValidator.Violation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Fixture truth: every JSON fixture under {@code src/test/resources/json} must match the OpenAPI
 * schema of the response (or request) it stands for, in the specifications vendored under
 * {@code src/test/resources/specs} (fetched from docs.equinix.com/.well-known/api-catalog on
 * 2026-11-11; Fabric v4 4.34.0).
 *
 * <ul>
 *     <li>{@code specs/fixture-manifest.json} maps each fixture to {@code spec} + {@code schema}
 *         (a JSON pointer), or marks it {@code validate=false} WITH a reason (external APIs,
 *         endpoints the spec omits, SDK-internal resources). A fixture with no entry fails the
 *         test: nothing is skipped silently.</li>
 *     <li>{@code specs/fixture-deviations.json} lists documented live-vs-spec deviations (the
 *         fixture follows the live service, or is the spec's own example) with reason and date.</li>
 *     <li>{@code specs/fixture-pending-review.json} is the baseline of untriaged violations from
 *         the first sweep (2026-11-12). It may only shrink.</li>
 * </ul>
 *
 * <p>The test fails on any NEW violation, on any allow-list entry that no longer matches a
 * violation (stale: delete it), and on entries without a reason or date.</p>
 */
@DisplayName("Fixtures conform to the vendored OpenAPI specifications")
class FixtureSpecConformanceTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private static JsonNode manifest;
    private static Path fixtureRoot;
    private static final Map<String, OpenApiSubsetValidator> validators = new HashMap<>();

    @BeforeAll
    static void load() throws IOException, URISyntaxException {
        manifest = readResource("/specs/fixture-manifest.json").get("fixtures");
        assertNotNull(manifest, "fixture-manifest.json has no 'fixtures' object");
        fixtureRoot = Path.of(FixtureSpecConformanceTest.class.getResource("/json").toURI());
    }

    private static JsonNode readResource(String path) throws IOException {
        try (InputStream in = FixtureSpecConformanceTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing classpath resource " + path);
            return (path.endsWith(".yaml") || path.endsWith(".yml") ? YAML : JSON).readTree(in);
        }
    }

    private static OpenApiSubsetValidator validator(String spec) {
        return validators.computeIfAbsent(spec, s -> {
            try {
                return new OpenApiSubsetValidator(readResource("/specs/" + s));
            } catch (IOException e) {
                throw new IllegalStateException("cannot read spec " + s, e);
            }
        });
    }

    private static Set<String> fixtureFiles() throws IOException {
        Set<String> files = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(fixtureRoot)) {
            walk.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> files.add(fixtureRoot.relativize(p).toString().replace('\\', '/')));
        }
        return files;
    }

    private static String key(String fixture, String pointer, String kind) {
        return fixture + " " + pointer + " " + kind;
    }

    private static Map<String, JsonNode> allowList(String resource, String arrayName) throws IOException {
        Map<String, JsonNode> entries = new HashMap<>();
        for (JsonNode e : readResource(resource).path(arrayName)) {
            entries.put(key(e.path("fixture").asText(), e.path("pointer").asText(), e.path("kind").asText()), e);
        }
        return entries;
    }

    @Test
    @DisplayName("every fixture has a manifest entry, and every entry names an existing fixture")
    void manifestCoversEveryFixture() throws IOException {
        Set<String> files = fixtureFiles();
        Set<String> mapped = new TreeSet<>();
        manifest.fieldNames().forEachRemaining(mapped::add);

        Set<String> unmapped = new TreeSet<>(files);
        unmapped.removeAll(mapped);
        Set<String> orphaned = new TreeSet<>(mapped);
        orphaned.removeAll(files);
        if (!unmapped.isEmpty() || !orphaned.isEmpty()) {
            fail("Fixtures without a manifest entry (map them to a spec schema, or set validate=false with a reason): "
                    + unmapped + "\nManifest entries without a fixture: " + orphaned);
        }
    }

    @Test
    @DisplayName("every validate=false entry says why, and every mapped entry names a vendored spec")
    void manifestEntriesAreExplained() {
        List<String> problems = new ArrayList<>();
        List<String> notValidated = new ArrayList<>();
        manifest.fields().forEachRemaining(e -> {
            JsonNode entry = e.getValue();
            if (entry.has("validate") && !entry.get("validate").asBoolean()) {
                if (entry.path("reason").asText("").isBlank()) {
                    problems.add(e.getKey() + ": validate=false without a reason");
                }
                notValidated.add(e.getKey() + " - " + entry.path("reason").asText());
            } else if (entry.path("spec").asText("").isBlank() || !entry.path("schema").asText("").startsWith("#/")) {
                problems.add(e.getKey() + ": needs spec and a #/ schema pointer");
            } else if (FixtureSpecConformanceTest.class.getResource("/specs/" + entry.get("spec").asText()) == null) {
                problems.add(e.getKey() + ": spec " + entry.get("spec").asText() + " is not vendored under specs/");
            }
        });
        System.out.println("[fixture-spec] fixtures not validated against a spec (" + notValidated.size() + "):");
        notValidated.forEach(n -> System.out.println("    " + n));
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    @DisplayName("fixtures match their spec schema, except documented deviations and the pending-review baseline")
    void fixturesConformToTheirSchemas() throws IOException {
        Map<String, JsonNode> deviations = allowList("/specs/fixture-deviations.json", "deviations");
        Map<String, JsonNode> pending = allowList("/specs/fixture-pending-review.json", "pending");

        List<String> problems = new ArrayList<>();
        for (Map<String, JsonNode> list : List.of(deviations, pending)) {
            list.forEach((k, e) -> {
                if (e.path("reason").asText("").isBlank() || e.path("date").asText("").isBlank()) {
                    problems.add("allow-list entry without reason/date: " + k);
                }
            });
        }

        Set<String> found = new LinkedHashSet<>();
        Map<String, String> details = new HashMap<>();
        var fields = manifest.fields();
        while (fields.hasNext()) {
            var e = fields.next();
            JsonNode entry = e.getValue();
            if (entry.has("validate") && !entry.get("validate").asBoolean()) {
                continue;
            }
            JsonNode fixture = JSON.readTree(fixtureRoot.resolve(e.getKey()).toFile());
            List<Violation> violations = validator(entry.get("spec").asText())
                    .validate(fixture, entry.get("schema").asText());
            for (Violation v : violations) {
                String k = key(e.getKey(), v.normalizedPointer(), v.kind());
                if (found.add(k)) {
                    details.put(k, v.detail() + " [" + entry.get("spec").asText() + " " + entry.get("schema").asText() + "]");
                }
            }
        }

        List<String> fresh = new ArrayList<>();
        for (String k : found) {
            if (!deviations.containsKey(k) && !pending.containsKey(k)) {
                fresh.add(k + " - " + details.get(k));
            }
        }
        List<String> stale = new ArrayList<>();
        for (Map<String, JsonNode> list : List.of(deviations, pending)) {
            for (String k : list.keySet()) {
                if (!found.contains(k)) {
                    stale.add(k);
                }
            }
        }
        System.out.printf("[fixture-spec] %d violations: %d documented deviations, %d pending review, %d new%n",
                found.size(), deviations.size(), pending.size(), fresh.size());
        if (!fresh.isEmpty()) {
            problems.add("Fixtures violate their spec schema (fix the fixture, or document a LIVE deviation in "
                    + "specs/fixture-deviations.json with reason and date):\n  " + String.join("\n  ", fresh));
        }
        if (!stale.isEmpty()) {
            problems.add("Allow-list entries that no longer match a violation (delete them):\n  "
                    + String.join("\n  ", stale));
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    @DisplayName("the pending-review baseline only shrinks")
    void pendingBaselineDoesNotGrow() throws IOException {
        int pending = readResource("/specs/fixture-pending-review.json").path("pending").size();
        // Baseline recorded 2026-11-12. Lower this number when entries are triaged; never raise it.
        assertFalse(pending > PENDING_BASELINE,
                "fixture-pending-review.json grew to " + pending + " entries (baseline " + PENDING_BASELINE
                        + "): new fixtures must conform to the spec, not join the baseline");
    }

    /** Size of the untriaged baseline on 2026-11-12; ratchets down only. */
    static final int PENDING_BASELINE = 231;
}
