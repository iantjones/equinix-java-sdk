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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.eqixiac.equinix.fabric.enums.PrecisionTimeType;
import com.eqixiac.equinix.fabric.model.Project;
import com.eqixiac.equinix.fabric.model.implementation.Md5;
import com.eqixiac.equinix.fabric.model.implementation.PrecisionTimeIpv4;
import com.eqixiac.equinix.fabric.model.implementation.PrecisionTimeOrder;
import com.eqixiac.equinix.fabric.model.implementation.PrecisionTimePackageRequest;
import com.eqixiac.equinix.fabric.model.implementation.PtpAdvanceConfiguration;
import com.eqixiac.equinix.fabric.model.implementation.TimeServiceFulfillRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Request body for creating a Precision Time service (the Fabric v4
 * {@code precisionTimeServiceRequest} schema).
 */
@Setter(AccessLevel.PRIVATE)
public class PrecisionTimeCreatorJson {

    @JsonProperty("type")
    private PrecisionTimeType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("package")
    private PrecisionTimePackageRequest timePackage;

    @JsonProperty("connections")
    private List<TimeServiceFulfillRequest.Connection> connections;

    @JsonProperty("ipv4")
    private PrecisionTimeIpv4 ipv4;

    @JsonProperty("ntpAdvancedConfiguration")
    private List<Md5> ntpAdvancedConfiguration;

    @JsonProperty("ptpAdvancedConfiguration")
    private PtpAdvanceConfiguration ptpAdvancedConfiguration;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("order")
    private PrecisionTimeOrder order;

    public PrecisionTimeCreatorJson(PrecisionTimeOperator.PrecisionTimeBuilder precisionTimeBuilder) {
        this.type = precisionTimeBuilder.getType();
        this.name = precisionTimeBuilder.getName();
        this.timePackage = precisionTimeBuilder.getPackageCode() != null
                ? new PrecisionTimePackageRequest(precisionTimeBuilder.getPackageCode()) : null;
        this.connections = precisionTimeBuilder.getConnectionUuids() != null
                ? precisionTimeBuilder.getConnectionUuids().stream()
                        .map(TimeServiceFulfillRequest.Connection::new).collect(Collectors.toList())
                : null;
        this.ipv4 = precisionTimeBuilder.getIpv4();
        this.ntpAdvancedConfiguration = precisionTimeBuilder.getNtpAdvancedConfiguration();
        this.ptpAdvancedConfiguration = precisionTimeBuilder.getPtpAdvancedConfiguration();
        this.project = precisionTimeBuilder.getProject();
        this.order = precisionTimeBuilder.getOrder();
    }
}
