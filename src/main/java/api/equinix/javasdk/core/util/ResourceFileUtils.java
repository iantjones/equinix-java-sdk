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

package api.equinix.javasdk.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * <p>ResourceFileUtils class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ResourceFileUtils {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * <p>loadResourceFileJson.</p>
     *
     * @param fileName a {@link java.lang.String} object.
     * @return a {@link com.fasterxml.jackson.databind.JsonNode} object.
     * @throws java.io.IOException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws java.lang.NullPointerException if any.
     */
    public static JsonNode loadResourceFileJson(String fileName) throws IOException, URISyntaxException, NullPointerException {

        File jsonFile = null;
        URL resourceFileUrl = ResourceFileUtils.class.getClassLoader().getResource(fileName);

        if(resourceFileUrl != null) {
            jsonFile = Path.of(resourceFileUrl.toURI()).toFile();
        }

        if(jsonFile != null) {
            return jsonMapper.readTree(loadFileContents(jsonFile));
        }
        else {
            return null;
        }
    }

    private static String loadFileContents(File resourceFile) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(resourceFile.getPath()), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }

        return contentBuilder.toString();
    }
}
