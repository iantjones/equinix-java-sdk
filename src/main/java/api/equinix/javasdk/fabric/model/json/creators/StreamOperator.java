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
import api.equinix.javasdk.fabric.client.internal.implementation.StreamClientImpl;
import api.equinix.javasdk.fabric.enums.StreamType;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.Stream;
import api.equinix.javasdk.fabric.model.json.StreamJson;
import api.equinix.javasdk.fabric.model.wrappers.StreamWrapper;
import lombok.Getter;

/**
 *
 * @author ianjones
 */
public class StreamOperator extends ResourceImpl<Stream> {

    @Getter
    private final Pageable<Stream> serviceClient;

    public StreamOperator(Pageable<Stream> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public StreamBuilder create() {
        return new StreamBuilder();
    }

    /**
     * <p>Begins a fluent update of an existing stream, pre-populated with its current
     * state. Streams are updated with a {@code PUT} whose body carries the writable fields
     * ({@code name}, {@code description}); the returned builder is seeded from
     * {@code existing} so callers need only change the fields they want, then call {@code save()}.</p>
     *
     * @param existing the current JSON state of the stream to update
     * @return a seeded {@link api.equinix.javasdk.fabric.model.json.creators.StreamOperator.StreamBuilder}
     */
    public StreamBuilder update(StreamJson existing) {
        StreamBuilder builder = new StreamBuilder();
        builder.targetUuid = existing.getUuid();
        builder.type = existing.getType();
        builder.name = existing.getName();
        builder.description = existing.getDescription();
        builder.project = existing.getProject();
        builder.enabled = existing.getEnabled();
        return builder;
    }

    @Getter
    public class StreamBuilder {

        private String targetUuid;
        private StreamType type;
        private String name;
        private String description;
        private Project project;
        private Boolean enabled;

        protected StreamBuilder() {
        }

        public StreamOperator.StreamBuilder withType(StreamType type) {
            this.type = type;
            return this;
        }

        public StreamOperator.StreamBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public StreamOperator.StreamBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public StreamOperator.StreamBuilder withProject(Project project) {
            this.project = project;
            return this;
        }

        public StreamOperator.StreamBuilder withEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Stream create() {
            if (targetUuid != null) {
                throw new IllegalStateException("This builder targets an existing stream; call save() to update, not create().");
            }
            StreamCreatorJson streamCreatorJson = new StreamCreatorJson(this);
            StreamJson streamJson = ((StreamClientImpl) StreamOperator.this.getServiceClient()).create(streamCreatorJson);
            return new StreamWrapper(streamJson, StreamOperator.this.getServiceClient());
        }

        /**
         * Applies the accumulated changes to the existing stream ({@code PUT} carrying the
         * writable {@code name}/{@code description} fields) and returns
         * the model refreshed from the server. Only valid on a builder obtained via
         * {@link StreamOperator#update(StreamJson)}.
         *
         * @return the updated {@link api.equinix.javasdk.fabric.model.Stream}
         */
        public Stream save() {
            if (targetUuid == null) {
                throw new IllegalStateException("save() requires update(...); use create() for new streams.");
            }
            StreamCreatorJson streamCreatorJson = new StreamCreatorJson(this, true);
            StreamJson streamJson = ((StreamClientImpl) StreamOperator.this.getServiceClient()).update(targetUuid, streamCreatorJson);
            return new StreamWrapper(streamJson, StreamOperator.this.getServiceClient());
        }
    }
}
