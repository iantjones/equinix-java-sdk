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

import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.PortClientImpl;
import com.eqixiac.equinix.fabric.enums.BmmrType;
import com.eqixiac.equinix.fabric.enums.ConnectivitySourceType;
import com.eqixiac.equinix.fabric.enums.PhysicalPortType;
import com.eqixiac.equinix.fabric.enums.PortServiceCode;
import com.eqixiac.equinix.fabric.enums.PortServiceType;
import com.eqixiac.equinix.fabric.enums.PortType;
import com.eqixiac.equinix.fabric.model.Port;
import com.eqixiac.equinix.fabric.model.implementation.DemarcationPoint;
import com.eqixiac.equinix.fabric.model.implementation.Encapsulation;
import com.eqixiac.equinix.fabric.model.implementation.PhysicalPort;
import com.eqixiac.equinix.fabric.model.implementation.PortCreatorJson;
import com.eqixiac.equinix.fabric.model.implementation.PortLoa;
import com.eqixiac.equinix.fabric.model.implementation.PortNotification;
import com.eqixiac.equinix.fabric.model.implementation.PortOrder;
import com.eqixiac.equinix.fabric.model.implementation.PackageRef;
import com.eqixiac.equinix.fabric.model.implementation.PortSettings;
import com.eqixiac.equinix.fabric.model.implementation.Redundancy;
import com.eqixiac.equinix.fabric.model.Project;
import com.eqixiac.equinix.fabric.model.json.PortJson;
import com.eqixiac.equinix.fabric.model.wrappers.PortWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent create/update entry point for Fabric ports, mirroring the route-filter operator.
 * {@link #create()} builds a {@link PortCreatorJson} for {@code POST /fabric/v4/ports};
 * {@link #update(String)} accumulates JSON Patch operations for {@code PATCH /fabric/v4/ports/{uuid}}.
 *
 * @author ianjones
 */
public class PortOperator extends ResourceImpl<Port> {

    @Getter
    private final PageablePost<Port> serviceClient;

    public PortOperator(PageablePost<Port> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * Begins the fluent builder for creating a new port.
     *
     */
    public PortBuilder create() {
        return new PortBuilder();
    }

    /**
     * Begins a fluent update of an existing port, identified by uuid.
     *
     * @param uuid the uuid of the port to update
     */
    public PortUpdater update(String uuid) {
        return new PortUpdater(uuid);
    }

    /**
     * Fluent builder for {@link PortCreatorJson}. Each setter returns {@code this}; {@link #create()}
     * sends the {@code POST} and returns the created port wrapped as a {@link Port}.
     */
    @Getter(AccessLevel.PUBLIC)
    public class PortBuilder {

        private PortType type;
        private Integer physicalPortsSpeed;
        private PhysicalPortType physicalPortsType;
        private Integer physicalPortsCount;
        private ConnectivitySourceType connectivitySourceType;
        private BmmrType bmmrType;
        private Boolean lagEnabled;
        private Project project;
        private Long accountNumber;
        private String metroCode;
        private Encapsulation encapsulation;
        private PortOrder order;
        private String demarcationPointIbx;
        private String tetherIbx;
        private DemarcationPoint demarcationPoint;
        private Redundancy redundancy;
        private PackageRef portPackage;
        private PortSettings settings;
        private List<PortNotification> notifications;
        private List<PhysicalPort> physicalPorts;
        private List<PortLoa> loas;
        private PortServiceType serviceType;
        private PortServiceCode serviceCode;
        private Integer bandwidth;
        private boolean dryRun;

        protected PortBuilder() {
        }

        /**
         * Marks this create as a validate-only dry run: {@code create()} sends
         * {@code POST /fabric/v4/ports?dryRun=true} (spec: "option to verify that API calls will
         * succeed"; boolean, default {@code false}). No port is ordered — the API responds
         * {@code 200} (rather than the real create's {@code 202}) with the validated port order
         * echoed back, so the returned {@link Port} carries no uuid/name/state.
         *
         * @return this builder
         */
        public PortBuilder dryRun() {
            this.dryRun = true;
            return this;
        }

        public PortBuilder ofType(PortType type) {
            this.type = type;
            return this;
        }

        public PortBuilder physicalPortsSpeed(Integer physicalPortsSpeed) {
            this.physicalPortsSpeed = physicalPortsSpeed;
            return this;
        }

        public PortBuilder physicalPortsType(PhysicalPortType physicalPortsType) {
            this.physicalPortsType = physicalPortsType;
            return this;
        }

        public PortBuilder physicalPortsCount(Integer physicalPortsCount) {
            this.physicalPortsCount = physicalPortsCount;
            return this;
        }

        /**
         * Sets the port connectivity type (spec {@code PortRequest.connectivitySourceType}:
         * {@code COLO}, {@code BMMR} or {@code REMOTE}).
         *
         * @param connectivitySourceType the connectivity source type
         * @return this builder
         */
        public PortBuilder connectivitySourceType(ConnectivitySourceType connectivitySourceType) {
            this.connectivitySourceType = connectivitySourceType;
            return this;
        }

        /**
         * Sets the BMMR type ({@code SELF} or {@code EQUINIX}); mandatory when the
         * connectivity source type is {@code BMMR}.
         *
         * @param bmmrType the BMMR type
         * @return this builder
         */
        public PortBuilder bmmrType(BmmrType bmmrType) {
            this.bmmrType = bmmrType;
            return this;
        }

        public PortBuilder lagEnabled(Boolean lagEnabled) {
            this.lagEnabled = lagEnabled;
            return this;
        }

        public PortBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public PortBuilder projectId(String projectId) {
            this.project = new Project(projectId);
            return this;
        }

        public PortBuilder accountNumber(Long accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public PortBuilder metroCode(String metroCode) {
            this.metroCode = metroCode;
            return this;
        }

        public PortBuilder withEncapsulation(Encapsulation encapsulation) {
            this.encapsulation = encapsulation;
            return this;
        }

        /**
         * Sets the order details (purchase order, customer reference id, signature), following
         * the spec's {@code PortOrder} request shape.
         *
         * @param order the port order details
         * @return this builder
         */
        public PortBuilder withOrder(PortOrder order) {
            this.order = order;
            return this;
        }

        /**
         * Sets the A-side/Equinix IBX for the demarcation point.
         *
         * @param demarcationPointIbx the A-side IBX code
         * @return this builder
         */
        public PortBuilder demarcationPointIbx(String demarcationPointIbx) {
            this.demarcationPointIbx = demarcationPointIbx;
            return this;
        }

        /**
         * Sets the z-side/Equinix IBX for the tether.
         *
         * @param tetherIbx the z-side IBX code
         * @return this builder
         */
        public PortBuilder tetherIbx(String tetherIbx) {
            this.tetherIbx = tetherIbx;
            return this;
        }

        /**
         * Sets the customer demarcation point ({@code PortDemarcationPoint}).
         *
         * @param demarcationPoint the demarcation point
         * @return this builder
         */
        public PortBuilder withDemarcationPoint(DemarcationPoint demarcationPoint) {
            this.demarcationPoint = demarcationPoint;
            return this;
        }

        /**
         * Sets the redundancy configuration ({@code PortRedundancy}), e.g. the primary port's
         * uuid as group and a {@code SECONDARY} priority when ordering a redundant port.
         *
         * @param redundancy the redundancy configuration
         * @return this builder
         */
        public PortBuilder withRedundancy(Redundancy redundancy) {
            this.redundancy = redundancy;
            return this;
        }

        /**
         * Sets the billing package for the port ({@code Package}: {@code STANDARD},
         * {@code UNLIMITED} or {@code UNLIMITED_PLUS}).
         *
         * @param portPackage the port package
         * @return this builder
         */
        public PortBuilder withPackage(PackageRef portPackage) {
            this.portPackage = portPackage;
            return this;
        }

        /**
         * Sets the port configuration settings ({@code PortSettings}).
         *
         * @param settings the port settings
         * @return this builder
         */
        public PortBuilder withSettings(PortSettings settings) {
            this.settings = settings;
            return this;
        }

        /**
         * Sets the notification preferences ({@code PortNotification} entries).
         *
         * @param notifications the notification preferences
         * @return this builder
         */
        public PortBuilder withNotifications(List<PortNotification> notifications) {
            this.notifications = notifications;
            return this;
        }

        /**
         * Sets the physical ports that implement this port.
         *
         * @param physicalPorts the physical member ports
         * @return this builder
         */
        public PortBuilder withPhysicalPorts(List<PhysicalPort> physicalPorts) {
            this.physicalPorts = physicalPorts;
            return this;
        }

        /**
         * Sets the port letters of authorization ({@code PortLoa} entries).
         *
         * @param loas the port LOAs
         * @return this builder
         */
        public PortBuilder withLoas(List<PortLoa> loas) {
            this.loas = loas;
            return this;
        }

        /**
         * Sets the port service type ({@code EPL} or {@code MSP}).
         *
         * @param serviceType the service type
         * @return this builder
         * @deprecated deprecated in the Fabric v4 spec
         */
        @Deprecated
        public PortBuilder serviceType(PortServiceType serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        /**
         * Sets the service code identifying the service type associated with this port
         * ({@code CX}, {@code IX}, {@code IA} or {@code MC}).
         *
         * @param serviceCode the service code
         * @return this builder
         */
        public PortBuilder serviceCode(PortServiceCode serviceCode) {
            this.serviceCode = serviceCode;
            return this;
        }

        /**
         * Sets the port bandwidth in Mbps.
         *
         * @param bandwidth the bandwidth in Mbps
         * @return this builder
         * @deprecated deprecated in the Fabric v4 spec
         */
        @Deprecated
        public PortBuilder bandwidth(Integer bandwidth) {
            this.bandwidth = bandwidth;
            return this;
        }

        public Port create() {
            PortCreatorJson portCreatorJson = new PortCreatorJson(this);
            PortClientImpl clientImpl = (PortClientImpl) PortOperator.this.getServiceClient();
            PortJson portJson = dryRun
                    ? clientImpl.dryRunCreate(portCreatorJson)
                    : clientImpl.create(portCreatorJson);
            return new PortWrapper(portJson, PortOperator.this.getServiceClient());
        }
    }

    /**
     * Fluent builder for updating an existing port. Each typed setter records a {@code replace}
     * change operation; {@link #save()} sends them as one {@code PATCH} (an op/path/value array,
     * content-type {@code application/json}) and returns the refreshed model.
     *
     * <pre>{@code port.update().name("New-Name").save();}</pre>
     */
    public class PortUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();
        private boolean dryRun;

        protected PortUpdater(String uuid) {
            this.uuid = uuid;
        }

        /**
         * Marks this update as a validate-only dry run: {@link #save()} sends
         * {@code PATCH /fabric/v4/ports/{uuid}?dryRun=true} (spec: "option to verify that API
         * calls will succeed"; boolean, default {@code false}). Nothing is changed on the port.
         *
         * <p>Wire-shape caveat: unlike the real update's bare {@code Port} body, the dry-run
         * {@code 200} body is an {@code AllPortsResponse} paginated envelope
         * ({@code {pagination, data:[Port]}}) carrying the simulated updated port in
         * {@code data[0]} (spec example {@code PortUpdateDryRunResponse}); the SDK deserializes
         * that envelope and unwraps {@code data[0]} into the {@link Port} returned by
         * {@link #save()}.</p>
         *
         * @return this updater
         */
        public PortUpdater dryRun() {
            this.dryRun = true;
            return this;
        }

        /**
         * Replaces the port name.
         *
         * @param name the new name
         * @return this updater
         */
        public PortUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        /**
         * Adds an arbitrary change operation, for paths not covered by the typed setters above.
         *
         * @param operation the patch operation
         * @return this updater
         */
        public PortUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Applies the accumulated changes and returns the port refreshed from the server.
         *
         * @return the updated {@link com.eqixiac.equinix.fabric.model.Port}
         */
        public Port save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            PortClientImpl clientImpl = (PortClientImpl) PortOperator.this.getServiceClient();
            PortJson portJson = dryRun
                    ? clientImpl.dryRunUpdate(uuid, operations)
                    : clientImpl.update(uuid, operations);
            return new PortWrapper(portJson, PortOperator.this.getServiceClient());
        }
    }
}
