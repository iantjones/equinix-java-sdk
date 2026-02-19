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

package api.equinix.javasdk.internetaccess.client;

import api.equinix.javasdk.internetaccess.client.internal.InternetAccessServiceClient;
import api.equinix.javasdk.internetaccess.client.internal.InternetAccessPortClient;
import api.equinix.javasdk.internetaccess.client.internal.RoutingConfigClient;
import api.equinix.javasdk.internetaccess.model.InternetAccessService;
import api.equinix.javasdk.internetaccess.model.InternetAccessPort;
import api.equinix.javasdk.internetaccess.model.RoutingConfig;

public interface InternetAccessConfig {

    InternetAccessServiceClient<InternetAccessService> getInternetAccessServiceClient();

    InternetAccessPortClient<InternetAccessPort> getInternetAccessPortClient();

    RoutingConfigClient<RoutingConfig> getRoutingConfigClient();
}
