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

import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.enums.PhysicalPortType;
import api.equinix.javasdk.fabric.enums.PortType;
import api.equinix.javasdk.fabric.model.json.creators.PortOperator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Typed request body for creating a single Fabric port ({@code POST /fabric/v4/ports}, schema
 * {@code PortRequest}). Only the spec's required and most commonly used fields are modelled:
 * {@code type}, {@code physicalPortsSpeed}, {@code physicalPortsType}, {@code physicalPortsCount},
 * {@code connectivitySourceType}, {@code project}, {@code account}, {@code location},
 * {@code encapsulation}, {@code order}, {@code lagEnabled}, and {@code name}.
 *
 * <p>The {@code account} and {@code location} members follow the spec's
 * {@code SimplifiedAccountRequest} ({@code accountNumber}) and {@code SimplifiedLocationRequest}
 * ({@code metroCode}) request shapes. Instances are built from
 * {@link PortOperator.PortBuilder}; null members are omitted from the JSON.</p>
 *
 * @author ianjones
 */
@Getter
@Setter(AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortCreatorJson {

    @JsonProperty("type")
    private PortType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("physicalPortsSpeed")
    private Integer physicalPortsSpeed;

    @JsonProperty("physicalPortsType")
    private PhysicalPortType physicalPortsType;

    @JsonProperty("physicalPortsCount")
    private Integer physicalPortsCount;

    @JsonProperty("connectivitySourceType")
    private String connectivitySourceType;

    @JsonProperty("lagEnabled")
    private Boolean lagEnabled;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("account")
    private SimplifiedAccountRequest account;

    @JsonProperty("location")
    private SimplifiedLocationRequest location;

    @JsonProperty("encapsulation")
    private Encapsulation encapsulation;

    @JsonProperty("order")
    private Order order;

    /**
     * Constructs the request from a configured {@link PortOperator.PortBuilder}.
     *
     * @param builder the fluent builder carrying the configured fields
     */
    public PortCreatorJson(PortOperator.PortBuilder builder) {
        this.type = builder.getType();
        this.name = builder.getName();
        this.physicalPortsSpeed = builder.getPhysicalPortsSpeed();
        this.physicalPortsType = builder.getPhysicalPortsType();
        this.physicalPortsCount = builder.getPhysicalPortsCount();
        this.connectivitySourceType = builder.getConnectivitySourceType();
        this.lagEnabled = builder.getLagEnabled();
        this.project = builder.getProject();
        this.encapsulation = builder.getEncapsulation();
        this.order = builder.getOrder();
        if (builder.getAccountNumber() != null) {
            this.account = new SimplifiedAccountRequest(builder.getAccountNumber());
        }
        if (builder.getMetroCode() != null) {
            this.location = new SimplifiedLocationRequest(builder.getMetroCode());
        }
    }

    /**
     * Request-side account reference ({@code SimplifiedAccountRequest}): the account number under
     * which the port is created.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class SimplifiedAccountRequest {

        @JsonProperty("accountNumber")
        private final Long accountNumber;

        public SimplifiedAccountRequest(Long accountNumber) {
            this.accountNumber = accountNumber;
        }
    }

    /**
     * Request-side location reference ({@code SimplifiedLocationRequest}): the metro in which the
     * port is provisioned.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class SimplifiedLocationRequest {

        @JsonProperty("metroCode")
        private final String metroCode;

        public SimplifiedLocationRequest(String metroCode) {
            this.metroCode = metroCode;
        }
    }
}
