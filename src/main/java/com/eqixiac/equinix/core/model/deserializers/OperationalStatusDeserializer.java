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

import com.eqixiac.equinix.core.enums.OperationalStatus;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Case-insensitive {@link OperationalStatus} deserializer with a forward-compatible fallback:
 * a status value this SDK does not yet list maps to {@link OperationalStatus#UNKNOWN} rather
 * than failing the whole response (mirroring {@code MetroCodeDeserializer}).
 *
 * @author ianjones
 */
public class OperationalStatusDeserializer extends StdDeserializer<OperationalStatus> {

    public OperationalStatusDeserializer() {
        this(null);
    }

    public OperationalStatusDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public OperationalStatus deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String operationalStatus = jsonParser.getText();
        if (operationalStatus == null || operationalStatus.isBlank()) {
            return null;
        }

        try {
            return OperationalStatus.valueOf(operationalStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // A status the API has introduced that this enum does not yet list: map to the
            // forward-compatible UNKNOWN sentinel rather than failing the whole response.
            return OperationalStatus.UNKNOWN;
        }
    }
}
