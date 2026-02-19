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

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.client.internal.implementation.StreamSubscriptionClientImpl;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionSinkType;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionType;
import api.equinix.javasdk.fabric.model.Stream;
import api.equinix.javasdk.fabric.model.StreamSubscription;
import api.equinix.javasdk.fabric.model.json.StreamSubscriptionJson;
import api.equinix.javasdk.fabric.model.wrappers.StreamSubscriptionWrapper;
import lombok.Getter;

/**
 * <p>StreamSubscriptionOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class StreamSubscriptionOperator extends ResourceImpl<StreamSubscription> {

    @Getter
    private final Pageable<StreamSubscription> serviceClient;

    /**
     * <p>Constructor for StreamSubscriptionOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public StreamSubscriptionOperator(Pageable<StreamSubscription> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @param streamId a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.StreamSubscriptionOperator.StreamSubscriptionBuilder} object.
     */
    public StreamSubscriptionBuilder create(String streamId) {
        return new StreamSubscriptionBuilder(streamId);
    }

    public StreamSubscriptionBuilder create(Stream stream) {
        return create(stream.getUuid());
    }

    @Getter
    public class StreamSubscriptionBuilder {

        private final String streamId;
        private StreamSubscriptionType type;
        private String name;
        private String description;
        private Boolean enabled;
        private StreamSubscriptionSinkType sinkType;
        private String sinkUri;
        private String credentialType;
        private String accessToken;
        private String integrationKey;

        protected StreamSubscriptionBuilder(String streamId) {
            this.streamId = streamId;
        }

        public StreamSubscriptionOperator.StreamSubscriptionBuilder withType(StreamSubscriptionType type) {
            this.type = type;
            return this;
        }

        public StreamSubscriptionOperator.StreamSubscriptionBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public StreamSubscriptionOperator.StreamSubscriptionBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public StreamSubscriptionOperator.StreamSubscriptionBuilder withEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public StreamSubscriptionOperator.StreamSubscriptionBuilder withSinkType(StreamSubscriptionSinkType sinkType) {
            this.sinkType = sinkType;
            return this;
        }

        public StreamSubscriptionOperator.StreamSubscriptionBuilder withSinkUri(String sinkUri) {
            this.sinkUri = sinkUri;
            return this;
        }

        public StreamSubscriptionOperator.StreamSubscriptionBuilder withCredentialType(String credentialType) {
            this.credentialType = credentialType;
            return this;
        }

        public StreamSubscriptionOperator.StreamSubscriptionBuilder withAccessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public StreamSubscriptionOperator.StreamSubscriptionBuilder withIntegrationKey(String integrationKey) {
            this.integrationKey = integrationKey;
            return this;
        }

        public StreamSubscription create() {
            StreamSubscriptionCreatorJson streamSubscriptionCreatorJson = new StreamSubscriptionCreatorJson(this);
            StreamSubscriptionJson streamSubscriptionJson = ((StreamSubscriptionClientImpl) StreamSubscriptionOperator.this.getServiceClient()).create(this.streamId, streamSubscriptionCreatorJson);
            return new StreamSubscriptionWrapper(streamSubscriptionJson, StreamSubscriptionOperator.this.getServiceClient());
        }
    }
}
