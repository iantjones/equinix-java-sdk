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
import api.equinix.javasdk.fabric.model.implementation.StreamSink;
import api.equinix.javasdk.fabric.model.implementation.StreamSinkCredential;
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

    /**
     * <p>Begins a fluent full-body update of an existing stream subscription, pre-populated with its
     * current state. Subscriptions are updated with a full {@code PUT}; the returned builder is seeded
     * from {@code existing} so callers need only change the fields they want, then call {@code save()}.</p>
     *
     * @param streamId a {@link java.lang.String} object identifying the parent stream.
     * @param existing the current JSON state of the stream subscription to update.
     * @return a seeded {@link api.equinix.javasdk.fabric.model.json.creators.StreamSubscriptionOperator.StreamSubscriptionBuilder} object.
     */
    public StreamSubscriptionBuilder update(String streamId, StreamSubscriptionJson existing) {
        StreamSubscriptionBuilder builder = new StreamSubscriptionBuilder(streamId);
        builder.targetUuid = existing.getUuid();
        builder.type = existing.getType();
        builder.name = existing.getName();
        builder.description = existing.getDescription();
        builder.enabled = existing.getEnabled();

        StreamSink sink = existing.getSink();
        if (sink != null) {
            builder.sinkType = sink.getType();
            builder.sinkUri = sink.getUri();
            StreamSinkCredential credential = sink.getCredential();
            if (credential != null) {
                builder.credentialType = credential.getType();
                builder.accessToken = credential.getAccessToken();
                builder.integrationKey = credential.getIntegrationKey();
            }
        }
        return builder;
    }

    @Getter
    public class StreamSubscriptionBuilder {

        private final String streamId;
        private String targetUuid;
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
            if (targetUuid != null) {
                throw new IllegalStateException("This builder targets an existing stream subscription; call save() to update, not create().");
            }
            StreamSubscriptionCreatorJson streamSubscriptionCreatorJson = new StreamSubscriptionCreatorJson(this);
            StreamSubscriptionJson streamSubscriptionJson = ((StreamSubscriptionClientImpl) StreamSubscriptionOperator.this.getServiceClient()).create(this.streamId, streamSubscriptionCreatorJson);
            return new StreamSubscriptionWrapper(streamSubscriptionJson, StreamSubscriptionOperator.this.getServiceClient());
        }

        /**
         * Applies the accumulated changes to the existing stream subscription (full-body {@code PUT})
         * and returns the model refreshed from the server. Only valid on a builder obtained via
         * {@link StreamSubscriptionOperator#update(String, StreamSubscriptionJson)}.
         *
         * @return the updated {@link api.equinix.javasdk.fabric.model.StreamSubscription}
         */
        public StreamSubscription save() {
            if (targetUuid == null) {
                throw new IllegalStateException("save() requires update(...); use create() for new stream subscriptions.");
            }
            StreamSubscriptionCreatorJson streamSubscriptionCreatorJson = new StreamSubscriptionCreatorJson(this);
            StreamSubscriptionJson streamSubscriptionJson = ((StreamSubscriptionClientImpl) StreamSubscriptionOperator.this.getServiceClient()).update(this.streamId, targetUuid, streamSubscriptionCreatorJson);
            return new StreamSubscriptionWrapper(streamSubscriptionJson, StreamSubscriptionOperator.this.getServiceClient());
        }
    }
}
