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

import com.eqixiac.equinix.fabric.enums.RouteTableEntryProtocolType;
import com.eqixiac.equinix.fabric.enums.RouteTableEntryType;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.RouteTableEntryConnection;

import java.util.List;

/**
 * A single route table entry. Used for advertised/received routes on a connection and for
 * Fabric Cloud Router route table searches. Read-only.
 */
public interface RouteTableEntry {

    RouteTableEntryType getType();

    RouteTableEntryProtocolType getProtocolType();

    String getState();

    String getPrefix();

    String getNextHop();

    Integer getMED();

    Integer getLocalPreference();

    List<String> getAsPath();

    RouteTableEntryConnection getConnection();

    ChangeLog getChangeLog();
}
