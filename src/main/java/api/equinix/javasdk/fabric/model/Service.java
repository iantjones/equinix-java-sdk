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

import api.equinix.javasdk.fabric.enums.ServiceProfileState;
import api.equinix.javasdk.fabric.enums.ServiceProfileType;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.ServiceConfig;

import java.util.List;

/**
 * <p>Service interface.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface Service {

    /**
     * <p>getUuid.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getUuid();

    /**
     * <p>getState.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceProfileState} object.
     */
    ServiceProfileState getState();

    /**
     * <p>getTags.</p>
     *
     * @return a {@link java.util.List} object.
     */
    List<String> getTags();

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getName();

    /**
     * <p>getDescription.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    String getDescription();

    /**
     * <p>getType.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.enums.ServiceProfileType} object.
     */
    ServiceProfileType getType();

    /**
     * <p>getConfig.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ServiceConfig} object.
     */
    ServiceConfig getConfig();

    /**
     * <p>getChangeLog.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.ChangeLog} object.
     */
    ChangeLog getChangeLog();

    /**
     * <p>getAccount.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.implementation.Account} object.
     */
    Account getAccount();

    /**
     * <p>refresh.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.Service} object.
     */
    Service refresh();
}
