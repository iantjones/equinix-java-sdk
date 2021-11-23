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

import api.equinix.javasdk.core.exception.EquinixServiceException;
import api.equinix.javasdk.networkedge.enums.ServiceType;
import api.equinix.javasdk.networkedge.model.implementation.BackupServiceDetail;
import api.equinix.javasdk.networkedge.model.implementation.GenericDataObject;
import api.equinix.javasdk.networkedge.model.json.RestoreFeasibilityJson;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * <p>BackupServiceDetailDeserializer class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class BackupServiceDetailDeserializer extends JsonDeserializer<GenericDataObject<?>> {

    /** {@inheritDoc} */
    @Override
    public GenericDataObject<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode jsonNode = mapper.readTree(jsonParser);

        ServiceType backupServiceDetail = ((BackupServiceDetail) jsonParser.getCurrentValue()).getServiceName();

        Class<?> objectClass = null;

        if (jsonNode != null && backupServiceDetail != null) {
            switch (backupServiceDetail) {
                case L3_CONNECTION: objectClass = RestoreFeasibilityJson.Connection.class;
                case L2_CONNECTION: objectClass = RestoreFeasibilityJson.Connection.class;
                case BYOC: objectClass = RestoreFeasibilityJson.Connection.class;
                case VPN: objectClass = RestoreFeasibilityJson.VPN.class;
                case BGP: objectClass = RestoreFeasibilityJson.BGPPeering.class;
                case ACL: objectClass = RestoreFeasibilityJson.ACLTemplate.class;
                case LICENSE: objectClass = RestoreFeasibilityJson.License.class;
                case DEVICE_LINKING: objectClass = RestoreFeasibilityJson.DeviceLink.class;
            }

            return (GenericDataObject<?>) mapper.readValue(jsonNode.toString(), objectClass);
        }
        else {
            throw new EquinixServiceException("Could not determine what clazz to use to deserialize generic data object in RestoreFeasibility.");
        }
    }
}
