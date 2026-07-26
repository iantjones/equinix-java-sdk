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

package com.eqixiac.equinix.ibxsmartview.client.implementation;

import com.eqixiac.equinix.core.client.Config;
import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.ibxsmartview.client.internal.implementation.EnvironmentalClientImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.implementation.HierarchyClientImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.implementation.LegacyEnvironmentalClientImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.implementation.LegacyPowerClientImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.implementation.PowerEventClientImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.implementation.SmartViewAssetClientImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.implementation.StreamingSubscriptionClientImpl;
import com.eqixiac.equinix.ibxsmartview.client.internal.implementation.SystemAlertClientImpl;
import lombok.Getter;

@Getter
public class IBXSmartViewConfigImpl extends Config {

    private final EnvironmentalClientImpl environmentalClient;
    private final PowerEventClientImpl powerEventClient;
    private final StreamingSubscriptionClientImpl streamingSubscriptionClient;
    private final SystemAlertClientImpl systemAlertClient;
    private final HierarchyClientImpl hierarchyClient;
    private final SmartViewAssetClientImpl smartViewAssetClient;
    private final LegacyEnvironmentalClientImpl legacyEnvironmentalClient;
    private final LegacyPowerClientImpl legacyPowerClient;

    public IBXSmartViewConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.environmentalClient = new EnvironmentalClientImpl(this);
        this.powerEventClient = new PowerEventClientImpl(this);
        this.streamingSubscriptionClient = new StreamingSubscriptionClientImpl(this);
        this.systemAlertClient = new SystemAlertClientImpl(this);
        this.hierarchyClient = new HierarchyClientImpl(this);
        this.smartViewAssetClient = new SmartViewAssetClientImpl(this);
        this.legacyEnvironmentalClient = new LegacyEnvironmentalClientImpl(this);
        this.legacyPowerClient = new LegacyPowerClientImpl(this);
    }
}
