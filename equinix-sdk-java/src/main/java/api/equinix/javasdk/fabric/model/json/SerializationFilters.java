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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.model.annotations.SerializeOperation;
import api.equinix.javasdk.core.enums.JsonOperation;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

import java.util.Arrays;

/**
 * <p>SerializationFilters class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class SerializationFilters {

    /** Constant <code>createServiceTokenFilter</code> */
    public final static PropertyFilter createServiceTokenFilter = new SimpleBeanPropertyFilter() {
        @Override
        public void serializeAsField(Object pojo, JsonGenerator generator, SerializerProvider provider, PropertyWriter writer)
                throws Exception {
            if(writer.getAnnotation(SerializeOperation.class) != null) {
                SerializeOperation serializeOperation = writer.getAnnotation(SerializeOperation.class);
                if(Arrays.asList(serializeOperation.allowedOperations()).contains(JsonOperation.CREATE) &&
                        Arrays.asList(serializeOperation.includeIn()).contains(ServiceTokenJson.class)) {
                    writer.serializeAsField(pojo, generator, provider);
                };
            }
        }

        @Override
        protected boolean include(BeanPropertyWriter writer) {
            return true;
        }

        @Override
        protected boolean include(PropertyWriter writer) {
            return true;
        }
    };
}
