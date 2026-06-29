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

package api.equinix.javasdk.networkedge.enums;

import api.equinix.javasdk.core.model.APIParam;
import api.equinix.javasdk.networkedge.model.deserializers.LicenseTypeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 *
 * @author ianjones
 */
@JsonDeserialize(using = LicenseTypeDeserializer.class)
public enum LicenseType implements APIParam {
    SUB("Subscription"),
    BYOL("BYOL");

    private final String queryValue;

    LicenseType(String queryValue) {
        this.queryValue = queryValue;
    }

    /**
     * Returns the value expected by the licenseType <em>query</em> parameter of the pricing,
     * order-summary and vendor-terms endpoints ({@code Subscription} / {@code BYOL}). This differs
     * from the enum name / body serialization, where the device-create {@code licenseMode}/
     * {@code licenseType} body fields use {@code SUB} / {@code BYOL}.
     *
     */
    public String getQueryValue() {
        return queryValue;
    }
}
