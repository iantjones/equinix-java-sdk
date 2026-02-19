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
 * <p>StreamOperator class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class StreamOperator extends ResourceImpl<Stream> {

    @Getter
    private final Pageable<Stream> serviceClient;

    /**
     * <p>Constructor for StreamOperator.</p>
     *
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public StreamOperator(Pageable<Stream> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * <p>create.</p>
     *
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.StreamOperator.StreamBuilder} object.
     */
    public StreamBuilder create() {
        return new StreamBuilder();
    }

    @Getter
    public class StreamBuilder {

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
            StreamCreatorJson streamCreatorJson = new StreamCreatorJson(this);
            StreamJson streamJson = ((StreamClientImpl) StreamOperator.this.getServiceClient()).create(streamCreatorJson);
            return new StreamWrapper(streamJson, StreamOperator.this.getServiceClient());
        }
    }
}
