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

package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.fabric.model.json.MetroJson;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * <p>ServiceProfileMetro class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class ServiceProfileMetro extends MetroJson {

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("ibxs")
    private List<String> ibxs;

    @JsonProperty("inTrail")
    private Boolean inTrail;

    @JsonProperty("sellerRegions")
    Map<String, String> sellerRegions;
}
