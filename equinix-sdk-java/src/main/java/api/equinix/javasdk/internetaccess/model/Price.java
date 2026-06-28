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

package api.equinix.javasdk.internetaccess.model;

import api.equinix.javasdk.internetaccess.enums.PriceCategory;
import api.equinix.javasdk.internetaccess.enums.ProductType;
import api.equinix.javasdk.internetaccess.model.implementation.PriceCharge;
import api.equinix.javasdk.internetaccess.model.implementation.PriceSummary;

import java.util.List;

/**
 * An Equinix Internet Access (EIA) v1 price entry, as returned by the price search
 * {@code POST /internetAccess/v1/prices/search}.
 *
 * <p>Currency depends on the billing account. If a product (or part of it) is provided for
 * free, it is listed with a price of {@code 0}. This is a read-only response view.</p>
 */
public interface Price {

    /**
     * @return an absolute URL that returns the specified pricing data
     */
    String getHref();

    /**
     * @return the product type ({@code INTERNET_ACCESS_PRODUCT} or {@code IP_BLOCK_PRODUCT})
     */
    ProductType getType();

    /**
     * @return the Equinix-assigned product code
     */
    String getCode();

    /**
     * @return the full product name
     */
    String getName();

    /**
     * @return the product description
     */
    String getDescription();

    /**
     * @return the ISO 4217 currency code of the price
     */
    String getCurrency();

    /**
     * @return the price category ({@code COUNTRY} or {@code CUSTOMER})
     */
    PriceCategory getCategory();

    /**
     * @return the collection of individual product price charges
     */
    List<PriceCharge> getCharges();

    /**
     * @return the summarized pricing (charges plus roll-up total charge)
     */
    PriceSummary getSummary();
}
