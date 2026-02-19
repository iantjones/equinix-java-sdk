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

import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.NetworkClientImpl;
import api.equinix.javasdk.fabric.enums.NetworkScope;
import api.equinix.javasdk.fabric.enums.NetworkType;
import api.equinix.javasdk.fabric.enums.NotificationType;
import api.equinix.javasdk.fabric.model.Network;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.implementation.MinimalLocation;
import api.equinix.javasdk.fabric.model.implementation.Notification;
import api.equinix.javasdk.fabric.model.json.NetworkJson;
import api.equinix.javasdk.fabric.model.wrappers.NetworkWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>NetworkOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class NetworkOperator extends ResourceImpl<Network> {

    @Getter
    private final PageablePost<Network> serviceClient;

    /**
     * <p>Constructor for NetworkOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.PageablePost} object.
     */
    public NetworkOperator(PageablePost<Network> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @param type a {@link api.equinix.javasdk.fabric.enums.NetworkType} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.NetworkOperator.NetworkBuilder} object.
     */
    public NetworkBuilder create(NetworkType type) {
        return new NetworkBuilder(type);
    }

    @Getter(AccessLevel.PACKAGE)
    public class NetworkBuilder {

        private final NetworkType type;
        private String name;
        private NetworkScope scope;
        private Project project;
        private MinimalLocation location;
        private List<Notification> notifications;

        protected NetworkBuilder(NetworkType type) {
            this.type = type;
        }

        public NetworkOperator.NetworkBuilder name(String name) {
            this.name = name;
            return this;
        }

        public NetworkOperator.NetworkBuilder scope(NetworkScope scope) {
            this.scope = scope;
            return this;
        }

        public NetworkOperator.NetworkBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public NetworkOperator.NetworkBuilder withLocation(MinimalLocation location) {
            this.location = location;
            return this;
        }

        public NetworkOperator.NetworkBuilder notification(NotificationType type, String emailAddress) {
            if(this.notifications == null) {
                this.notifications = Collections.singletonList(new Notification(type, Collections.singletonList(emailAddress)));
            }
            else {
                if (notifications.stream().noneMatch(o -> o.getType().equals(type))) {
                    notifications.add(new Notification(type, Collections.singletonList(emailAddress)));
                } else {
                    Objects.requireNonNull(notifications.stream().filter(o -> o.getType().equals(type)).findFirst().orElse(null)).addEmail(emailAddress);
                }
            }
            return this;
        }

        public NetworkOperator.NetworkBuilder notification(List<Notification> notifications) {
            this.notifications = notifications;
            return this;
        }

        public Network create() {
            NetworkCreatorJson networkCreatorJson = new NetworkCreatorJson(this);
            NetworkJson networkJson = ((NetworkClientImpl) NetworkOperator.this.getServiceClient()).create(networkCreatorJson);
            return new NetworkWrapper(networkJson, NetworkOperator.this.getServiceClient());
        }
    }
}
