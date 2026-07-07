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

package api.equinix.javasdk.core.model.deserializers;

import api.equinix.javasdk.core.enums.BandwidthUnit;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Deserializes {@link BandwidthUnit} from the varied unit spellings the APIs emit:
 * canonical values ({@code "MBPS"}, {@code "Gbps"}) and short forms ({@code "MB"}, {@code "gb"})
 * in any case. Case-folding happens <em>before</em> the short-form mapping, so already-canonical
 * values are never corrupted. An unrecognized unit maps to {@code null} (matching the shared
 * mapper's forward-compatible enum handling) rather than failing the whole response.
 *
 * @author ianjones
 */
public class BandwidthDeserializer extends StdDeserializer<BandwidthUnit> {

    public BandwidthDeserializer() {
        this(null);
    }

    public BandwidthDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public BandwidthUnit deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException {
        String unitString = jsonParser.getText();
        if (unitString == null || unitString.isBlank()) {
            return null;
        }

        // Uppercase first, then normalize short forms: MB -> MBPS, GB -> GBPS, TB -> TBPS, PB -> PBPS.
        String normalized = unitString.trim().toUpperCase();
        switch (normalized) {
            case "MB" -> normalized = "MBPS";
            case "GB" -> normalized = "GBPS";
            case "TB" -> normalized = "TBPS";
            case "PB" -> normalized = "PBPS";
            default -> { /* already canonical or unknown */ }
        }

        try {
            return BandwidthUnit.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // A unit this enum does not list: forward-compatible null instead of crashing the read.
            return null;
        }
    }
}
