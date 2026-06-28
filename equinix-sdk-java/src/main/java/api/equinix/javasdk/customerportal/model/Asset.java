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

package api.equinix.javasdk.customerportal.model;

import api.equinix.javasdk.customerportal.enums.AssetStatus;
import api.equinix.javasdk.customerportal.model.implementation.AssetAdditionalDetails;
import api.equinix.javasdk.customerportal.model.implementation.AssetProperty;

import java.util.List;

/**
 * An installed-base asset retrieved from the Equinix Customer Portal Assets v1 API: a product or
 * piece of equipment belonging to the organization that is installed in an Equinix IBX.
 */
public interface Asset {

    /**
     * Returns the asset number (the asset identifier).
     *
     * @return the asset number
     */
    String getAssetNumber();

    /**
     * Returns the serial number of the asset.
     *
     * @return the serial number, or {@code null} if not provided
     */
    String getSerialNumber();

    /**
     * Returns the order number associated with the asset/service.
     *
     * @return the order number, or {@code null} if not provided
     */
    String getOrderNumber();

    /**
     * Returns the product type/name.
     *
     * @return the product name, or {@code null} if not provided
     */
    String getProductName();

    /**
     * Returns the IBX of the location.
     *
     * @return the IBX, or {@code null} if not provided
     */
    String getIbx();

    /**
     * Returns the cage of the location.
     *
     * @return the cage, or {@code null} if not provided
     */
    String getCage();

    /**
     * Returns the product description.
     *
     * @return the product description, or {@code null} if not provided
     */
    String getProductDescription();

    /**
     * Returns the account number the product/service belongs to.
     *
     * @return the account number, or {@code null} if not provided
     */
    String getAccountNumber();

    /**
     * Returns the account name of the product/service.
     *
     * @return the account name, or {@code null} if not provided
     */
    String getAccountName();

    /**
     * Returns the date and time the product or service was purchased/installed.
     *
     * @return the installation date, or {@code null} if not provided
     */
    String getInstallationDate();

    /**
     * Returns the customer reference number.
     *
     * @return the customer reference number, or {@code null} if not provided
     */
    String getCustomerReferenceNumber();

    /**
     * Returns the status of the asset.
     *
     * @return the asset status, or {@code null} if not provided
     */
    AssetStatus getStatus();

    /**
     * Returns the key/value product details for the asset.
     *
     * @return the product details, or {@code null} if not provided
     */
    List<AssetProperty> getProductDetails();

    /**
     * Returns additional details specific to the asset.
     *
     * @return the additional details, or {@code null} if not provided
     */
    AssetAdditionalDetails getAdditionalDetails();
}
