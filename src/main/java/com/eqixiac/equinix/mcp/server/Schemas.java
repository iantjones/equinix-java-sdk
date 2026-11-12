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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny hand-rolled JSON-Schema builders for the tool catalog. Every schema is a plain
 * {@code Map<String,Object>} in the 2020-12 dialect the MCP SDK defaults to; descriptions are
 * written for the calling LLM.
 */
final class Schemas {

    private Schemas() {
    }

    /** An object schema with named properties, a required list, and closed extra properties. */
    static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required.length > 0) {
            schema.put("required", new ArrayList<>(Arrays.asList(required)));
        }
        schema.put("additionalProperties", false);
        return schema;
    }

    /** A permissive object output schema: documented but open, so payload evolution never breaks validation. */
    static Map<String, Object> looseObject(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", description);
        schema.put("additionalProperties", true);
        return schema;
    }

    /** An insertion-ordered property map; pairs must alternate name, schema. */
    static Map<String, Object> props(Object... namesAndSchemas) {
        if (namesAndSchemas.length % 2 != 0) {
            throw new IllegalArgumentException("props() takes alternating name/schema pairs");
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < namesAndSchemas.length; i += 2) {
            properties.put((String) namesAndSchemas[i], namesAndSchemas[i + 1]);
        }
        return properties;
    }

    static Map<String, Object> string(String description) {
        return leaf("string", description);
    }

    static Map<String, Object> stringEnum(String description, String... values) {
        Map<String, Object> schema = leaf("string", description);
        schema.put("enum", new ArrayList<>(Arrays.asList(values)));
        return schema;
    }

    static Map<String, Object> number(String description) {
        return leaf("number", description);
    }

    static Map<String, Object> integer(String description) {
        return leaf("integer", description);
    }

    static Map<String, Object> bool(String description) {
        return leaf("boolean", description);
    }

    static Map<String, Object> array(String description, Map<String, Object> items) {
        Map<String, Object> schema = leaf("array", description);
        schema.put("items", items);
        return schema;
    }

    /**
     * Adds keywords to a schema ({@code minimum}, {@code maximum}, {@code minItems}, {@code default}, ...).
     *
     * @param schema        the schema to extend (modified and returned)
     * @param keywordsValues alternating keyword, value
     * @return {@code schema}
     */
    static Map<String, Object> with(Map<String, Object> schema, Object... keywordsValues) {
        if (keywordsValues.length % 2 != 0) {
            throw new IllegalArgumentException("with() takes alternating keyword/value pairs");
        }
        for (int i = 0; i < keywordsValues.length; i += 2) {
            schema.put((String) keywordsValues[i], keywordsValues[i + 1]);
        }
        return schema;
    }

    /** A value that may be an object or a string (a document given inline or as JSON/YAML text). */
    static Map<String, Object> objectOrString(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", new ArrayList<>(List.of("object", "string")));
        schema.put("description", description);
        return schema;
    }

    /** An object whose property values all follow {@code valueSchema} (a JSON map). */
    static Map<String, Object> mapOf(String description, Map<String, Object> valueSchema) {
        Map<String, Object> schema = leaf("object", description);
        schema.put("additionalProperties", valueSchema);
        return schema;
    }

    /** The {@code detail} argument shared by the design tools. */
    static Map<String, Object> detail(String defaultLevel, String whatEachLevelAdds) {
        return with(stringEnum("How much to return. summary: the decision-relevant fields and short capped lists. "
                        + "standard: adds the supporting detail. full: adds every list up to a hard cap and the "
                        + "Markdown report. " + whatEachLevelAdds + " Lists that are cut say how many entries were "
                        + "omitted. Default " + defaultLevel + ".", "summary", "standard", "full"),
                "default", defaultLevel);
    }

    private static Map<String, Object> leaf(String type, String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type);
        schema.put("description", description);
        return schema;
    }

    /** Names of the enum constants, lower-cased for LLM-friendly schema values. */
    static String[] lowerNames(Class<? extends Enum<?>> enumType) {
        Enum<?>[] constants = enumType.getEnumConstants();
        List<String> names = new ArrayList<>(constants.length);
        for (Enum<?> constant : constants) {
            names.add(constant.name().toLowerCase(java.util.Locale.ROOT));
        }
        return names.toArray(new String[0]);
    }
}
