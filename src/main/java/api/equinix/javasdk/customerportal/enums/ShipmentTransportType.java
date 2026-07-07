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

package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * How a v1 shipment moves in or out of the IBX: via a carrier service or carried by the
 * customer. Maps to {@code inboundType} ({@code inboundServiceDetail.shipmentDetails}) and
 * {@code outboundType} ({@code outboundServiceDetail.shipmentDetails}) in the shipments v1 spec;
 * both declare the same {@code CARRIER}/{@code CUSTOMER_CARRY} value set.
 */
public enum ShipmentTransportType implements APIParam {
    CARRIER,
    CUSTOMER_CARRY
}
