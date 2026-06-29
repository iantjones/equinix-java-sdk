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

import api.equinix.javasdk.iam.model.ResourceTypeAction;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only JSON model for the IAM {@code ResourceTypeAction} response. Implements
 * {@link ResourceTypeAction} directly.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceTypeActionJson implements ResourceTypeAction {

    @JsonProperty("action")
    private String action;

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("resourceTypeErn")
    private String resourceTypeErn;

    @JsonProperty("rev")
    private String rev;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private String createdAt;
}
