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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture test locking the dependency hygiene of the embedded MCP server: this SDK is a
 * <em>library</em>, and the MCP server it embeds must never leak its runtime into consumers'
 * dependency trees.
 *
 * <p>The contract, enforced against the actual {@code pom.xml}:</p>
 * <ul>
 *   <li>Both {@code io.modelcontextprotocol.sdk} artifacts ({@code mcp-core},
 *       {@code mcp-json-jackson2}) are declared {@code <optional>true</optional>}. Optional
 *       dependencies stay on this project's own classpath (so the server compiles and the
 *       shaded {@code mcp-server} jar bundles them) but are NOT resolved transitively — an
 *       application that depends on {@code equinix-sdk-java} gets no MCP SDK, no reactor-core,
 *       no json-schema-validator.</li>
 *   <li>{@code slf4j-simple} — bundled into the shaded server jar as its logging backend — is
 *       likewise {@code <optional>true</optional>}: a library must expose only the slf4j-api
 *       facade, never impose a concrete logging binding on its consumers.</li>
 * </ul>
 */
class DependencyHygieneTest {

    private static final String MCP_GROUP_ID = "io.modelcontextprotocol.sdk";

    private static Element dependenciesElement;

    @BeforeAll
    static void parsePom() throws Exception {
        Path pom = Path.of(System.getProperty("user.dir", "."), "pom.xml");
        assertTrue(Files.isRegularFile(pom),
                "pom.xml not found at " + pom + " — this test must run from the project basedir "
                        + "(surefire's default working directory).");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document document = factory.newDocumentBuilder().parse(pom.toFile());

        // The project-level <dependencies> is a direct child of <project> (as opposed to
        // plugin-level or profile-level dependency blocks).
        Element project = document.getDocumentElement();
        dependenciesElement = directChild(project, "dependencies");
        assertTrue(dependenciesElement != null, "pom.xml has no top-level <dependencies> element.");
    }

    @Test
    @DisplayName("both io.modelcontextprotocol.sdk dependencies are <optional>true</optional>")
    void mcpSdkDependenciesAreOptional() {
        List<Element> mcpDependencies = dependencies().stream()
                .filter(d -> MCP_GROUP_ID.equals(childText(d, "groupId")))
                .toList();

        assertEquals(2, mcpDependencies.size(),
                "Expected exactly two " + MCP_GROUP_ID + " dependencies (mcp-core and "
                        + "mcp-json-jackson2), found " + mcpDependencies.size() + ". The embedded MCP "
                        + "server's footprint in the library pom is deliberately these two artifacts; "
                        + "anything more (e.g. the 'mcp' aggregate, which pulls Jackson 3) widens what "
                        + "the shaded server jar drags in.");

        List<String> artifactIds = mcpDependencies.stream().map(d -> childText(d, "artifactId")).toList();
        assertTrue(artifactIds.contains("mcp-core") && artifactIds.contains("mcp-json-jackson2"),
                "Expected the two " + MCP_GROUP_ID + " dependencies to be mcp-core and "
                        + "mcp-json-jackson2, found " + artifactIds + ".");

        for (Element dependency : mcpDependencies) {
            assertOptional(dependency,
                    "consumers of equinix-sdk-java must not inherit the MCP SDK (nor its "
                            + "reactor-core / json-schema-validator transitives) just because this "
                            + "library embeds an MCP server. The runnable server ships as the shaded "
                            + "'mcp-server' classifier jar, which bundles these; the library jar's "
                            + "dependency tree must stay clean.");
        }
    }

    @Test
    @DisplayName("slf4j-simple (bundled into the shaded server jar) is <optional>true</optional>")
    void slf4jSimpleIsOptional() {
        List<Element> matches = dependencies().stream()
                .filter(d -> "org.slf4j".equals(childText(d, "groupId"))
                        && "slf4j-simple".equals(childText(d, "artifactId")))
                .toList();
        assertEquals(1, matches.size(), "Expected exactly one org.slf4j:slf4j-simple dependency "
                + "(runtime + optional: the test suites' and the shaded mcp-server jar's logging "
                + "backend), found " + matches.size() + ".");
        assertOptional(matches.get(0),
                "a library must expose only the slf4j-api facade; shipping a concrete logging "
                        + "BINDING transitively would hijack consumers' logging. slf4j-simple exists "
                        + "here solely for the test suites and for bundling into the shaded "
                        + "'mcp-server' jar (which is why it cannot be test scope).");
    }

    private static void assertOptional(Element dependency, String why) {
        String coordinates = childText(dependency, "groupId") + ":" + childText(dependency, "artifactId");
        assertEquals("true", childText(dependency, "optional"),
                coordinates + " must be declared <optional>true</optional>: " + why);
    }

    private static List<Element> dependencies() {
        List<Element> result = new ArrayList<>();
        NodeList children = dependenciesElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "dependency".equals(node.getNodeName())) {
                result.add((Element) node);
            }
        }
        return result;
    }

    private static Element directChild(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private static String childText(Element parent, String name) {
        Element child = directChild(parent, name);
        return child == null ? null : child.getTextContent().trim();
    }
}
