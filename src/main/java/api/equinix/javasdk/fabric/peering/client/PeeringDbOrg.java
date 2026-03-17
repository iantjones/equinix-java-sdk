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

package api.equinix.javasdk.fabric.peering.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Represents an organization from the PeeringDB API.
 *
 * <p>When queried with {@code depth=2}, the nested {@code ix_set} and {@code fac_set}
 * provide the complete catalog of internet exchanges and facilities operated by the
 * organization. For Equinix (org_id=2), this yields all ~47 Equinix IXes and hundreds
 * of IBX facilities globally in a single API call.</p>
 *
 * @author ianjones
 * @see PeeringDbIx
 * @see PeeringDbFacility
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeeringDbOrg {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("aka")
    private String aka;

    @JsonProperty("website")
    private String website;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("country")
    private String country;

    @JsonProperty("status")
    private String status;

    /** Internet exchanges operated by this organization (populated at depth >= 1). */
    @JsonProperty("ix_set")
    private List<PeeringDbIx> ixSet;

    /** Facilities operated by this organization (populated at depth >= 1). */
    @JsonProperty("fac_set")
    private List<PeeringDbFacility> facSet;

    /** Networks operated by this organization (populated at depth >= 1). */
    @JsonProperty("net_set")
    private List<PeeringDbNetwork> netSet;
}
