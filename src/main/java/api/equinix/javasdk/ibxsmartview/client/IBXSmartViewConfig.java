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

package api.equinix.javasdk.ibxsmartview.client;

import api.equinix.javasdk.ibxsmartview.client.internal.EnvironmentalClient;
import api.equinix.javasdk.ibxsmartview.client.internal.HierarchyClient;
import api.equinix.javasdk.ibxsmartview.client.internal.LegacyEnvironmentalClient;
import api.equinix.javasdk.ibxsmartview.client.internal.LegacyPowerClient;
import api.equinix.javasdk.ibxsmartview.client.internal.PowerClient;
import api.equinix.javasdk.ibxsmartview.client.internal.SmartViewAssetClient;
import api.equinix.javasdk.ibxsmartview.client.internal.StreamingSubscriptionClient;
import api.equinix.javasdk.ibxsmartview.client.internal.SystemAlertClient;
import api.equinix.javasdk.ibxsmartview.model.LocationHierarchy;
import api.equinix.javasdk.ibxsmartview.model.PowerReading;
import api.equinix.javasdk.ibxsmartview.model.SensorReading;
import api.equinix.javasdk.ibxsmartview.model.SmartViewAsset;
import api.equinix.javasdk.ibxsmartview.model.StreamingSubscription;
import api.equinix.javasdk.ibxsmartview.model.SystemAlert;

public interface IBXSmartViewConfig {

    EnvironmentalClient<SensorReading> getEnvironmentalClient();

    PowerClient<PowerReading> getPowerClient();

    StreamingSubscriptionClient<StreamingSubscription> getStreamingSubscriptionClient();

    SystemAlertClient<SystemAlert> getSystemAlertClient();

    HierarchyClient<LocationHierarchy> getHierarchyClient();

    SmartViewAssetClient<SmartViewAsset> getSmartViewAssetClient();

    LegacyEnvironmentalClient getLegacyEnvironmentalClient();

    LegacyPowerClient getLegacyPowerClient();
}
