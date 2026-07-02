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

import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.enums.PrecisionTimeType;
import api.equinix.javasdk.fabric.model.Project;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Request body for fulfilling (provisioning) a Precision Time service via
 * {@code PUT /timeServices/{serviceId}} (spec schema {@code precisionTimeServiceRequest}).
 * Carries the Fabric connection UUIDs to attach plus the service {@code type}, {@code name},
 * {@code package} and {@code ipv4} network information required by the spec, and the optional
 * NTP / PTP advanced configuration, project and order references. Unset fields are omitted
 * from the serialized body.
 *
 * @author ianjones
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeServiceFulfillRequest {

    @JsonProperty("connections")
    private final List<Connection> connections;

    @JsonProperty("type")
    private PrecisionTimeType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("package")
    private PrecisionTimePackageRequest servicePackage;

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

    /**
     *
     * @param connectionUuids the Fabric connection UUIDs to attach to the service
     */
    public TimeServiceFulfillRequest(List<String> connectionUuids) {
        this.connections = connectionUuids.stream().map(Connection::new).collect(Collectors.toList());
    }

    /**
     * Sets the service type ({@code NTP} or {@code PTP}).
     *
     * @param type the protocol type
     * @return this request
     */
    public TimeServiceFulfillRequest withType(PrecisionTimeType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the Precision Time service name.
     *
     * @param name the service name
     * @return this request
     */
    public TimeServiceFulfillRequest withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the service package (tier).
     *
     * @param packageCode the package code
     * @return this request
     */
    public TimeServiceFulfillRequest withPackageCode(PrecisionTimePackageCode packageCode) {
        this.servicePackage = new PrecisionTimePackageRequest(packageCode);
        return this;
    }

    /**
     * Sets the EPT service network information.
     *
     * @param ipv4 the ipv4 configuration
     * @return this request
     */
    public TimeServiceFulfillRequest withIpv4(PrecisionTimeIpv4 ipv4) {
        this.ipv4 = ipv4;
        return this;
    }

    /**
     * Sets the NTP advanced configuration (MD5 authentication entries).
     *
     * @param ntpAdvancedConfiguration the MD5 entries
     * @return this request
     */
    public TimeServiceFulfillRequest withNtpAdvancedConfiguration(List<Md5> ntpAdvancedConfiguration) {
        this.ntpAdvancedConfiguration = ntpAdvancedConfiguration;
        return this;
    }

    /**
     * Sets the PTP advanced configuration.
     *
     * @param ptpAdvancedConfiguration the PTP configuration
     * @return this request
     */
    public TimeServiceFulfillRequest withPtpAdvancedConfiguration(PtpAdvanceConfiguration ptpAdvancedConfiguration) {
        this.ptpAdvancedConfiguration = ptpAdvancedConfiguration;
        return this;
    }

    /**
     * Sets the owning project reference.
     *
     * @param project the project
     * @return this request
     */
    public TimeServiceFulfillRequest withProject(Project project) {
        this.project = project;
        return this;
    }

    /**
     * Sets the order details.
     *
     * @param order the order reference
     * @return this request
     */
    public TimeServiceFulfillRequest withOrder(PrecisionTimeOrder order) {
        this.order = order;
        return this;
    }

    /**
     * Reference to a Fabric connection, identified by UUID.
     */
    public static class Connection {

        @JsonProperty("uuid")
        private final String uuid;

        public Connection(String uuid) {
            this.uuid = uuid;
        }

        public String getUuid() {
            return this.uuid;
        }
    }
}
