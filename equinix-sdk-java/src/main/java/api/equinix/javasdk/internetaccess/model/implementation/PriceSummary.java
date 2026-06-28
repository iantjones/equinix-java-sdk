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

import java.math.BigDecimal;
import java.util.List;

/**
 * Summarized pricing for an Equinix Internet Access (EIA) v1 price entry: the individual
 * charges plus a single roll-up total charge.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceSummary {

    @JsonProperty("charges")
    private List<PriceCharge> charges;

    @JsonProperty("totalCharge")
    private TotalCharge totalCharge;

    /**
     * The roll-up total charge of a {@link PriceSummary}.
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TotalCharge {

        @JsonProperty("price")
        private BigDecimal price;
    }
}
