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

package api.equinix.javasdk.fabric.client;

import api.equinix.javasdk.core.util.ModelUtils;
import api.equinix.javasdk.core.model.RequestBuilderBase;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ianjones
 */
public class RequestBuilder {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ConnectionPricing extends RequestBuilderBase<ConnectionPricing> {

        private List<Integer> customSpeeds;

        public static ConnectionPricing builder() {
            return new ConnectionPricing();
        }

        public ConnectionPricing addCustomSpeed(Integer customSpeed) {
            if(this.customSpeeds == null) {
                this.customSpeeds = new ArrayList<>();
            }
            this.customSpeeds.add(customSpeed);
            return this;
        }

        public ConnectionPricing addCustomSpeeds(List<Integer> customSpeeds) {
            if(this.customSpeeds == null) {
                this.customSpeeds = new ArrayList<>();
            }
            this.customSpeeds.addAll(customSpeeds);
            return this;
        }

        public ConnectionPricing build() {
            this.queryParameters = new HashMap<>();

            if(customSpeeds.size() > 0) {
                this.queryParameters.put("customSpeeds", ModelUtils.stringListFromIntegerList(this.customSpeeds));
            }
            this.wasBuilt = true;
            return this;
        }
    }
}
