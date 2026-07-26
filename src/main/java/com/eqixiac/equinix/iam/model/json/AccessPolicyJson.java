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

package com.eqixiac.equinix.iam.model.json;

import com.eqixiac.equinix.iam.model.AccessPolicy;
import com.eqixiac.equinix.iam.model.PolicyExpression;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Read-only JSON model for the IAM {@code AccessPolicy} response. Implements
 * {@link AccessPolicy} directly.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessPolicyJson implements AccessPolicy {

    @JsonProperty("accessPolicyId")
    private String accessPolicyId;

    @JsonProperty("ern")
    private String ern;

    @JsonProperty("description")
    private String description;

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonProperty("permissions")
    private List<PolicyExpression> permissions;

    @JsonProperty("intersect")
    private List<PolicyExpression> intersect;

    @JsonProperty("subtract")
    private List<PolicyExpression> subtract;

    @JsonProperty("rev")
    private String rev;

    @JsonProperty("disabledPolicy")
    private Boolean disabledPolicy;

    @JsonProperty("managed")
    private Boolean managed;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedBy")
    private String updatedBy;

    @JsonProperty("updatedAt")
    private String updatedAt;

    @JsonProperty("approvedAt")
    private String approvedAt;

    @JsonProperty("approvedBy")
    private String approvedBy;
}
