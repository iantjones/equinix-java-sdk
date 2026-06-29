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

package api.equinix.javasdk.networkedge.model.deserializers;

import api.equinix.javasdk.networkedge.enums.LicenseType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * <p>LicenseTypeDeserializer class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class LicenseTypeDeserializer extends StdDeserializer<LicenseType> {

    /**
     * <p>Constructor for LicenseTypeDeserializer.</p>
     */
    public LicenseTypeDeserializer() {
        this(null);
    }

    /**
     * <p>Constructor for LicenseTypeDeserializer.</p>
     *
     * @param vc a {@link java.lang.Class} object.
     */
    public LicenseTypeDeserializer(Class<?> vc) {
        super(vc);
    }

    /** {@inheritDoc} */
    @Override
    public LicenseType deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String licenseTypeString = node.toString();
        LicenseType licenseType = null;

        if(licenseTypeString != null) {
            licenseTypeString = licenseTypeString.toUpperCase().replace("\"", "");
        }

        return LicenseType.valueOf(licenseTypeString);
    }
}
