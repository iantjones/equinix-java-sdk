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

package com.eqixiac.equinix.core.model.multicloud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Package-level promises of {@code core.model.multicloud}: the dependency boundary stated in its
 * {@code package-info.java}, and the package-info example.
 */
@DisplayName("core.model.multicloud package contracts")
class MulticloudPackageContractTest {

    private static final Path PACKAGE_SOURCES =
            Paths.get("src", "main", "java", "com", "eqixiac", "equinix", "core", "model", "multicloud");

    /** Any reference to an SDK package other than core, in an import or a fully-qualified name. */
    private static final Pattern NON_CORE_SDK_REFERENCE =
            Pattern.compile("com\\.eqixiac\\.equinix\\.(?!core\\.)[A-Za-z_]");

    @Test
    @DisplayName("doc contract: 'It imports nothing outside core, the JDK, Jackson and Lombok'")
    void importsNothingOutsideCore() throws IOException {
        assumeTrue(Files.isDirectory(PACKAGE_SOURCES),
                "source tree not present relative to the working directory: " + PACKAGE_SOURCES.toAbsolutePath());

        List<Path> sources;
        try (Stream<Path> files = Files.list(PACKAGE_SOURCES)) {
            sources = files.filter(p -> p.getFileName().toString().endsWith(".java")).collect(Collectors.toList());
        }
        assertFalse(sources.isEmpty(), "no sources found under " + PACKAGE_SOURCES);

        Pattern allowedImport = Pattern.compile(
                "^import\\s+(static\\s+)?(java\\.|com\\.eqixiac\\.equinix\\.core\\.|com\\.fasterxml\\.jackson\\.|lombok\\.).*");
        List<String> violations = new ArrayList<>();
        for (Path source : sources) {
            int lineNumber = 0;
            for (String line : Files.readAllLines(source, StandardCharsets.UTF_8)) {
                lineNumber++;
                String code = line.trim();
                if (code.startsWith("*") || code.startsWith("/*") || code.startsWith("//")) {
                    continue; // javadoc and comments may name domain types; code may not
                }
                if (code.startsWith("import ") && !allowedImport.matcher(code).matches()) {
                    violations.add(source.getFileName() + ":" + lineNumber + " " + code);
                }
                Matcher reference = NON_CORE_SDK_REFERENCE.matcher(code);
                if (reference.find()) {
                    violations.add(source.getFileName() + ":" + lineNumber + " " + code);
                }
            }
        }
        assertTrue(violations.isEmpty(), "core.model.multicloud must not depend on a domain package: " + violations);
    }

    @Test
    @DisplayName("the package-info example compiles and produces the values its comments state")
    void packageInfoExample() {
        String keyFromProviderA = ActivationKey.V1.builder()
                .destinationEnvironmentUri("providers/gcp/environments/aws-gcp-us-east")
                .sharedConnectionUuid("3f2b8c1e-6d4a-4e7b-9a55-0c1d2e3f4a5b")
                .connectionSizeMbps(10000)
                .destinationAccountId("example-project-123456")
                .build()
                .encode();

        // --- begin: package-info.java example, verbatim ---
        EnvironmentRef env = EnvironmentRef.builder()
                .environmentId("aws-gcp-us-east")
                .providerSite(ProviderSite.of(ProviderRef.AWS, "us-east-1"))
                .providerSite(ProviderSite.of(ProviderRef.GCP, "us-east4"))
                .supportedConnectionSizeMbps(BandwidthTier.of(1000, 10000))
                .build();

        OptionalInt tier = env.getSupportedConnectionSizeMbps().coveringTier(3000);   // 10000

        ActivationKey key = ActivationKey.decode(keyFromProviderA);
        String summary = switch (key) {
            case ActivationKey.V1 v1 -> v1.connectionSizeMbps() + " Mbps to " + v1.destinationEnvironmentUri();
            case ActivationKey.V2Encrypted v2 -> "encrypted, for " + v2.destinationEnvironmentUri();
            case ActivationKey.Opaque opaque -> "format not recognized";
        };
        boolean sameEnvironment = key.destinationEnvironmentId().map(env.getEnvironmentId()::equals).orElse(false);
        String toEnterAtProviderB = key.encode();
        // --- end ---

        assertEquals(OptionalInt.of(10000), tier);
        assertEquals("10000 Mbps to providers/gcp/environments/aws-gcp-us-east", summary);
        assertTrue(sameEnvironment);
        assertEquals(keyFromProviderA, toEnterAtProviderB);
    }
}
