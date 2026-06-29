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

import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class FilterPropertyListSerializer extends StdSerializer<FilterPropertyList> {

    public FilterPropertyListSerializer() {
        this(null);
    }

    public FilterPropertyListSerializer(Class<FilterPropertyList> t) {
        super(t);
    }

    @Override
    public void serialize(FilterPropertyList value, JsonGenerator jsonGenerator, SerializerProvider provider)
            throws IOException {

        jsonGenerator.writeStartObject();
        provider.defaultSerializeField(value.getFilterType().literal(), value.getFilterProperties(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }
}
