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

package com.eqixiac.equinix.internetaccess.model.json;

import com.eqixiac.equinix.internetaccess.enums.ServiceBilling;
import com.eqixiac.equinix.internetaccess.enums.ServiceState;
import com.eqixiac.equinix.internetaccess.enums.ServiceTypeV2;
import com.eqixiac.equinix.internetaccess.enums.UseCase;
import com.eqixiac.equinix.internetaccess.model.InternetAccessService;
import com.eqixiac.equinix.internetaccess.model.implementation.Account;
import com.eqixiac.equinix.internetaccess.model.implementation.Change;
import com.eqixiac.equinix.internetaccess.model.implementation.ChangeLog;
import com.eqixiac.equinix.internetaccess.model.implementation.Location;
import com.eqixiac.equinix.internetaccess.model.implementation.ProjectReadModel;
import com.eqixiac.equinix.internetaccess.model.implementation.RoutingProtocolReadModel;
import com.eqixiac.equinix.internetaccess.model.implementation.ServiceConnection;
import com.eqixiac.equinix.internetaccess.model.implementation.ServiceOrderReadModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Read-only JSON model for the {@code ServiceReadModel} / {@code ServiceCreateResponse} returned by
 * the Equinix Internet Access (EIA) v2 service create, get-details, update and search operations.
 * Implements {@link InternetAccessService} directly.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternetAccessServiceJson implements InternetAccessService {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private ServiceTypeV2 type;

    @JsonProperty("bandwidth")
    private Long bandwidth;

    @JsonProperty("state")
    private ServiceState state;

    @JsonProperty("useCase")
    private UseCase useCase;

    @JsonProperty("billing")
    private ServiceBilling billing;

    @JsonProperty("billingEnabled")
    private Boolean billingEnabled;

    @JsonProperty("billingStartDate")
    private String billingStartDate;

    @JsonProperty("account")
    private Account account;

    @JsonProperty("project")
    private ProjectReadModel project;

    @JsonProperty("change")
    private Change change;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("order")
    private ServiceOrderReadModel order;

    @JsonProperty("connections")
    private List<ServiceConnection> connections;

    @JsonProperty("routingProtocol")
    private RoutingProtocolReadModel routingProtocol;

    @JsonProperty("locations")
    private List<Location> locations;

    @JsonProperty("tags")
    private List<String> tags;
}
