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

package api.equinix.javasdk.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * An error reported inline on a resource (the Fabric v4 {@code Error} schema), e.g. in
 * {@code ConnectionOperation.errors} or {@code IpBlock.error}. The {@code correlationId}
 * is the diagnostic reference Equinix support asks for.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Error {

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("details")
    private String details;

    @JsonProperty("help")
    private String help;

    @JsonProperty("additionalInfo")
    private List<ErrorAdditionalInfo> additionalInfo;
}
