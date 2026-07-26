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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.eqixiac.equinix.fabric.enums.StreamType;
import com.eqixiac.equinix.fabric.model.Project;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
public class StreamCreatorJson {

    @JsonProperty("type")
    private StreamType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("project")
    private Project project;

    public StreamCreatorJson(StreamOperator.StreamBuilder streamBuilder) {
        this(streamBuilder, false);
    }

    /**
     * Builds the request body. Create ({@code POST}) uses the full {@code StreamPostRequest}
     * shape; update ({@code PUT}) uses the {@code StreamPutRequest} shape, which only accepts
     * {@code name} and {@code description} (unset fields are omitted via the mapper's
     * {@code NON_NULL} inclusion).
     *
     * @param streamBuilder the source builder
     * @param forUpdate {@code true} to produce a {@code StreamPutRequest}-shaped body
     */
    public StreamCreatorJson(StreamOperator.StreamBuilder streamBuilder, boolean forUpdate) {
        this.name = streamBuilder.getName();
        this.description = streamBuilder.getDescription();
        if (!forUpdate) {
            this.type = streamBuilder.getType();
            this.project = streamBuilder.getProject();
        }
    }
}
