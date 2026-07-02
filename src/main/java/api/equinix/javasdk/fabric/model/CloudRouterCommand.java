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

import api.equinix.javasdk.fabric.enums.CloudRouterCommandState;
import api.equinix.javasdk.fabric.enums.CloudRouterCommandType;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.CloudRouterCommandRequest;
import api.equinix.javasdk.fabric.model.implementation.CloudRouterCommandResponse;

/**
 * A Fabric Cloud Router diagnostic command (ping / traceroute) and its result. Read-only.
 */
public interface CloudRouterCommand {

    String getHref();

    String getUuid();

    CloudRouterCommandType getType();

    String getName();

    String getDescription();

    CloudRouterCommandState getState();

    Project getProject();

    CloudRouterCommandRequest getRequest();

    CloudRouterCommandResponse getResponse();

    ChangeLog getChangeLog();
}
