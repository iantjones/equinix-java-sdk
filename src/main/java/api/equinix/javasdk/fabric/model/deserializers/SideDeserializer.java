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

package api.equinix.javasdk.fabric.model.deserializers;

import api.equinix.javasdk.fabric.enums.Side;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 *
 * @author ianjones
 */
public class SideDeserializer extends StdDeserializer<Side> {

    public SideDeserializer() {
        this(null);
    }

    public SideDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Side deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String sideString = node.toString();
        Side side = null;

        if(sideString != null) {
            sideString = sideString
                    .toLowerCase()
                    .replace("_", "")
                    .replace("\"","");

            switch (sideString) {
                case "aside": side = Side.A_Side;
                    break;
                case "zside": side = Side.Z_Side;
                    break;
            }
        }

        return side;
    }
}
