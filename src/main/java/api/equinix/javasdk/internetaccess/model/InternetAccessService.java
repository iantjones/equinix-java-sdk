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

package api.equinix.javasdk.internetaccess.model;

import api.equinix.javasdk.internetaccess.enums.ServiceBilling;
import api.equinix.javasdk.internetaccess.enums.ServiceState;
import api.equinix.javasdk.internetaccess.enums.ServiceTypeV2;
import api.equinix.javasdk.internetaccess.enums.UseCase;
import api.equinix.javasdk.internetaccess.model.implementation.Account;
import api.equinix.javasdk.internetaccess.model.implementation.Change;
import api.equinix.javasdk.internetaccess.model.implementation.ChangeLog;
import api.equinix.javasdk.internetaccess.model.implementation.Location;
import api.equinix.javasdk.internetaccess.model.implementation.ProjectReadModel;
import api.equinix.javasdk.internetaccess.model.implementation.RoutingProtocolReadModel;
import api.equinix.javasdk.internetaccess.model.implementation.ServiceConnection;
import api.equinix.javasdk.internetaccess.model.implementation.ServiceOrderReadModel;

import java.util.List;

/**
 * An Equinix Internet Access (EIA) v2 service, as returned by the create
 * ({@code POST /internetAccess/v2/services}), get-details
 * ({@code GET /internetAccess/v2/services/{serviceId}}), update
 * ({@code PATCH /internetAccess/v2/services/{serviceId}}) and search
 * ({@code POST /internetAccess/v2/services/search}) operations.
 *
 * <p>This is a read-only response view; the nested IP blocks and routing configuration are
 * supplied at creation time through the
 * {@link api.equinix.javasdk.internetaccess.client.InternetAccessServices#define() builder}.</p>
 *
 * <p>The create response ({@code ServiceCreateResponse}) is a subset of the full read model
 * returned by get/update/search ({@code ServiceReadModel}); fields not present in the create
 * response (for example {@code billing}, {@code locations}, {@code billingStartDate}) are
 * {@code null} on a freshly created service.</p>
 */
public interface InternetAccessService {

    /**
     * @return the unique identifier of the service
     */
    String getUuid();

    /**
     * @return the URI of the service
     */
    String getHref();

    /**
     * @return the name of the service
     */
    String getName();

    /**
     * @return the description of the service
     */
    String getDescription();

    /**
     * @return the topology of the service ({@code SINGLE} or {@code DUAL})
     */
    ServiceTypeV2 getType();

    /**
     * @return service bandwidth in Mbps
     */
    Long getBandwidth();

    /**
     * @return the lifecycle state of the service
     */
    ServiceState getState();

    /**
     * @return the intended use case of the service ({@code MAIN}, {@code BACKUP} or
     *         {@code MANAGEMENT_ACCESS})
     */
    UseCase getUseCase();

    /**
     * @return the billing type of the service ({@code FIXED}, {@code USAGE_BASED} or
     *         {@code BURST_BASED}); only present on the full read model
     */
    ServiceBilling getBilling();

    /**
     * @return whether the service is billed; only present on the full read model
     */
    Boolean getBillingEnabled();

    /**
     * @return the billing start date (YYYY-MM-DD) when the service is billed; only present on the
     *         full read model
     */
    String getBillingStartDate();

    /**
     * @return the billing/organization account associated with the service
     */
    Account getAccount();

    /**
     * @return the resource-manager project the service belongs to
     */
    ProjectReadModel getProject();

    /**
     * @return the current state of the latest service change (the change flow)
     */
    Change getChange();

    /**
     * @return the audit trail (created/updated/deleted by and timestamps)
     */
    ChangeLog getChangeLog();

    /**
     * @return the order backing the service (identity, status and submitted order detail)
     */
    ServiceOrderReadModel getOrder();

    /**
     * @return the connections of the service (1 or 2 entries)
     */
    List<ServiceConnection> getConnections();

    /**
     * @return the routing protocol of the service (direct, static or BGP)
     */
    RoutingProtocolReadModel getRoutingProtocol();

    /**
     * @return the locations (metro/IBX/region) of the service; only present on the full read model
     */
    List<Location> getLocations();

    /**
     * @return the tags applied to the service
     */
    List<String> getTags();
}
