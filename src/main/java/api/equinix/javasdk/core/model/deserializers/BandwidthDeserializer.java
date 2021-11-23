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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * <p>BandwidthDeserializer class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class BandwidthDeserializer extends StdDeserializer<BandwidthUnit> {

    /**
     * <p>Constructor for BandwidthDeserializer.</p>
     */
    public BandwidthDeserializer() {
        this(null);
    }

    /**
     * <p>Constructor for BandwidthDeserializer.</p>
     *
     * @param vc a {@link java.lang.Class} object.
     */
    public BandwidthDeserializer(Class<?> vc) {
        super(vc);
    }

    /** {@inheritDoc} */
    @Override
    public BandwidthUnit deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String unitString = node.toString();

        if(unitString != null) {
            unitString = unitString
                    .replace("\"","")
                    .replace("MB", "Mbps")
                    .replace("GB", "Gbps")
                    .replace("TB", "Tbps")
                    .replace("PB", "Pbps")
                    .toUpperCase();
        }

        return BandwidthUnit.valueOf(unitString);
    }
}
