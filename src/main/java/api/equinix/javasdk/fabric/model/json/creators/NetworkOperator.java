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

import api.equinix.javasdk.core.http.request.PatchOperation;
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
import java.util.List;
import java.util.Objects;

/**
 *
 * @author ianjones
 */
public class NetworkOperator extends ResourceImpl<Network> {

    @Getter
    private final PageablePost<Network> serviceClient;

    public NetworkOperator(PageablePost<Network> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public NetworkBuilder create(NetworkType type) {
        return new NetworkBuilder(type);
    }

    /**
     * <p>Begins a fluent update of an existing network, identified by uuid.</p>
     *
     * @param uuid the uuid of the network to update
     */
    public NetworkUpdater update(String uuid) {
        return new NetworkUpdater(uuid);
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
                this.notifications = new ArrayList<>(List.of(new Notification(type, new ArrayList<>(List.of(emailAddress)))));
            }
            else {
                if (notifications.stream().noneMatch(o -> o.getType().equals(type))) {
                    notifications.add(new Notification(type, new ArrayList<>(List.of(emailAddress))));
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

    /**
     * Fluent builder for updating an existing network via RFC&nbsp;6902 JSON Patch. Each setter records
     * a {@code replace} operation; {@link #save()} sends the accumulated operations as a single
     * {@code PATCH} with content-type {@code application/json-patch+json} and returns the refreshed model.
     *
     * <pre>{@code network.update().name("New-Name").save();}</pre>
     */
    public class NetworkUpdater {

        private final String uuid;
        private final List<PatchOperation> operations = new ArrayList<>();

        protected NetworkUpdater(String uuid) {
            this.uuid = uuid;
        }

        /**
         * Replaces the network name.
         *
         * @param name the new name
         * @return this updater
         */
        public NetworkUpdater name(String name) {
            operations.add(PatchOperation.replace("/name", name));
            return this;
        }

        /**
         * Replaces the notification configuration.
         *
         * @param notifications the new notification list
         * @return this updater
         */
        public NetworkUpdater notifications(List<Notification> notifications) {
            operations.add(PatchOperation.replace("/notifications", notifications));
            return this;
        }

        /**
         * Adds an arbitrary JSON Patch operation, for paths not covered by the typed setters above.
         *
         * @param operation the patch operation
         * @return this updater
         */
        public NetworkUpdater patch(PatchOperation operation) {
            operations.add(operation);
            return this;
        }

        /**
         * Applies the accumulated changes and returns the network refreshed from the server's response.
         *
         * @return the updated {@link api.equinix.javasdk.fabric.model.Network}
         */
        public Network save() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("No changes specified; set at least one field before calling save().");
            }
            NetworkJson networkJson = ((NetworkClientImpl) NetworkOperator.this.getServiceClient()).update(uuid, operations);
            return new NetworkWrapper(networkJson, NetworkOperator.this.getServiceClient());
        }
    }
}
