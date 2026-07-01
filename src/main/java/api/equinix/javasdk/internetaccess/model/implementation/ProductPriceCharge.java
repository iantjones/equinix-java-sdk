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

package api.equinix.javasdk.internetaccess.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A single charge line of an Equinix Internet Access (EIA) v1 price entry's top-level
 * {@code charges} array. Extends {@link PriceCharge} with the per-charge product attribution
 * ({@code product}) carried by the spec's {@code ProductPriceCharge}.
 *
 * <p>The summarized charges ({@code PriceSummary.getCharges()}) remain plain {@link PriceCharge}
 * entries, which carry no product attribution.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductPriceCharge extends PriceCharge {

    @JsonProperty("product")
    private ProductInfo product;
}
