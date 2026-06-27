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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.customerportal.enums.NegotiationAction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Request body for replying to an order negotiation.
 * Carries the {@code action} to perform, the {@code referenceId} of the activity or
 * order line, and an optional {@code reason} (used when cancelling a negotiation).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NegotiationsRequestJson {

    @JsonProperty("referenceId")
    private final String referenceId;

    @JsonProperty("action")
    private final NegotiationAction action;

    @JsonProperty("reason")
    private final String reason;

    public NegotiationsRequestJson(NegotiationAction action, String referenceId, String reason) {
        this.action = action;
        this.referenceId = referenceId;
        this.reason = reason;
    }
}
