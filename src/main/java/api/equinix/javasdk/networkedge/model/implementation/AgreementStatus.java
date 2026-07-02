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

package api.equinix.javasdk.networkedge.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

/**
 *
 * @author ianjones
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class AgreementStatus {

    @Getter static TypeReference<AgreementStatus> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("termsVersionID")
    private String termsVersionId;

    @JsonProperty("terms")
    private String terms;

    @JsonProperty("isValid")
    private Boolean valid;

    /**
     * <p>Only populated on the create-agreement response ({@code AgreementAcceptResponse.status});
     * the API returns {@code SUCCESS} or {@code FAILED}.</p>
     */
    @JsonProperty("status")
    private api.equinix.javasdk.networkedge.enums.AgreementStatus status;

    @JsonProperty("errorMessage")
    private String errorMessage;
}
