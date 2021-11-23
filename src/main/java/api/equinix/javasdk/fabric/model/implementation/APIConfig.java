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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>APIConfig class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class APIConfig {

    @JsonProperty("apiAvailable")
    private Boolean apiAvailable;

    @JsonProperty("bandwidthFromApi")
    private Boolean bandwidthFromApi;

    @JsonProperty("equinixManagedVlan")
    private Boolean equinixManagedVlan;

    @JsonProperty("equinixManagedPort")
    private Boolean equinixManagedPort;

    @JsonProperty("allowOverSubscription")
    private Boolean allowOverSubscription;

    @JsonProperty("overSubscriptionLimit")
    private Integer overSubscriptionLimit;

    @JsonProperty("integrationId")
    private String integrationId;
}
