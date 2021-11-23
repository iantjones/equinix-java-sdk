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
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.networkedge.model.Metro;
import api.equinix.javasdk.networkedge.model.implementation.Zone;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>MetroJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class MetroJson {

    @Getter static TypeReference<Page<Metro, MetroJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<MetroJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<MetroJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("metroCode")
    private MetroCode metroCode;

    @JsonProperty("region")
    private Region region;

    @JsonProperty("defaultIbx")
    private String defaultIbx;

    @JsonProperty("availableZones")
    private ArrayList<Zone> availableZones;

    @JsonProperty("clusterSupported")
    private Boolean clusterSupported;

    @JsonProperty("metroDescription")
    private String metroDescription;
}
