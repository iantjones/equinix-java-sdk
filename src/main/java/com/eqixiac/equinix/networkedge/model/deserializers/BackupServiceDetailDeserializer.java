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

package com.eqixiac.equinix.networkedge.model.deserializers;

import com.eqixiac.equinix.core.exception.EquinixServiceException;
import com.eqixiac.equinix.networkedge.enums.ServiceType;
import com.eqixiac.equinix.networkedge.model.implementation.BackupServiceDetail;
import com.eqixiac.equinix.networkedge.model.implementation.GenericDataObject;
import com.eqixiac.equinix.networkedge.model.json.RestoreFeasibilityJson;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 *
 * @author ianjones
 */
public class BackupServiceDetailDeserializer extends JsonDeserializer<GenericDataObject<?>> {

    @Override
    public GenericDataObject<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode jsonNode = mapper.readTree(jsonParser);

        ServiceType backupServiceDetail = ((BackupServiceDetail) jsonParser.getCurrentValue()).getServiceName();

        if (jsonNode != null && backupServiceDetail != null) {
            // Arrow switch: the original statement switch had no break statements, so every service
            // type fell through to DEVICE_LINKING and deserialized as DeviceLink regardless of type.
            Class<?> objectClass = switch (backupServiceDetail) {
                case L3_CONNECTION, L2_CONNECTION, BYOC -> RestoreFeasibilityJson.Connection.class;
                case VPN -> RestoreFeasibilityJson.VPN.class;
                case BGP -> RestoreFeasibilityJson.BGPPeering.class;
                case ACL -> RestoreFeasibilityJson.ACLTemplate.class;
                case LICENSE -> RestoreFeasibilityJson.License.class;
                case DEVICE_LINKING -> RestoreFeasibilityJson.DeviceLink.class;
            };

            return (GenericDataObject<?>) mapper.readValue(jsonNode.toString(), objectClass);
        }
        else {
            throw new EquinixServiceException("Could not determine what clazz to use to deserialize generic data object in RestoreFeasibility.");
        }
    }
}
