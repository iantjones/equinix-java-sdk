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

package com.eqixiac.equinix.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author ianjones
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceFileUtils {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Loads a classpath resource as JSON. The resource is read through an {@link InputStream}
     * so it resolves correctly whether it is a loose file on disk (e.g. running from an IDE)
     * or packed inside a jar (where the URL is a {@code jar:file:...!/...} entry that cannot be
     * turned into a {@link java.nio.file.Path}).
     *
     * @param fileName the classpath-relative resource name (e.g. {@code "json/apiParams_Core.json"})
     * @return the parsed JSON tree; never {@code null}
     * @throws FileNotFoundException if no such resource exists on the classpath — a missing
     *         resource fails fast with the resource name rather than surfacing later as an
     *         opaque NPE during client bootstrap
     * @throws IOException if the resource exists but cannot be read or parsed
     */
    public static JsonNode loadResourceFileJson(String fileName) throws IOException {

        try (InputStream resourceStream = ResourceFileUtils.class.getClassLoader().getResourceAsStream(fileName)) {
            if (resourceStream == null) {
                throw new FileNotFoundException("Classpath resource not found: " + fileName);
            }
            return jsonMapper.readTree(resourceStream);
        }
    }
}
