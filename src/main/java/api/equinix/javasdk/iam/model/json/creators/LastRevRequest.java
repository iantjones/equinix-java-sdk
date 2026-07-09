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

package api.equinix.javasdk.iam.model.json.creators;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Request body carrying only a revision marker (spec schema {@code LastRevBody}), used by the IAM
 * enable/disable operations (e.g. {@code enablePrincipalPolicy}, {@code disablePrincipalPolicy}).
 *
 * <p>The {@code lastRev} field supports optimistic concurrency control.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class LastRevRequest {

    @JsonProperty("lastRev")
    private String lastRev;

    /**
     * Creates an empty request; populate via {@link #lastRev(String)}.
     */
    public LastRevRequest() {
    }

    /**
     * Creates a request with the supplied revision marker.
     *
     * @param lastRev the last revision
     */
    public LastRevRequest(String lastRev) {
        this.lastRev = lastRev;
    }

    /**
     * Sets the last known revision for optimistic concurrency control.
     *
     * @param lastRev the last revision
     * @return this request for chaining
     */
    public LastRevRequest lastRev(String lastRev) {
        this.lastRev = lastRev;
        return this;
    }
}
