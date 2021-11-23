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
import api.equinix.javasdk.networkedge.enums.DeviceLinkStatus;
import api.equinix.javasdk.networkedge.enums.Source;
import api.equinix.javasdk.networkedge.model.DeviceLink;
import api.equinix.javasdk.networkedge.model.implementation.DeviceLinkSupportDetail;
import api.equinix.javasdk.networkedge.model.implementation.Link;
import api.equinix.javasdk.networkedge.model.implementation.LinkDevice;
import api.equinix.javasdk.networkedge.model.implementation.UUIDResult;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * <p>DeviceLinkJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class DeviceLinkJson extends Lifecycle {

    @Getter static TypeReference<Page<DeviceLink, DeviceLinkJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<DeviceLinkJson> singleTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<UUIDResult> createTypeRef = new TypeReference<>() {};

    @JsonAlias("id")
    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("source")
    private Source source;

    @JsonProperty("groupName")
    private String groupName;

    @JsonProperty("subnet")
    private String subnet;

    @JsonProperty("status")
    private DeviceLinkStatus status;

    @JsonProperty("links")
    private List<Link> links;

    @JsonProperty("linkDevices")
    private List<LinkDevice> linkDevices;

    @JsonProperty("supportDetails")
    private List<DeviceLinkSupportDetail> supportDetails;
}
