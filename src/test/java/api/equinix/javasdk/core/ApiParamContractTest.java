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

package api.equinix.javasdk.core;

import api.equinix.javasdk.core.model.APIParam;
import com.fasterxml.jackson.annotation.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture test enforcing the {@link APIParam} contract across every implementor on the
 * classpath: a value must have exactly ONE wire form, whether it travels in a request body
 * (governed by the Jackson {@code @JsonValue} accessor) or in a query/path parameter (governed by
 * {@link APIParam#paramValue()}).
 *
 * <p>Historically the interface only <em>documented</em> that {@code toString()} is the wire form
 * — which the compiler cannot enforce, so an enum whose wire code differs from its constant name
 * could silently send {@code name()} in query parameters while sending the {@code @JsonValue}
 * code in bodies. This test turns that convention into an invariant: it scans all
 * {@code api.equinix.javasdk} classes, and for every {@code APIParam} enum with a
 * {@code @JsonValue} accessor, asserts {@code paramValue()} equals the accessor's value for every
 * constant.</p>
 */
class ApiParamContractTest {

    private static final String ROOT_PACKAGE = "api.equinix.javasdk";

    @Test
    @DisplayName("every APIParam enum's paramValue() agrees with its @JsonValue body form")
    void paramValueMatchesJsonValue() throws Exception {
        List<Class<?>> implementors = scanApiParamTypes();
        assertTrue(implementors.size() > 100,
                "classpath scan should find the ~156 APIParam implementors, found " + implementors.size());

        List<String> violations = new ArrayList<>();
        int checkedEnums = 0;
        for (Class<?> type : implementors) {
            if (!type.isEnum()) {
                continue;
            }
            Method jsonValueAccessor = findJsonValueAccessor(type);
            if (jsonValueAccessor == null) {
                continue;
            }
            checkedEnums++;
            for (Object constant : type.getEnumConstants()) {
                Object bodyForm = jsonValueAccessor.invoke(constant);
                String paramForm = ((APIParam) constant).paramValue();
                if (bodyForm != null && !String.valueOf(bodyForm).equals(paramForm)) {
                    violations.add(type.getSimpleName() + "." + constant + ": body='" + bodyForm
                            + "' vs param='" + paramForm + "'");
                }
            }
        }

        assertTrue(checkedEnums > 0, "expected at least one APIParam enum with a @JsonValue accessor");
        assertFalse(!violations.isEmpty(),
                "APIParam enums whose query-parameter form diverges from their @JsonValue body form"
                        + " (override toString()/paramValue() to return the wire code):\n  "
                        + String.join("\n  ", violations));
    }

    private static Method findJsonValueAccessor(Class<?> type) {
        for (Method m : type.getDeclaredMethods()) {
            if (m.isAnnotationPresent(JsonValue.class) && m.getParameterCount() == 0) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static List<Class<?>> scanApiParamTypes() throws IOException {
        List<Class<?>> found = new ArrayList<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> roots = cl.getResources(ROOT_PACKAGE.replace('.', '/'));
        while (roots.hasMoreElements()) {
            URL url = roots.nextElement();
            if (!"file".equals(url.getProtocol())) {
                continue;
            }
            Path root;
            try {
                root = Path.of(url.toURI());
            } catch (Exception e) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(p -> p.toString().endsWith(".class"))
                        .forEach(p -> {
                            String rel = root.relativize(p).toString()
                                    .replace(java.io.File.separatorChar, '.')
                                    .replaceAll("\\.class$", "");
                            try {
                                Class<?> type = Class.forName(ROOT_PACKAGE + "." + rel, false, cl);
                                if (APIParam.class.isAssignableFrom(type) && type != APIParam.class) {
                                    found.add(type);
                                }
                            } catch (Throwable ignored) {
                                // unloadable class (missing optional dep etc.) — not an APIParam concern
                            }
                        });
            }
        }
        return found;
    }
}
