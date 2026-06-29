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

import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.model.APIParam;
import api.equinix.javasdk.core.util.ModelUtils;
import api.equinix.javasdk.core.model.RequestBuilderBase;
import api.equinix.javasdk.fabric.enums.Direction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>RequestBuilder class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
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

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TopPortStatistics extends RequestBuilderBase<TopPortStatistics> {

        private List<APIParam> metros;
        private Direction direction;
        private Integer top;

        public static TopPortStatistics builder() {
            return new TopPortStatistics();
        }

        public TopPortStatistics addMetro(MetroCode metro) {
            if(this.metros == null) {
                this.metros = new ArrayList<>();
            }
            this.metros.add(metro);
            return this;
        }

        public TopPortStatistics addMetros(List<MetroCode> metros) {
            if(this.metros == null) {
                this.metros = new ArrayList<>();
            }
            this.metros.addAll(metros);
            return this;
        }

        public TopPortStatistics direction(Direction direction) {
            this.direction = direction;
            return this;
        }

        public TopPortStatistics withTop(Integer top) {
            this.top = top;
            return this;
        }


        public TopPortStatistics build() {
            this.queryParameters = new HashMap<>();

            if(this.metros.size() > 0) {
                this.queryParameters.put("metros", ModelUtils.stringListFromEnumList(this.metros));
            }

            if(this.direction != null) {
                this.queryParameters.put("direction", ModelUtils.singleValueList(this.direction.toString().toLowerCase()));
            }

            if(this.top != null) {
                this.queryParameters.put("top", ModelUtils.singleValueList(this.top));
            }

            this.wasBuilt = true;
            return this;
        }
    }
}
