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

package com.eqixiac.equinix.fabric.model.json;

import com.eqixiac.equinix.fabric.model.PrivateService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * Response wrapper for {@code GET /fabric/v4/companyProfiles/{companyProfileId}/privateServices},
 * which returns a non-paginated {@code { "data": [...] }} envelope.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PrivateServiceListResponseJson {

    @Getter static TypeReference<PrivateServiceListResponseJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("data")
    private List<PrivateServiceJson> data;

    public List<? extends PrivateService> getPrivateServices() {
        return data;
    }
}
