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

import api.equinix.javasdk.fabric.enums.IpBlockOwnership;
import api.equinix.javasdk.fabric.enums.IpBlockProductType;
import api.equinix.javasdk.fabric.enums.IpBlockState;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Error;
import api.equinix.javasdk.fabric.model.implementation.IpBlockAccount;
import api.equinix.javasdk.fabric.model.implementation.IpBlockAsset;
import api.equinix.javasdk.fabric.model.implementation.IpBlockChange;
import api.equinix.javasdk.fabric.model.implementation.IpBlockLocation;
import api.equinix.javasdk.fabric.model.implementation.IpBlockOrder;
import api.equinix.javasdk.fabric.model.implementation.IpBlockRegulations;
import api.equinix.javasdk.fabric.model.json.creators.IpBlockOperator;

import java.util.List;

/**
 * An IP block (BYOIP / Equinix-owned IPv4 or IPv6 prefix) managed through Fabric.
 */
public interface IpBlock {

    String getUuid();

    String getHref();

    IpBlockProductType getType();

    IpBlockState getState();

    IpBlockOwnership getOwnership();

    Integer getPrefixLength();

    String getPrefix();

    Project getProject();

    IpBlockLocation getLocation();

    IpBlockOrder getOrder();

    IpBlockAccount getAccount();

    IpBlockRegulations getRegulations();

    List<IpBlockAsset> getAssets();

    IpBlockChange getChange();

    Error getError();

    ChangeLog getChangeLog();

    /**
     * Begins a fluent PATCH update of this IP block.
     *
     * @return a {@link IpBlockOperator.IpBlockUpdater}
     */
    IpBlockOperator.IpBlockUpdater update();

    Boolean delete();

    void refresh();
}
