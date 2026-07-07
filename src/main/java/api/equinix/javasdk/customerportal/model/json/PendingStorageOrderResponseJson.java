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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.customerportal.model.PendingStorageOrderResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * JSON model for one entry of the pending storage shipment order result
 * ({@code pending-storage-order-response} item in the shipments v1 spec). Note the mixed-case
 * wire names ({@code OrderReferenceId}, {@code Id}, {@code SRNumber}, {@code storageID},
 * {@code Ibx}) declared by the spec.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PendingStorageOrderResponseJson implements PendingStorageOrderResponse {

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("OrderReferenceId")
    private String orderReferenceId;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("SRNumber")
    private String srNumber;

    @JsonProperty("storageID")
    private String storageId;

    @JsonProperty("Ibx")
    private String ibx;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("trackingNumber")
    private String trackingNumber;
}
