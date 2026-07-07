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

package api.equinix.javasdk.networkedge.model;

import api.equinix.javasdk.networkedge.enums.DeviceLinkStatus;
import api.equinix.javasdk.networkedge.enums.RedundancyType;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator;
import api.equinix.javasdk.networkedge.model.implementation.Link;
import api.equinix.javasdk.networkedge.model.implementation.LinkDevice;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkUpdaterJson;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author ianjones
 */
public interface DeviceLink {

    String getUuid();

    String getGroupName();

    String getSubnet();

    RedundancyType getRedundancyType();

    DeviceLinkStatus getStatus();

    List<Link> getMetroLinks();

    List<LinkDevice> getLinkDevices();

    String getCreatedBy();

    LocalDateTime getCreatedDate();

    String getLastUpdatedBy();

    LocalDateTime getLastUpdatedDate();

    DeviceLinkOperator.DeviceLinkUpdater update();

    Boolean save(DeviceLinkUpdaterJson updaterJson);

    Boolean delete();

    Boolean refresh();
}
