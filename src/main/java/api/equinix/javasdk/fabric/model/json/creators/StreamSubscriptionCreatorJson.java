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

import api.equinix.javasdk.fabric.enums.StreamSubscriptionSinkCredentialType;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionSinkType;
import api.equinix.javasdk.fabric.enums.StreamSubscriptionType;
import api.equinix.javasdk.fabric.model.implementation.StreamSinkSetting;
import api.equinix.javasdk.fabric.model.implementation.StreamSubscriptionFilter;
import api.equinix.javasdk.fabric.model.implementation.StreamSubscriptionSelector;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
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

        /**
         * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}:
         * {@code uri}/{@code host} and {@code batchSizeMax}/{@code batchWaitTimeMax} are
         * same-typed adjacent parameters, so the argument order is pinned here in code
         * rather than by field declaration order.
         *
         * @param type             the sink type
         * @param uri              the sink endpoint URI
         * @param host             the sink host
         * @param batchEnabled     whether batching is enabled
         * @param batchSizeMax     the maximum batch size
         * @param batchWaitTimeMax the maximum batch wait time
         * @param settings         the sink settings
         * @param credential       the sink credential
         */
        Sink(StreamSubscriptionSinkType type, String uri, String host, Boolean batchEnabled,
             Integer batchSizeMax, Integer batchWaitTimeMax, StreamSinkSetting settings,
             Credential credential) {
            this.type = type;
            this.uri = uri;
            this.host = host;
            this.batchEnabled = batchEnabled;
            this.batchSizeMax = batchSizeMax;
            this.batchWaitTimeMax = batchWaitTimeMax;
            this.settings = settings;
            this.credential = credential;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Credential {

        @JsonProperty("type")
        private StreamSubscriptionSinkCredentialType type;

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

        /**
         * Explicit constructor replacing the Lombok-generated {@code @AllArgsConstructor}:
         * five consecutive {@code String} credential parameters mean a silent transposition
         * would post credentials into the wrong JSON fields, so the argument order is pinned
         * here in code rather than by field declaration order.
         *
         * @param type           the credential type
         * @param accessToken    the access token
         * @param integrationKey the integration key
         * @param apiKey         the API key
         * @param username       the username
         * @param password       the password
         */
        Credential(StreamSubscriptionSinkCredentialType type, String accessToken,
                   String integrationKey, String apiKey, String username, String password) {
            this.type = type;
            this.accessToken = accessToken;
            this.integrationKey = integrationKey;
            this.apiKey = apiKey;
            this.username = username;
            this.password = password;
        }
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
