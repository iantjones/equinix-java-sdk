package api.equinix.javasdk.mcp.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression lock for the pinned Fabric MCP tool catalog
 * ({@code src/main/resources/mcp-catalog.json}).
 *
 * <p>The manifest is a verbatim transcription of the official Fabric MCP Server
 * documentation (docs.equinix.com, pinned 2026-07-20). These tests hold three lines:
 * <ol>
 *     <li><b>Manifest integrity</b> - the file is well-formed, every tool has a
 *         snake_case name, description, and apiEndpoint, names are globally unique,
 *         and the pinned tool count is exact. A drive-by edit cannot silently
 *         corrupt or shrink the contract.</li>
 *     <li><b>Documented-name lock</b> - the historically miscarried tool names
 *         (e.g. {@code list_metro}, {@code search_connection}, {@code validate_connection})
 *         are banned, and their documented replacements must be present. Nobody can
 *         "fix" a failing bridge check by pinning a fictional name.</li>
 *     <li><b>Bridge cross-check</b> - every string literal passed to
 *         {@code callTool("...")} anywhere under {@code src/main/java} must exist in
 *         the manifest. Adding a bridge method against an undocumented tool name
 *         fails loudly here, pointing at the offending file.</li>
 * </ol>
 *
 * <p>If a test in this class fails, the fix is either to correct the bridge code to a
 * documented tool name, or - when Equinix genuinely ships new tools - to re-pin the
 * manifest from the docs page (and bump {@code toolCount} + {@code EXPECTED_TOOL_COUNT}
 * deliberately). Never edit the manifest just to make a test pass.</p>
 */
class McpCatalogTest {

    /**
     * Exact number of tools documented for the Fabric MCP Server as of the 2026-07-20 pin.
     * Update ONLY together with a deliberate re-pin of mcp-catalog.json from the docs.
     */
    private static final int EXPECTED_TOOL_COUNT = 109;

    private static final String MANIFEST_RESOURCE = "/mcp-catalog.json";

    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

    /**
     * Matches the first string-literal argument of {@code callTool("tool_name", ...)}.
     * Deliberately does NOT match {@code callPeeringTool(...)} - the Peering Insights
     * server has its own (undocumented) catalog and is out of scope for this manifest.
     */
    private static final Pattern CALL_TOOL_PATTERN =
            Pattern.compile("(?<![A-Za-z0-9_])callTool\\(\\s*\"([A-Za-z0-9_]+)\"");

    /**
     * Documented replacements for tool names the SDK bridges historically got wrong.
     * Every one of these MUST be pinned in the manifest.
     */
    private static final Set<String> DOCUMENTED_CORRECTED_NAMES = Set.of(
            "list_metros",              // was: list_metro
            "search_connections",       // was: search_connection
            "check_connection",         // was: validate_connection
            "search_routers",           // was: search_router
            "list_router_packages",     // was: get_router_package
            "get_metric",               // was: get_metrics (per-asset metrics)
            "get_metro",
            "create_router_commands",
            "search_prices",
            "search_metrics",
            "list_metrics_by_metros",
            "search_cloud_events",
            "search_cloud_events_by_asset",
            "list_streams",
            "list_stream_alert_rules");

    /**
     * Known-wrong legacy names. These must NEVER be pinned - if the bridge cross-check
     * fails on one of them, fix the bridge, do not add the name here.
     */
    private static final Set<String> BANNED_LEGACY_NAMES = Set.of(
            "list_metro",
            "search_connection",
            "validate_connection",
            "search_router",
            "get_router_package",
            "get_metrics");

    private static JsonNode manifest;

    @BeforeAll
    static void loadManifest() throws IOException {
        try (InputStream in = McpCatalogTest.class.getResourceAsStream(MANIFEST_RESOURCE)) {
            assertNotNull(in, "Pinned MCP catalog manifest " + MANIFEST_RESOURCE
                    + " is missing from the classpath (expected at src/main/resources/mcp-catalog.json).");
            manifest = new ObjectMapper().readTree(in);
        }
    }

