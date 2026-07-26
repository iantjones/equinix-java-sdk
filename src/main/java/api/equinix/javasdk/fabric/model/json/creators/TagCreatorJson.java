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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Request body for creating a Fabric resource tag ({@code POST /tags}).
 *
 * <p>Prefer {@code builder()} over the positional constructor — all three parameters are
 * {@code String}s, so builder construction is self-documenting and transposition-proof.</p>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagCreatorJson {

    @JsonProperty("type")
    private final String type;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("displayName")
    private final String displayName;

    /**
     * Positional constructor; the argument order is pinned by this signature.
     *
     * @param type        the tag type
     * @param name        the tag name
     * @param displayName the tag display name
     * @deprecated use {@code builder()} — three same-typed {@code String} parameters make
     *             positional construction transposition-prone; scheduled for removal at the
     *             next major version
     */
    @Deprecated
    @Builder
    public TagCreatorJson(String type, String name, String displayName) {
        this.type = type;
        this.name = name;
        this.displayName = displayName;
    }
}
