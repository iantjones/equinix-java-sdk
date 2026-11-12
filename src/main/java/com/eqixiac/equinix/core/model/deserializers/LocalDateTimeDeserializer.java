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

import com.eqixiac.equinix.core.internal.Constants;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;

/**
 *
 * @author ianjones
 */
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String string = jsonParser.getText().trim();
        if(string.length() == 0)
            return null;

        TemporalAccessor parsed = Constants.ALL_DATE_FORMATS.parse(string);
        LocalDateTime local = LocalDateTime.from(parsed);
        // A value that carries an offset or zone is normalised to UTC, so two instants written
        // with different offsets compare correctly; a value without one is taken as written.
        ZoneId zone = parsed.query(TemporalQueries.zone());
        if (zone == null) {
            return local;
        }
        return local.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
