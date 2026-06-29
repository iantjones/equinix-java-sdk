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

package api.equinix.javasdk.ibxsmartview.model.json.creators;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.ibxsmartview.client.internal.implementation.PowerEventClientImpl;
import api.equinix.javasdk.ibxsmartview.model.PowerEvent;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertCondition;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertConfigurationAsset;
import api.equinix.javasdk.ibxsmartview.model.implementation.PowerAlertRecipient;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for creating and updating IBX SmartView power alert configurations. Obtain a
 * create builder from {@code PowerEvents.defineAlertConfiguration()} and an update builder from
 * {@code PowerEvents.updateAlertConfiguration(uid)}.
 */
public class PowerAlertConfigurationOperator extends ResourceImpl<PowerEvent> {

    @Getter
    private final Pageable<PowerEvent> serviceClient;

    public PowerAlertConfigurationOperator(Pageable<PowerEvent> serviceClient) {
        this.serviceClient = serviceClient;
    }

    /**
     * Returns a new builder for creating a power alert configuration.
     *
     * @return a create builder
     */
    public PowerAlertConfigurationBuilder create() {
        return new PowerAlertConfigurationBuilder();
    }

    /**
     * Returns a new builder for updating the power alert configuration identified by {@code alertConfigurationUid}.
     *
     * @param alertConfigurationUid the unique identifier of the configuration to update
     * @return an update builder
     */
    public PowerAlertConfigurationUpdateBuilder update(String alertConfigurationUid) {
        return new PowerAlertConfigurationUpdateBuilder(alertConfigurationUid);
    }

    /**
     * Fluent builder for creating a power alert configuration.
     */
    @Getter
    public class PowerAlertConfigurationBuilder {

        private String accountNo;
        private String ibx;
        private String section;
        private PowerAlertCondition condition;
        private final List<PowerAlertRecipient> recipients;
        private final Map<String, List<PowerAlertConfigurationAsset>> assets;

        protected PowerAlertConfigurationBuilder() {
            this.recipients = new ArrayList<>();
            this.assets = new LinkedHashMap<>();
        }

        public PowerAlertConfigurationBuilder withAccountNo(String accountNo) {
            this.accountNo = accountNo;
            return this;
        }

        public PowerAlertConfigurationBuilder withIbx(String ibx) {
            this.ibx = ibx;
            return this;
        }

        public PowerAlertConfigurationBuilder withSection(String section) {
            this.section = section;
            return this;
        }

        public PowerAlertConfigurationBuilder withCondition(PowerAlertCondition condition) {
            this.condition = condition;
            return this;
        }

        public PowerAlertConfigurationBuilder addRecipient(PowerAlertRecipient recipient) {
            this.recipients.add(recipient);
            return this;
        }

        public PowerAlertConfigurationBuilder addAssets(String levelType, List<PowerAlertConfigurationAsset> levelAssets) {
            this.assets.computeIfAbsent(levelType, key -> new ArrayList<>()).addAll(levelAssets);
            return this;
        }

        /**
         * Creates the power alert configuration and returns the UID of the newly created configuration.
         *
         * @return the {@code alertConfigurationUid} of the created configuration
         */
        public String create() {
            PowerAlertConfigurationCreatorJson creatorJson = new PowerAlertConfigurationCreatorJson();
            creatorJson.setAccountNo(this.accountNo);
            creatorJson.setIbx(this.ibx);
            creatorJson.setSection(this.section);
            creatorJson.setCondition(this.condition);
            creatorJson.setRecipients(this.recipients.isEmpty() ? null : this.recipients);
            creatorJson.setAssets(this.assets.isEmpty() ? null : this.assets);
            return ((PowerEventClientImpl) PowerAlertConfigurationOperator.this.getServiceClient())
                    .createPowerAlertConfiguration(creatorJson).getAlertConfigurationUid();
        }
    }

    /**
     * Fluent builder for updating an existing power alert configuration.
     */
    @Getter
    public class PowerAlertConfigurationUpdateBuilder {

        private final String alertConfigurationUid;
        private String state;
        private PowerAlertCondition condition;
        private List<PowerAlertRecipient> recipients;
        private Map<String, List<PowerAlertConfigurationAsset>> assets;

        protected PowerAlertConfigurationUpdateBuilder(String alertConfigurationUid) {
            this.alertConfigurationUid = alertConfigurationUid;
        }

        public PowerAlertConfigurationUpdateBuilder withState(String state) {
            this.state = state;
            return this;
        }

        public PowerAlertConfigurationUpdateBuilder withCondition(PowerAlertCondition condition) {
            this.condition = condition;
            return this;
        }

        public PowerAlertConfigurationUpdateBuilder withRecipients(List<PowerAlertRecipient> recipients) {
            this.recipients = recipients;
            return this;
        }

        public PowerAlertConfigurationUpdateBuilder withAssets(Map<String, List<PowerAlertConfigurationAsset>> assets) {
            this.assets = assets;
            return this;
        }

        /**
         * Submits the update to the power alert configuration.
         */
        public void update() {
            PowerAlertConfigurationUpdateJson updateJson = new PowerAlertConfigurationUpdateJson();
            updateJson.setAlertConfigurationUid(this.alertConfigurationUid);
            updateJson.setState(this.state);
            updateJson.setCondition(this.condition);
            updateJson.setRecipients(this.recipients);
            updateJson.setAssets(this.assets);
            ((PowerEventClientImpl) PowerAlertConfigurationOperator.this.getServiceClient())
                    .updatePowerAlertConfiguration(updateJson);
        }
    }
}
