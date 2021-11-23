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

package api.equinix.javasdk.core.model.serializers;

import api.equinix.javasdk.core.enums.Side;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * <p>ViewPointSerializer class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ViewPointSerializer extends StdSerializer<Side> {

    /**
     * <p>Constructor for ViewPointSerializer.</p>
     */
    public ViewPointSerializer() {
        this(null);
    }

    /**
     * <p>Constructor for ViewPointSerializer.</p>
     *
     * @param t a {@link java.lang.Class} object.
     */
    public ViewPointSerializer(Class<Side> t) {
        super(t);
    }

    /** {@inheritDoc} */
    @Override
    public void serialize(Side value, JsonGenerator jsonGenerator, SerializerProvider provider)
            throws IOException {

        String sideString = value.toString();

        sideString = sideString
                .replace("A_Side", "aSide")
                .replace("Z_Side", "zSide");

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("viewPoint", sideString);
        jsonGenerator.writeEndObject();
    }
}
