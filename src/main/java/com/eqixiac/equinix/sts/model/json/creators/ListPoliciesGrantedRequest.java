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

package com.eqixiac.equinix.sts.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Request body for {@code POST /v1/accessPoliciesGranted} (operationId
 * {@code listAccessPoliciesGranted}, spec schema {@code ListPoliciesGrantedInput}) — lists the
 * access policies granted to the subject identified by a token within a project.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ListPoliciesGrantedRequest {

    @JsonProperty("projectId")
    private String projectId;

    @JsonProperty("subjectToken")
    private String subjectToken;

    @JsonProperty("subjectTokenType")
    private String subjectTokenType;

    @JsonProperty("pageSize")
    private Integer pageSize;

    @JsonProperty("pageToken")
    private String pageToken;

    /**
     * Sets the project identifier (required).
     *
     * @param projectId the project id
     * @return this request for chaining
     */
    public ListPoliciesGrantedRequest projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    /**
     * Sets the ID or access token identifying the subject to list granted policies for (required).
     *
     * @param subjectToken the subject token
     * @return this request for chaining
     */
    public ListPoliciesGrantedRequest subjectToken(String subjectToken) {
        this.subjectToken = subjectToken;
        return this;
    }

    /**
     * Sets the subject token type (required), e.g.
     * {@code urn:ietf:params:oauth:token-type:id_token}.
     *
     * @param subjectTokenType the subject token type
     * @return this request for chaining
     */
    public ListPoliciesGrantedRequest subjectTokenType(String subjectTokenType) {
        this.subjectTokenType = subjectTokenType;
        return this;
    }

    /**
     * Sets the maximum number of results per page.
     *
     * @param pageSize the page size
     * @return this request for chaining
     */
    public ListPoliciesGrantedRequest pageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Sets the opaque page token from a prior response.
     *
     * @param pageToken the page token
     * @return this request for chaining
     */
    public ListPoliciesGrantedRequest pageToken(String pageToken) {
        this.pageToken = pageToken;
        return this;
    }
}
