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

import com.eqixiac.equinix.fabric.enums.IpBlockOwnership;
import com.eqixiac.equinix.fabric.enums.IpBlockProductType;
import com.eqixiac.equinix.fabric.enums.IpBlockState;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.Error;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockAccount;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockAsset;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockChange;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockLocation;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockOrder;
import com.eqixiac.equinix.fabric.model.implementation.IpBlockRegulations;
import com.eqixiac.equinix.fabric.model.json.creators.IpBlockOperator;

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