    @Test
    @DisplayName("Manifest is well-formed: families and tools carry name/description/apiEndpoint")
    void manifestIsWellFormed() {
        JsonNode families = manifest.get("families");
        assertNotNull(families, "Manifest must have a top-level 'families' array.");
        assertTrue(families.isArray() && families.size() > 0, "'families' must be a non-empty array.");

        Set<String> familyNames = new LinkedHashSet<>();
        for (JsonNode family : families) {
            JsonNode familyName = family.get("family");
            assertNotNull(familyName, "Every family entry must have a 'family' name: " + family);
            assertFalse(familyName.asText().isBlank(), "Family name must not be blank.");
            assertTrue(familyNames.add(familyName.asText()),
                    "Duplicate family name in manifest: " + familyName.asText());

            JsonNode tools = family.get("tools");
            assertNotNull(tools, "Family '" + familyName.asText() + "' must have a 'tools' array.");
            assertTrue(tools.isArray() && tools.size() > 0,
                    "Family '" + familyName.asText() + "' must have a non-empty 'tools' array.");

            for (JsonNode tool : tools) {
                String context = "family '" + familyName.asText() + "', tool " + tool;
                for (String field : new String[]{"name", "description", "apiEndpoint"}) {
                    JsonNode value = tool.get(field);
                    assertNotNull(value, "Missing '" + field + "' in " + context);
                    assertFalse(value.asText().isBlank(), "Blank '" + field + "' in " + context);
                }
                String name = tool.get("name").asText();
                assertTrue(TOOL_NAME_PATTERN.matcher(name).matches(),
                        "Tool name '" + name + "' is not snake_case (" + context + ").");
            }
        }
    }

    @Test
    @DisplayName("Tool names are globally unique across families")
    void toolNamesAreGloballyUnique() {
        List<String> allNames = allPinnedToolNames();
        Set<String> seen = new LinkedHashSet<>();
        List<String> duplicates = allNames.stream()
                .filter(name -> !seen.add(name))
                .collect(Collectors.toList());
        assertTrue(duplicates.isEmpty(), "Duplicate tool names pinned in manifest: " + duplicates);
    }

    @Test
    @DisplayName("Pinned tool count is exact (deliberate re-pin required to change it)")
    void pinnedToolCountIsExact() {
        List<String> allNames = allPinnedToolNames();
        assertEquals(EXPECTED_TOOL_COUNT, allNames.size(),
                "The pinned Fabric MCP catalog must contain exactly " + EXPECTED_TOOL_COUNT
                        + " tools (2026-07-20 docs pin). If Equinix documented new tools, re-pin"
                        + " mcp-catalog.json from the docs AND update EXPECTED_TOOL_COUNT together.");

        JsonNode declaredCount = manifest.get("toolCount");
        assertNotNull(declaredCount, "Manifest must declare its own 'toolCount'.");
        assertEquals(allNames.size(), declaredCount.asInt(),
                "Manifest 'toolCount' (" + declaredCount.asInt() + ") disagrees with the actual"
                        + " number of pinned tools (" + allNames.size() + ") - fix the manifest.");
    }

    @Test
    @DisplayName("Documented corrected tool names are all pinned")
    void documentedCorrectedNamesArePresent() {
        Set<String> pinned = new LinkedHashSet<>(allPinnedToolNames());
        List<String> missing = DOCUMENTED_CORRECTED_NAMES.stream()
                .filter(name -> !pinned.contains(name))
                .sorted()
                .collect(Collectors.toList());
        assertTrue(missing.isEmpty(),
                "Documented tool names missing from the pinned manifest: " + missing
                        + ". These come straight from the Fabric MCP docs - restore them.");
    }

