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

package api.equinix.javasdk.networkedge.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Lifecycle;
import api.equinix.javasdk.core.model.deserializers.LocalDateTimeDeserializer;
import api.equinix.javasdk.networkedge.enums.BGPState;
import api.equinix.javasdk.networkedge.enums.BGPStatus;
import api.equinix.javasdk.networkedge.model.BGPPeering;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class BGPPeeringJson extends Lifecycle {

    @Getter static TypeReference<UUIDResult> createTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("connectionUuid")
    private String connectionUuid;

    @JsonProperty("connectionName")
    private String connectionName;

    @JsonProperty("virtualDeviceUuid")
    private String virtualDeviceUuid;

    @JsonProperty("localIpAddress")
    private String localIpAddress;

    @JsonProperty("remoteIpAddress")
    private String remoteIpAddress;

    @JsonProperty("localAsn")
    private Long localAsn;

    @JsonProperty("remoteAsn")
    private Long remoteAsn;

    @JsonProperty("authenticationKey")
    private String authenticationKey;

    @JsonProperty("provisioningStatus")
    private BGPStatus provisioningStatus;

    @JsonProperty("state")
    private BGPState state;

    // Network Edge responses use *DateTime audit fields rather than the shared Lifecycle *Date names.
    @JsonProperty("createdDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdDateTime;

    @JsonProperty("lastUpdatedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime lastUpdatedDateTime;

    @JsonProperty("deletedDateTime")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime deletedDateTime;

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDateTime != null ? createdDateTime : super.getCreatedDate();
    }

    @Override
    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDateTime != null ? lastUpdatedDateTime : super.getLastUpdatedDate();
    }

    @Override
    public LocalDateTime getDeletedDate() {
        return deletedDateTime != null ? deletedDateTime : super.getDeletedDate();
    }
}
