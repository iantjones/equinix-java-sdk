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

package api.equinix.javasdk.fabric.model.json.creators;

import api.equinix.javasdk.fabric.enums.StreamSubscriptionSinkType;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionType;
import api.equinix.javasdk.fabric.model.implementation.StreamSinkSetting;
import api.equinix.javasdk.fabric.model.implementation.StreamSubscriptionFilter;
import api.equinix.javasdk.fabric.model.implementation.StreamSubscriptionSelector;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
public class StreamSubscriptionCreatorJson {

    @JsonProperty("type")
    private StreamSubscriptionType type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("metricSelector")
    private StreamSubscriptionSelector metricSelector;

    @JsonProperty("eventSelector")
    private StreamSubscriptionSelector eventSelector;

    @JsonProperty("filters")
    private StreamSubscriptionFilter filters;

    @JsonProperty("sink")
    private Sink sink;

    @Setter(AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Sink {

        @JsonProperty("type")
        private StreamSubscriptionSinkType type;

        @JsonProperty("uri")
        private String uri;

        @JsonProperty("host")
        private String host;

        @JsonProperty("batchEnabled")
        private Boolean batchEnabled;

        @JsonProperty("batchSizeMax")
        private Integer batchSizeMax;

        @JsonProperty("batchWaitTimeMax")
        private Integer batchWaitTimeMax;

        @JsonProperty("settings")
        private StreamSinkSetting settings;

        @JsonProperty("credential")
        private Credential credential;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Credential {

        @JsonProperty("type")
        private String type;

        @JsonProperty("accessToken")
        private String accessToken;

        @JsonProperty("integrationKey")
        private String integrationKey;

        @JsonProperty("apiKey")
        private String apiKey;

        @JsonProperty("username")
        private String username;

        @JsonProperty("password")
        private String password;
    }

    public StreamSubscriptionCreatorJson(StreamSubscriptionOperator.StreamSubscriptionBuilder streamSubscriptionBuilder) {
        this.type = streamSubscriptionBuilder.getType();
        this.name = streamSubscriptionBuilder.getName();
        this.description = streamSubscriptionBuilder.getDescription();
        this.enabled = streamSubscriptionBuilder.getEnabled();
        this.metricSelector = streamSubscriptionBuilder.getMetricSelector();
        this.eventSelector = streamSubscriptionBuilder.getEventSelector();
        this.filters = streamSubscriptionBuilder.getFilters();

        Credential credential = new Credential(
                streamSubscriptionBuilder.getCredentialType(),
                streamSubscriptionBuilder.getAccessToken(),
                streamSubscriptionBuilder.getIntegrationKey(),
                streamSubscriptionBuilder.getApiKey(),
                streamSubscriptionBuilder.getUsername(),
                streamSubscriptionBuilder.getPassword()
        );

        this.sink = new Sink(
                streamSubscriptionBuilder.getSinkType(),
                streamSubscriptionBuilder.getSinkUri(),
                streamSubscriptionBuilder.getSinkHost(),
                streamSubscriptionBuilder.getBatchEnabled(),
                streamSubscriptionBuilder.getBatchSizeMax(),
                streamSubscriptionBuilder.getBatchWaitTimeMax(),
                streamSubscriptionBuilder.getSinkSettings(),
                credential
        );
    }
}
