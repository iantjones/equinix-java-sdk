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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.model.KeyValuePair;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * <p>Read-only JSON model for the {@code ConnectionResponse} body returned by the connection
 * validation API ({@code POST /fabric/v4/connections/validate}). Wraps the validated
 * connection specifications ({@link #getData()}) plus any additional informational
 * key/value pairs.</p>
 *
 * @author ianjones
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionValidationResponseJson {

    @Getter static TypeReference<ConnectionValidationResponseJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("additionalInfo")
    private List<KeyValuePair> additionalInfo;

    @JsonProperty("data")
    private List<ValidateConnectionResponseJson> data;
}
