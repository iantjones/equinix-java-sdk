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
import api.equinix.javasdk.networkedge.enums.BackupRequestType;
import api.equinix.javasdk.networkedge.enums.BackupStatus;
import api.equinix.javasdk.networkedge.enums.BackupType;
import api.equinix.javasdk.networkedge.model.Backup;
import api.equinix.javasdk.networkedge.model.implementation.DeviceRestore;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * <p>BackupJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class BackupJson extends Lifecycle {

    @Getter static TypeReference<Page<Backup, BackupJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<BackupJson> singleTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<UUIDResult> createTypeRef = new TypeReference<>() {};

    @JsonProperty(value = "uuid")
    private String uuid;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty("type")
    private BackupType type;

    @JsonProperty("status")
    private BackupStatus status;

    @JsonProperty("requestType")
    private BackupRequestType requestType;

    @JsonProperty("downloadUrl")
    private String downloadUrl;

    @JsonProperty("version")
    private String version;

    @JsonProperty("deleteAllowed")
    private Boolean deleteAllowed;

    @JsonProperty("restores")
    List<DeviceRestore> restores;
}
