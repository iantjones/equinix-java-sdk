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

package com.eqixiac.equinix.core.model.deserializers;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Case-insensitive {@link MetroCode} deserializer. Delegates to {@link MetroCode#fromCode(String)},
 * so a metro the API has brought online that the enum does not yet list — as well as a blank or
 * whitespace-only code — maps to the forward-compatible {@link MetroCode#UNKNOWN} sentinel rather
 * than failing the whole response. Only an explicit JSON {@code null} yields {@code null}.
 *
 * @author ianjones
 */
public class MetroCodeDeserializer extends StdDeserializer<MetroCode> {

    public MetroCodeDeserializer() {
        this(null);
    }

    public MetroCodeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public MetroCode deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String metroCode = jsonParser.getText();
        if (metroCode == null) {
            // Only an explicit JSON null stays null; blank/whitespace falls through to
            // fromCode(), whose contract maps anything unrecognisable to UNKNOWN.
            return null;
        }
        return MetroCode.fromCode(metroCode);
    }
}
