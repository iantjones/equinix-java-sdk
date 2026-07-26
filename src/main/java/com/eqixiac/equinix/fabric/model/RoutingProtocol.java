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

package com.eqixiac.equinix.fabric.model;

import com.eqixiac.equinix.fabric.enums.RoutingProtocolState;
import com.eqixiac.equinix.fabric.enums.RoutingProtocolType;
import com.eqixiac.equinix.fabric.model.implementation.*;
import com.eqixiac.equinix.fabric.model.json.creators.RoutingProtocolOperator;

public interface RoutingProtocol {

    String getUuid();

    String getHref();

    String getName();

    RoutingProtocolType getType();

    RoutingProtocolState getState();

    BGPConnectionIpv4 getBgpIpv4();

    BGPConnectionIpv6 getBgpIpv6();

    DirectConnectionIpv4 getDirectIpv4();

    DirectConnectionIpv6 getDirectIpv6();

    BFDConfig getBfd();

    Long getCustomerAsn();

    Long getEquinixAsn();

    String getBgpAuthKey();

    Boolean getAsOverrideEnabled();

    RoutingProtocolOperation getOperation();

    Project getProject();

    RoutingProtocolConnection getConnection();

    ChangeLog getChangeLog();

    Change getChange();

    /**
     * Begins a fluent update of this routing protocol on its parent connection, e.g.
     * {@code routingProtocol.update(connectionId).name("New-Name").save()}.
     *
     * @param connectionId the uuid of the parent connection
     * @return a {@link com.eqixiac.equinix.fabric.model.json.creators.RoutingProtocolOperator.RoutingProtocolUpdater}
     */
    RoutingProtocolOperator.RoutingProtocolUpdater update(String connectionId);

    Boolean delete(String connectionId);

    void refresh(String connectionId);
}
