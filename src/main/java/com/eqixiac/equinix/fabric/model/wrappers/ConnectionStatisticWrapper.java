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

import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.ConnectionClientImpl;
import com.eqixiac.equinix.fabric.model.Connection;
import com.eqixiac.equinix.fabric.model.ConnectionStatistic;
import com.eqixiac.equinix.fabric.model.json.ConnectionStatisticJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 *
 * @author ianjones
 */
public class ConnectionStatisticWrapper extends ResourceImpl<Connection> implements ConnectionStatistic {

    @Delegate(excludes = ConnectionStatisticMutability.class)
    private ConnectionStatisticJson jsonObject;
    @Getter
    private final PageablePost<Connection> serviceClient;

    public ConnectionStatisticWrapper(ConnectionStatisticJson connectionStatisticJson, PageablePost<Connection> serviceClient) {
        this.jsonObject = connectionStatisticJson;
        this.serviceClient = serviceClient;
    }

    public ConnectionStatistic refresh() {
        this.jsonObject = ((ConnectionClientImpl)this.serviceClient).refreshStatistics(this.getUuid(),
                this.getStats().getStartDateTime(), this.getStats().getEndDateTime(), this.getStats().getViewPoint());
        return this;
    }

    private interface ConnectionStatisticMutability {
        ConnectionStatistic refresh();
    }
}
