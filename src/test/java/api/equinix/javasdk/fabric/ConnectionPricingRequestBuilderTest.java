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

package api.equinix.javasdk.fabric;

import api.equinix.javasdk.fabric.client.RequestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit test for the connection-pricing query builder ({@code RequestBuilder.ConnectionPricing}):
 * custom speeds accumulate across both add methods and serialize into the
 * {@code customSpeeds} query parameter on build().
 */
class ConnectionPricingRequestBuilderTest {

    @Test
    @DisplayName("custom speeds accumulate and build() exposes them as the customSpeeds query param")
    void buildsCustomSpeedsQueryParam() {
        RequestBuilder.ConnectionPricing pricing = RequestBuilder.ConnectionPricing.builder()
                .addCustomSpeed(50)
                .addCustomSpeeds(List.of(200, 1000));

        assertSame(pricing, pricing.build(), "build() is fluent");
        assertEquals(List.of("50", "200", "1000"), pricing.getQueryParameters().get("customSpeeds"));
    }
}
