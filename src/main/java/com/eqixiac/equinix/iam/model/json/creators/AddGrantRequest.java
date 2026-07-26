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

package com.eqixiac.equinix.iam.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Request body for adding a grant to an IAM access policy via {@code POST
 * /v1/projects/{projectId}/accessPolicies/{accessPolicyId}/grants} (operationId {@code addGrant},
 * spec schema {@code AddGrantBody}).
 *
 * <p>The {@code lastRev} field supports optimistic concurrency control.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class AddGrantRequest {

    @JsonProperty("grantee")
    private String grantee;

    @JsonProperty("lastRev")
    private String lastRev;

    /**
     * Sets the grantee (required) — the principal receiving the grant.
     *
     * @param grantee the grantee
     * @return this request for chaining
     */
    public AddGrantRequest grantee(String grantee) {
        this.grantee = grantee;
        return this;
    }

    /**
     * Sets the last known revision for optimistic concurrency control.
     *
     * @param lastRev the last revision
     * @return this request for chaining
     */
    public AddGrantRequest lastRev(String lastRev) {
        this.lastRev = lastRev;
        return this;
    }
}
