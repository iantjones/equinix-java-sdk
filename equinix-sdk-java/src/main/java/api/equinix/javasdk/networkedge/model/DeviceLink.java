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
import api.equinix.javasdk.networkedge.enums.Source;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator;
import api.equinix.javasdk.networkedge.model.implementation.DeviceLinkSupportDetail;
import api.equinix.javasdk.networkedge.model.implementation.Link;
import api.equinix.javasdk.networkedge.model.implementation.LinkDevice;
import api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkUpdaterJson;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>DeviceLink interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface DeviceLink {

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>getSource.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.Source} object.
     */
    Source getSource();

    /**
     * <p>getGroupName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getGroupName();

    /**
     * <p>getSubnet.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getSubnet();

    /**
     * <p>getStatus.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.enums.DeviceLinkStatus} object.
     */
    DeviceLinkStatus getStatus();

    /**
     * <p>getLinks.</p>
     *
     * @return a {@link java.util.List} object.
     */
    List<Link> getLinks();

    /**
     * <p>getLinkDevices.</p>
     *
     * @return a {@link java.util.List} object.
     */
    List<LinkDevice> getLinkDevices();

    /**
     * <p>getSupportDetails.</p>
     *
     * @return a {@link java.util.List} object.
     */
    List<DeviceLinkSupportDetail> getSupportDetails();

    /**
     * <p>getCreatedBy.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getCreatedBy();

    /**
     * <p>getCreatedDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    LocalDateTime getCreatedDate();

    /**
     * <p>getLastUpdatedBy.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getLastUpdatedBy();

    /**
     * <p>getLastUpdatedDate.</p>
     *
     * @return a {@link java.time.LocalDateTime} object.
     */
    LocalDateTime getLastUpdatedDate();

    /**
     * <p>update.</p>
     *
     * @return a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkOperator.DeviceLinkUpdater} object.
     */
    DeviceLinkOperator.DeviceLinkUpdater update();

    /**
     * <p>save.</p>
     *
     * @param updaterJson a {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceLinkUpdaterJson} object.
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean save(DeviceLinkUpdaterJson updaterJson);

    /**
     * <p>delete.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean delete();

    /**
     * <p>refresh.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    Boolean refresh();
}
