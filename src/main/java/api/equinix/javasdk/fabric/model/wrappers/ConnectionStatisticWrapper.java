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

package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.ConnectionClientImpl;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.ConnectionStatistic;
import api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * <p>ConnectionStatisticWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class ConnectionStatisticWrapper extends ResourceImpl<Connection> implements ConnectionStatistic {

    @Delegate(excludes = ConnectionStatisticMutability.class)
    private ConnectionStatisticJson jsonObject;
    @Getter
    private final PageablePost<Connection> serviceClient;

    /**
     * <p>Constructor for ConnectionStatisticWrapper.</p>
     *
     * @param connectionStatisticJson a {@link api.equinix.javasdk.fabric.model.json.ConnectionStatisticJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public ConnectionStatisticWrapper(ConnectionStatisticJson connectionStatisticJson, PageablePost<Connection> serviceClient) {
        this.jsonObject = connectionStatisticJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>refresh.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.ConnectionStatistic} object.
     */
    public ConnectionStatistic refresh() {
        this.jsonObject = ((ConnectionClientImpl)this.serviceClient).refreshStatistics(this.getUuid(),
                this.getStats().getStartDateTime(), this.getStats().getEndDateTime(), this.getStats().getViewPoint());
        return this;
    }

    private interface ConnectionStatisticMutability {
        ConnectionStatistic refresh();
    }
}
