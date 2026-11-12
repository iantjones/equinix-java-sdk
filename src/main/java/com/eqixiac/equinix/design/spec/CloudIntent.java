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

package com.eqixiac.equinix.design.spec;

import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * A cloud or service provider the design must reach over private interconnect, with the traffic that
 * drives its cost.
 *
 * @param id                  unique name within the spec; {@link WorkloadIntent#clouds()} refers to it
 * @param provider            the cloud provider; {@code null} when the target is named by service profile only
 * @param region              the provider's region (for example {@code us-east-1}); may be {@code null}
 * @param serviceProfileUuid  a specific Fabric service profile, or {@code null} to resolve by provider
 * @param serviceProfileName  a Fabric service-profile name to match, or {@code null}
 * @param required            whether every recommended design must reach it; {@code null} becomes {@code true}
 * @param monthlyEgressGb     data leaving this cloud per month, in GB; may be {@code null} (not costed)
 * @param monthlyIngressGb    data sent to this cloud per month, in GB; may be {@code null}
 */
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CloudIntent(String id, CloudProviderType provider, String region, String serviceProfileUuid,
                          String serviceProfileName, Boolean required, Double monthlyEgressGb,
                          Double monthlyIngressGb) {

    /** Defaults {@code required} to {@code true}: a cloud named in a spec must be reached unless it says otherwise. */
    public CloudIntent {
        required = required == null ? Boolean.TRUE : required;
    }
}
