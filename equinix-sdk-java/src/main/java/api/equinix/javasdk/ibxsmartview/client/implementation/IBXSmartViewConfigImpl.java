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

package api.equinix.javasdk.ibxsmartview.client.implementation;

import api.equinix.javasdk.core.client.Config;
import api.equinix.javasdk.core.client.EquinixClient;
import api.equinix.javasdk.ibxsmartview.client.IBXSmartViewConfig;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.EnvironmentalClientImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.HierarchyClientImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.LegacyEnvironmentalClientImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.LegacyPowerClientImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.PowerClientImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.SmartViewAssetClientImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.StreamingSubscriptionClientImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.SystemAlertClientImpl;
import lombok.Getter;

@Getter
public class IBXSmartViewConfigImpl extends Config implements IBXSmartViewConfig {

    private final EnvironmentalClientImpl environmentalClient;
    private final PowerClientImpl powerClient;
    private final StreamingSubscriptionClientImpl streamingSubscriptionClient;
    private final SystemAlertClientImpl systemAlertClient;
    private final HierarchyClientImpl hierarchyClient;
    private final SmartViewAssetClientImpl smartViewAssetClient;
    private final LegacyEnvironmentalClientImpl legacyEnvironmentalClient;
    private final LegacyPowerClientImpl legacyPowerClient;

    public IBXSmartViewConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.environmentalClient = new EnvironmentalClientImpl(this);
        this.powerClient = new PowerClientImpl(this);
        this.streamingSubscriptionClient = new StreamingSubscriptionClientImpl(this);
        this.systemAlertClient = new SystemAlertClientImpl(this);
        this.hierarchyClient = new HierarchyClientImpl(this);
        this.smartViewAssetClient = new SmartViewAssetClientImpl(this);
        this.legacyEnvironmentalClient = new LegacyEnvironmentalClientImpl(this);
        this.legacyPowerClient = new LegacyPowerClientImpl(this);
    }
}
