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

/**
 * One entry of the result of submitting a pending storage shipment order
 * ({@code pending-storage-order-response} item in the shipments v1 spec) — the API returns one
 * entry per stored shipment submitted, identifying the created order and the storage, location
 * and account it applies to.
 */
public interface PendingStorageOrderResponse {

    String getOrderNumber();

    String getOrderReferenceId();

    String getId();

    String getSrNumber();

    String getStorageId();

    String getIbx();

    String getCage();

    String getAccountNumber();

    String getTrackingNumber();
}
