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

import api.equinix.javasdk.fabric.enums.RoutingProtocolState;
import api.equinix.javasdk.fabric.enums.RoutingProtocolType;
import api.equinix.javasdk.fabric.model.implementation.*;

public interface RoutingProtocol {

    String getUuid();

    String getHref();

    String getName();

    RoutingProtocolType getType();

    RoutingProtocolState getState();

    String getDescription();

    BGPConnectionIpv4 getBgpIpv4();

    BGPConnectionIpv6 getBgpIpv6();

    DirectConnectionIpv4 getDirectIpv4();

    DirectConnectionIpv6 getDirectIpv6();

    BFDConfig getBfd();

    Long getCustomerAsn();

    Long getEquinixAsn();

    ChangeLog getChangeLog();

    Change getChange();

    Boolean delete(String connectionId);

    void refresh(String connectionId);
}
