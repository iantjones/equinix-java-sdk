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

package com.eqixiac.equinix.fabric.model.json.creators;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.client.internal.implementation.StreamAlertRuleClientImpl;
import com.eqixiac.equinix.fabric.enums.StreamAlertRuleType;
import com.eqixiac.equinix.fabric.model.StreamAlertRule;
import com.eqixiac.equinix.fabric.model.json.StreamAlertRuleJson;
import com.eqixiac.equinix.fabric.model.wrappers.StreamAlertRuleWrapper;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Map;

/**
 * Fluent builder/updater for Fabric stream alert rules. The metric/resource selectors and detection
 * method are accepted as generic maps because they have several polymorphic shapes in the API.
 *
 * @author ianjones
 */
public class StreamAlertRuleOperator extends ResourceImpl<StreamAlertRule> {

    @Getter
    private final Pageable<StreamAlertRule> serviceClient;

    public StreamAlertRuleOperator(Pageable<StreamAlertRule> serviceClient) {
        this.serviceClient = serviceClient;
    }

    public StreamAlertRuleBuilder create(String streamId) {
        return new StreamAlertRuleBuilder(streamId);
    }

    public StreamAlertRuleUpdater update(String streamId, String uuid) {
        return new StreamAlertRuleUpdater(streamId, uuid);
    }

    @Getter(AccessLevel.PACKAGE)
    public class StreamAlertRuleBuilder {

        private final String streamId;
        private StreamAlertRuleType type = StreamAlertRuleType.METRIC_ALERT;
        private String name;
        private String description;
        private Boolean enabled;
        private Map<String, Object> metricSelector;
        private Map<String, Object> resourceSelector;
        private Map<String, Object> detectionMethod;

        protected StreamAlertRuleBuilder(String streamId) {
            this.streamId = streamId;
        }

        /**
         * Sets the alert rule type (spec {@code AlertRulePostRequest.type}: {@code METRIC_ALERT},
         * which is also the default).
         *
         * @param type the alert rule type
         * @return this builder
         */
        public StreamAlertRuleBuilder type(StreamAlertRuleType type) {
            this.type = type;
            return this;
        }

        public StreamAlertRuleBuilder name(String name) {
            this.name = name;
            return this;
        }

        public StreamAlertRuleBuilder description(String description) {
            this.description = description;
            return this;
        }

        public StreamAlertRuleBuilder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public StreamAlertRuleBuilder metricSelector(Map<String, Object> metricSelector) {
            this.metricSelector = metricSelector;
            return this;
        }

        public StreamAlertRuleBuilder resourceSelector(Map<String, Object> resourceSelector) {
            this.resourceSelector = resourceSelector;
            return this;
        }

        public StreamAlertRuleBuilder detectionMethod(Map<String, Object> detectionMethod) {
            this.detectionMethod = detectionMethod;
            return this;
        }

        public StreamAlertRule create() {
            StreamAlertRuleCreatorJson creatorJson = new StreamAlertRuleCreatorJson(this);
            StreamAlertRuleJson json = ((StreamAlertRuleClientImpl) StreamAlertRuleOperator.this.getServiceClient()).create(streamId, creatorJson);
            return new StreamAlertRuleWrapper(json, StreamAlertRuleOperator.this.getServiceClient());
        }
    }

    @Getter(AccessLevel.PACKAGE)
    public class StreamAlertRuleUpdater {

        private final String streamId;
        private final String uuid;
        private StreamAlertRuleType type = StreamAlertRuleType.METRIC_ALERT;
        private String name;
        private String description;
        private Boolean enabled;
        private Map<String, Object> metricSelector;
        private Map<String, Object> resourceSelector;
        private Map<String, Object> detectionMethod;

        protected StreamAlertRuleUpdater(String streamId, String uuid) {
            this.streamId = streamId;
            this.uuid = uuid;
        }

        /**
         * Sets the alert rule type (spec {@code AlertRulePutRequest.type}: {@code METRIC_ALERT},
         * which is also the default).
         *
         * @param type the alert rule type
         * @return this updater
         */
        public StreamAlertRuleUpdater type(StreamAlertRuleType type) {
            this.type = type;
            return this;
        }

        public StreamAlertRuleUpdater name(String name) {
            this.name = name;
            return this;
        }

        public StreamAlertRuleUpdater description(String description) {
            this.description = description;
            return this;
        }

        public StreamAlertRuleUpdater enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public StreamAlertRuleUpdater metricSelector(Map<String, Object> metricSelector) {
            this.metricSelector = metricSelector;
            return this;
        }

        public StreamAlertRuleUpdater resourceSelector(Map<String, Object> resourceSelector) {
            this.resourceSelector = resourceSelector;
            return this;
        }

        public StreamAlertRuleUpdater detectionMethod(Map<String, Object> detectionMethod) {
            this.detectionMethod = detectionMethod;
            return this;
        }

        public StreamAlertRule save() {
            StreamAlertRulePutJson putJson = new StreamAlertRulePutJson(this);
            StreamAlertRuleJson json = ((StreamAlertRuleClientImpl) StreamAlertRuleOperator.this.getServiceClient()).update(streamId, uuid, putJson);
            return new StreamAlertRuleWrapper(json, StreamAlertRuleOperator.this.getServiceClient());
        }
    }
}
