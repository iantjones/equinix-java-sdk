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

package com.eqixiac.equinix.fabric.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.PortStatisticClientImpl;
import com.eqixiac.equinix.fabric.model.PortStatistic;
import com.eqixiac.equinix.fabric.model.json.PortStatisticJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 *
 * @author ianjones
 */
public class PortStatisticWrapper extends ResourceImpl<PortStatistic> implements PortStatistic {

    @Delegate(excludes = PortStatisticMutability.class)
    private PortStatisticJson jsonObject;
    @Getter
    private final Pageable<PortStatistic> serviceClient;

    public PortStatisticWrapper(PortStatisticJson portJson, Pageable<PortStatistic> serviceClient) {
        this.jsonObject = portJson;
        this.serviceClient = serviceClient;
    }

    public PortStatistic refresh() {
        java.time.LocalDateTime start = this.getStartDateTime() != null
                ? this.getStartDateTime()
                : (this.getStats() != null ? this.getStats().getStartDateTime() : null);
        java.time.LocalDateTime end = this.getEndDateTime() != null
                ? this.getEndDateTime()
                : (this.getStats() != null ? this.getStats().getEndDateTime() : null);
        this.jsonObject = ((PortStatisticClientImpl)this.serviceClient).refreshStatistics(this.getUuid(), start, end);
        return this;
    }

    private interface PortStatisticMutability {
        PortStatistic refresh();
    }
}
