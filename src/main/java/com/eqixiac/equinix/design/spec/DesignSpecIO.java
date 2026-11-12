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

package com.eqixiac.equinix.design.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Reads and writes a {@link DesignSpec} as JSON or YAML.
 *
 * <p>Reading is strict: an unknown property is an error (a typo in a hand-written spec must not be
 * silently ignored) and enum values are matched case-insensitively. Reading does not validate the
 * content; call {@link DesignSpec#validate()} or {@link DesignSpec#requireValid()}.</p>
 */
public final class DesignSpecIO {

    private static final ObjectMapper JSON = configure(JsonMapper.builder()).build();

    private static final ObjectMapper YAML = configure(YAMLMapper.builder(new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))).build();

    /** Property-sorted, entry-sorted JSON used only for {@link #fingerprint(DesignSpec)}. */
    private static final ObjectMapper CANONICAL = configure(JsonMapper.builder())
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private DesignSpecIO() {
    }

    private static <M extends ObjectMapper, B extends com.fasterxml.jackson.databind.cfg.MapperBuilder<M, B>>
            B configure(B builder) {
        return builder
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Parses a JSON document.
     *
     * @param json the document
     * @return the spec (not yet validated)
     * @throws IllegalArgumentException when the document is not a readable spec; the message names the location
     */
    public static DesignSpec fromJson(String json) {
        return parse(JSON, json, "JSON");
    }

    /**
     * Parses a YAML document.
     *
     * @param yaml the document
     * @return the spec (not yet validated)
     * @throws IllegalArgumentException when the document is not a readable spec; the message names the location
     */
    public static DesignSpec fromYaml(String yaml) {
        return parse(YAML, yaml, "YAML");
    }

    /**
     * Reads a spec from a file, choosing the format by extension: {@code .yaml} / {@code .yml} is
     * YAML, anything else is JSON.
     *
     * @param file the file to read (UTF-8)
     * @return the spec (not yet validated)
     * @throws UncheckedIOException when the file cannot be read
     * @throws IllegalArgumentException when the content is not a readable spec
     */
    public static DesignSpec read(Path file) {
        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read design spec " + file, e);
        }
        return isYaml(file) ? fromYaml(content) : fromJson(content);
    }

    /**
     * Writes a spec to a file, choosing the format by extension as {@link #read(Path)} does.
     *
     * @param spec the spec
     * @param file the file to write (UTF-8, replaced if present)
     * @throws UncheckedIOException when the file cannot be written
     */
    public static void write(DesignSpec spec, Path file) {
        try {
            Files.writeString(file, isYaml(file) ? toYaml(spec) : toJson(spec), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write design spec " + file, e);
        }
    }

    /**
     * @param spec the spec
     * @return indented JSON
     */
    public static String toJson(DesignSpec spec) {
        return serialize(JSON.writerWithDefaultPrettyPrinter(), spec);
    }

    /**
     * @param spec the spec
     * @return YAML
     */
    public static String toYaml(DesignSpec spec) {
        return serialize(YAML.writer(), spec);
    }

    /**
     * The SHA-256 of the spec's canonical JSON (properties and map entries sorted), as lowercase hex.
     *
     * @param spec the spec
     * @return 64 hex characters, equal for equal content
     */
    public static String fingerprint(DesignSpec spec) {
        byte[] canonical = serialize(CANONICAL.writer(), spec).getBytes(StandardCharsets.UTF_8);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every Java runtime", e);
        }
    }

    private static boolean isYaml(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private static DesignSpec parse(ObjectMapper mapper, String content, String format) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("The " + format + " design spec is empty");
        }
        try {
            DesignSpec spec = mapper.readValue(content, DesignSpec.class);
            if (spec == null) {
                throw new IllegalArgumentException("The " + format + " design spec is empty");
            }
            return spec;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Not a readable " + format + " design spec: " + e.getOriginalMessage()
                    + (e.getLocation() == null ? "" : " (line " + e.getLocation().getLineNr() + ", column "
                    + e.getLocation().getColumnNr() + ")"), e);
        }
    }

    private static String serialize(com.fasterxml.jackson.databind.ObjectWriter writer, DesignSpec spec) {
        try {
            return writer.writeValueAsString(spec);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("A DesignSpec is always serializable", e);
        }
    }
}
