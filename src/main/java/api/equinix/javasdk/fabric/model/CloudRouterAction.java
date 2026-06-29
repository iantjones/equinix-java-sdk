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

import api.equinix.javasdk.fabric.enums.CloudRouterActionState;
import api.equinix.javasdk.fabric.enums.CloudRouterActionType;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.RouterActionsConnection;
import api.equinix.javasdk.fabric.model.implementation.RouterActionsRouter;

/**
 * A route-table / BGP-session action issued against a Fabric Cloud Router (for example a route
 * table refresh) and its status. Read-only.
 */
public interface CloudRouterAction {

    String getHref();

    String getUuid();

    CloudRouterActionType getType();

    String getDescription();

    CloudRouterActionState getState();

    RouterActionsConnection getConnection();

    RouterActionsRouter getRouter();

    ChangeLog getChangeLog();
}
