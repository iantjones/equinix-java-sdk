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

package api.equinix.javasdk.networkedge.model.implementation;

import api.equinix.javasdk.networkedge.enums.VersionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * <p>VersionDetail class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class VersionDetail {

    @JsonProperty("version")
    private String version;

    @JsonProperty("imageName")
    private String imageName;

    @JsonProperty("versionDate")
    private Timestamp versionDate;
    
    @JsonProperty("status")
    private VersionStatus status;

    @JsonProperty("stableVersion")
    private Boolean stableVersion;

    @JsonProperty("maxInterfaceCount")
    private Integer maxInterfaceCount;

    @JsonProperty("defaultInterfaceCount")
    private Integer defaultInterfaceCount;

    @JsonProperty("clusterMaxInterfaceCount")
    private Integer clusterMaxInterfaceCount;

    @JsonProperty("clusterDefaultInterfaceCount")
    private Integer clusterDefaultInterfaceCount;

    @JsonProperty("upgradeAllowed")
    private Boolean upgradeAllowed;

    @JsonProperty("allowedUpgradableVersions")
    private ArrayList<String> allowedUpgradableVersions;

    @JsonProperty("requiredPreviousVersions")
    private ArrayList<String> requiredPreviousVersions;

    @JsonProperty("releaseNotesLink")
    private String releaseNotesLink;

    @JsonProperty("subscriptionImageName")
    private String subscriptionImageName;
}
