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

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import api.equinix.javasdk.fabric.model.implementation.ConnectionStat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * <p>ConnectionStatisticJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class ConnectionStatisticJson implements Serializable {

    @Getter static TypeReference<Page<ConnectionStatistic, ConnectionStatisticJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<ConnectionStatisticJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<ConnectionStatisticJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private ConnectionType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("stats")
    private ConnectionStat stats;
}
