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

package com.eqixiac.equinix.internetaccess.client.internal;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.internetaccess.enums.Redundancy;
import com.eqixiac.equinix.internetaccess.enums.UseCase;
import com.eqixiac.equinix.internetaccess.model.RoutingProtocolConfiguration;
import com.eqixiac.equinix.internetaccess.model.json.RoutingProtocolConfigurationJson;

/**
 * Internal client for the Equinix Internet Access (EIA) v1 routing-configuration lookup:
 * {@code GET /internetAccess/v1/routingProtocolConfigurations} — the allowed routing-protocol
 * configurations for a given use case and redundancy.
 */
public interface RoutingConfigurationClient extends Pageable<RoutingProtocolConfiguration> {

    Page<RoutingProtocolConfigurationJson> list(UseCase useCase, Redundancy type);
}
