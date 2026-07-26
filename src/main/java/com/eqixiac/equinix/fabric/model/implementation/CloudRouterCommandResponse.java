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

package com.eqixiac.equinix.fabric.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Result payload of a Cloud Router diagnostic command (spec schema
 * {@code CloudRouterCommandResponse}, a oneOf of {@code CloudRouterCommandPingResponse} and
 * {@code CloudRouterCommandTracerouteResponse}). For a ping command
 * {@code outputStructuredPing} is populated; for a traceroute command
 * {@code outputStructuredTraceroute} is populated. {@code output} carries the raw textual output
 * in both cases.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudRouterCommandResponse {

    @JsonProperty("output")
    private String output;

    @JsonProperty("outputStructuredPing")
    private OutputStructuredPing outputStructuredPing;

    @JsonProperty("outputStructuredTraceroute")
    private OutputStructuredTraceroute outputStructuredTraceroute;

    @JsonProperty("errors")
    private List<Error> errors;
}
