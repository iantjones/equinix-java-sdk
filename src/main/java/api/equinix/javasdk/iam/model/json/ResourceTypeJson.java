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

package api.equinix.javasdk.iam.model.json;

import api.equinix.javasdk.iam.model.Attribute;
import api.equinix.javasdk.iam.model.ResourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Read-only JSON model for the IAM {@code ResourceType} response. Implements
 * {@link ResourceType} directly.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceTypeJson implements ResourceType {

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("ern")
    private String ern;

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonProperty("attributes")
    private List<AttributeJson> attributes;

    @JsonProperty("rev")
    private String rev;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedBy")
    private String updatedBy;

    @JsonProperty("updatedAt")
    private String updatedAt;

    @JsonProperty("approvedBy")
    private String approvedBy;

    @JsonProperty("approvedAt")
    private String approvedAt;

    @Override
    public List<Attribute> getAttributes() {
        if (attributes == null) {
            return null;
        }
        return java.util.Collections.unmodifiableList(attributes);
    }
}
