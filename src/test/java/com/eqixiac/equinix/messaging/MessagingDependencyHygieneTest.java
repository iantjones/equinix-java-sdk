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

package com.eqixiac.equinix.messaging;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the EMG dependency boundary: the Azure Service Bus client is {@code <optional>} in the pom,
 * and only {@code messaging.transport.servicebus} imports it, so the models, parser, handler and
 * listener work without it on the classpath.
 */
class MessagingDependencyHygieneTest {

    private static final Path BASE = Path.of(System.getProperty("user.dir", "."));

    @Test
    void serviceBusDependencyIsOptional() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Element project = factory.newDocumentBuilder().parse(BASE.resolve("pom.xml").toFile()).getDocumentElement();
        Element dependencies = child(project, "dependencies");
        int found = 0;
        NodeList list = dependencies.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i) instanceof Element dep && "dependency".equals(dep.getNodeName())
                    && "com.azure".equals(text(dep, "groupId"))) {
                found++;
                assertEquals("azure-messaging-servicebus", text(dep, "artifactId"),
                        "the only com.azure dependency should be the Service Bus client");
                assertEquals("true", text(dep, "optional"),
                        "azure-messaging-servicebus must be <optional>true</optional>: SDK users who do not "
                                + "consume EMG must not inherit it and its Netty/Reactor/proton-j transitives");
            }
        }
        assertEquals(1, found, "expected exactly one com.azure dependency");
    }

    @Test
    void onlyTheServiceBusPackageImportsAzure() throws IOException {
        Path root = BASE.resolve("src/main/java/com/eqixiac/equinix");
        Path allowed = root.resolve("messaging/transport/servicebus");
        List<Path> offenders;
        try (Stream<Path> files = Files.walk(root)) {
            offenders = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.startsWith(allowed))
                    .filter(MessagingDependencyHygieneTest::importsAzure)
                    .toList();
        }
        assertTrue(offenders.isEmpty(), "com.azure imported outside messaging.transport.servicebus: " + offenders);
    }

    private static boolean importsAzure(Path file) {
        try {
            return Files.readString(file).contains("import com.azure.");
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static Element child(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element e && name.equals(e.getNodeName())) {
                return e;
            }
        }
        throw new AssertionError("no <" + name + ">");
    }

    private static String text(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element e && name.equals(e.getNodeName())) {
                return e.getTextContent().trim();
            }
        }
        return null;
    }
}
