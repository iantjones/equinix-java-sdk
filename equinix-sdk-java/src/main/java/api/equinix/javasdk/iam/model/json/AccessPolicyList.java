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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Token-paginated JSON model for the IAM {@code AccessPolicyList} response schema. Wraps a
 * page of {@link AccessPolicyJson} items along with an opaque continuation token.
 * <p>
 * To fetch the next page, callers pass the value returned by {@link #getNextPageToken()}
 * back as the {@code pageToken} argument on the subsequent request.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessPolicyList {

    @JsonProperty("list")
    private List<AccessPolicyJson> list;

    @JsonProperty("nextPageToken")
    private String nextPageToken;
}