    @Test
    @DisplayName("Known-wrong legacy tool names are not pinned")
    void bannedLegacyNamesAreAbsent() {
        Set<String> pinned = new LinkedHashSet<>(allPinnedToolNames());
        List<String> offenders = BANNED_LEGACY_NAMES.stream()
                .filter(pinned::contains)
                .sorted()
                .collect(Collectors.toList());
        assertTrue(offenders.isEmpty(),
                "Legacy (never-documented) tool names found in the pinned manifest: " + offenders
                        + ". The manifest mirrors the docs; do not pin fictional names to make"
                        + " a bridge test pass - fix the bridge instead.");
    }

    @Test
    @DisplayName("No delete tools pinned (docs: delete not supported by the Fabric MCP Server)")
    void noDeleteToolsPinned() {
        List<String> deleteTools = allPinnedToolNames().stream()
                .filter(name -> name.startsWith("delete"))
                .collect(Collectors.toList());
        assertTrue(deleteTools.isEmpty(),
                "The Fabric MCP docs state delete functionality is not supported, but the"
                        + " manifest pins: " + deleteTools + ". Re-verify against the docs before pinning.");
    }

    @Test
    @DisplayName("Every callTool(\"...\") literal in src/main/java is a pinned, documented tool name")
    void everyBridgeReferencedToolNameIsPinned() throws IOException {
        Path mainSources = Path.of("src", "main", "java");
        assertTrue(Files.isDirectory(mainSources),
                "Cannot find " + mainSources.toAbsolutePath() + " - this scan assumes surefire's"
                        + " working directory is the project basedir. If the module layout moved,"
                        + " update McpCatalogTest, do not delete this check.");

        Map<String, Set<String>> referencedNamesToFiles = scanCallToolLiterals(mainSources);
        assertFalse(referencedNamesToFiles.isEmpty(),
                "The source scan found no callTool(\"...\") literals under " + mainSources
                        + ". The MCP bridges are known to call tools, so the scan mechanism itself"
                        + " has rotted (regex or path) - fix the scan, do not weaken this test.");

        Set<String> pinned = new LinkedHashSet<>(allPinnedToolNames());
        List<String> report = new ArrayList<>();
        for (String name : new TreeSet<>(referencedNamesToFiles.keySet())) {
            if (!pinned.contains(name)) {
                report.add("  '" + name + "' referenced by " + referencedNamesToFiles.get(name));
            }
        }

        assertTrue(report.isEmpty(),
                "Tool name(s) referenced in main sources but NOT pinned in mcp-catalog.json:\n"
                        + String.join("\n", report)
                        + "\nEither the bridge drifted from the documented catalog (fix the bridge"
                        + " to use the documented name), or Equinix documented a new tool (re-pin"
                        + " src/main/resources/mcp-catalog.json from the docs page).");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static List<String> allPinnedToolNames() {
        List<String> names = new ArrayList<>();
        JsonNode families = manifest.get("families");
        if (families != null && families.isArray()) {
            for (JsonNode family : families) {
                JsonNode tools = family.get("tools");
                if (tools != null && tools.isArray()) {
                    for (JsonNode tool : tools) {
                        JsonNode name = tool.get("name");
                        if (name != null) {
                            names.add(name.asText());
                        }
                    }
                }
            }
        }
        return names;
    }

    /**
     * Scans every .java file under {@code root} for {@code callTool("...")} string
     * literals, ignoring comments (so javadoc examples do not count as references).
     *
     * @return map of tool name to the set of repo-relative files referencing it
     */
    private static Map<String, Set<String>> scanCallToolLiterals(Path root) throws IOException {
        Map<String, Set<String>> found = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        String source;
                        try {
                            source = Files.readString(path, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed reading " + path, e);
                        }
                        Matcher matcher = CALL_TOOL_PATTERN.matcher(stripComments(source));
                        while (matcher.find()) {
                            found.computeIfAbsent(matcher.group(1), key -> new TreeSet<>())
                                    .add(root.relativize(path).toString().replace('\\', '/'));
                        }
                    });
        }
        return found;
    }

    /** Removes block comments (incl. javadoc) and line comments from Java source. */
    private static String stripComments(String source) {
        return source
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("//[^\\n]*", " ");
    }
}
