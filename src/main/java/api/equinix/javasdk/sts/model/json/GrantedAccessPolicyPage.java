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

package api.equinix.javasdk.sts.model.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A paginated list of the access policy ids granted to a subject within a project (spec schema
 * {@code ListAccessPoliciesGrantedOutput}), as returned by {@code POST /v1/accessPoliciesGranted}
 * (operationId {@code listAccessPoliciesGranted}).
 *
 * <p>Callers fetch the next page by passing {@code getNextPageToken()} back as the input's
 * {@code pageToken}.</p>
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrantedAccessPolicyPage {

    @JsonProperty("list")
    private List<GrantedAccessPolicy> list;

    @JsonProperty("nextPageToken")
    private String nextPageToken;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrantedAccessPolicy {

        @JsonProperty("accessPolicyId")
        private String accessPolicyId;
    }
}
