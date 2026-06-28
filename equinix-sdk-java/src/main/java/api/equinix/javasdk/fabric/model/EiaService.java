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

package api.equinix.javasdk.fabric.model;

import api.equinix.javasdk.fabric.enums.EiaServiceState;
import api.equinix.javasdk.fabric.enums.EiaServiceType;
import api.equinix.javasdk.fabric.enums.EiaServiceUseCase;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.EiaServiceAccount;
import api.equinix.javasdk.fabric.model.implementation.EiaServiceBilling;
import api.equinix.javasdk.fabric.model.implementation.EiaServiceChange;
import api.equinix.javasdk.fabric.model.implementation.EiaServiceLocation;
import api.equinix.javasdk.fabric.model.implementation.EiaServiceOrder;
import api.equinix.javasdk.fabric.model.json.creators.EiaServiceOperator;

import java.util.List;

/**
 * An Equinix Internet Access (EIA) service, providing dedicated internet connectivity
 * (single or dual) over Equinix Fabric.
 */
public interface EiaService {

    String getUuid();

    String getHref();

    EiaServiceType getType();

    String getName();

    Integer getBandwidth();

    Integer getBandwidthCommit();

    EiaServiceState getState();

    EiaServiceUseCase getUseCase();

    EiaServiceChange getChange();

    ChangeLog getChangeLog();

    List<EiaServiceLocation> getLocations();

    EiaServiceBilling getBilling();

    EiaServiceAccount getAccount();

    Project getProject();

    EiaServiceOrder getOrder();

    /**
     * Begins a fluent update of this EIA service, e.g.
     * {@code eiaService.update().bandwidth(1000).save()}.
     *
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.EiaServiceOperator.EiaServiceUpdater}
     */
    EiaServiceOperator.EiaServiceUpdater update();

    Boolean delete();

    void refresh();
}
