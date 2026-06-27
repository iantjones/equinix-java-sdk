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

package api.equinix.javasdk.fabric.model.serializers;

import api.equinix.javasdk.fabric.enums.LinkProtocolType;
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
public class LinkProtocolTypeSerializer extends StdSerializer<LinkProtocolType> {

    /**
     * <p>Constructor for ViewPointSerializer.</p>
     */
    public LinkProtocolTypeSerializer() {
        this(null);
    }

    /**
     * <p>Constructor for LinkProtocolTypeSerializer.</p>
     *
     * @param t a {@link java.lang.Class} object.
     */
    public LinkProtocolTypeSerializer(Class<LinkProtocolType> t) {
        super(t);
    }

    /** {@inheritDoc} */
    @Override
    public void serialize(LinkProtocolType value, JsonGenerator jsonGenerator, SerializerProvider provider)
            throws IOException {

        String linkProtocolTypeString = value.toString();

        if(linkProtocolTypeString != null) {
            String startStr = linkProtocolTypeString.substring(0, 1).toUpperCase();
            String endStr = linkProtocolTypeString.substring(1).toLowerCase();
            linkProtocolTypeString = startStr.concat(endStr);
        }

        jsonGenerator.writeString(linkProtocolTypeString);
    }
}
