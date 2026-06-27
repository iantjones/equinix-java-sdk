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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.fabric.enums.*;
import api.equinix.javasdk.fabric.model.implementation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Setter(AccessLevel.PRIVATE)
public class ConnectionCreatorJson {

        @JsonProperty("type")
        private ConnectionType type;

        @JsonProperty("name")
        private String name;

        @JsonProperty("order")
        private Order order;

        @JsonProperty("bandwidth")
        private Integer bandwidth;

        @JsonProperty("redundancy")
        private Redundancy redundancy;

        @JsonProperty("aSide")
        private ConnectionSide aSide;

        @JsonProperty("zSide")
        private ConnectionSide zSide;

        @JsonProperty("notifications")
        private List<Notification> notifications;

        @AllArgsConstructor(access = AccessLevel.PACKAGE)
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        public static class ConnectionSide {
                @JsonProperty("accessPoint")
                private SimpleAccessPoint accessPoint;

                @JsonProperty("serviceToken")
                private MinimalServiceToken serviceToken;
        }

        public ConnectionCreatorJson(ConnectionOperator.ConnectionBuilder connectionBuilder) {
                this.type = connectionBuilder.getType();
                this.name = connectionBuilder.getName();
                this.bandwidth = connectionBuilder.getBandwidth();
                this.redundancy = connectionBuilder.getRedundancy();
                this.aSide = new ConnectionSide(connectionBuilder.getASideAccessPoint(), connectionBuilder.getASideServiceToken());
                this.zSide = new ConnectionSide(connectionBuilder.getZSideAccessPoint(), connectionBuilder.getZSideServiceToken());
                this.notifications = connectionBuilder.getNotifications();
        }
}
