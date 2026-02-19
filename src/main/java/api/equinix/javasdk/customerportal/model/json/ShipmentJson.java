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

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.model.Serializable;
import api.equinix.javasdk.customerportal.enums.CarrierType;
import api.equinix.javasdk.customerportal.enums.ShipmentStatus;
import api.equinix.javasdk.customerportal.enums.ShipmentType;
import api.equinix.javasdk.customerportal.model.Shipment;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class ShipmentJson implements Serializable {

    @Getter static TypeReference<Page<Shipment, ShipmentJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<List<ShipmentJson>> listTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<ShipmentJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("shipmentId")
    private String shipmentId;

    @JsonProperty("type")
    private ShipmentType type;

    @JsonProperty("status")
    private ShipmentStatus status;

    @JsonProperty("carrierType")
    private CarrierType carrierType;

    @JsonProperty("trackingNumber")
    private String trackingNumber;

    @JsonProperty("ibxCode")
    private String ibxCode;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("description")
    private String description;

    @JsonProperty("senderName")
    private String senderName;

    @JsonProperty("senderCompany")
    private String senderCompany;

    @JsonProperty("senderAddress")
    private String senderAddress;

    @JsonProperty("recipientName")
    private String recipientName;

    @JsonProperty("recipientCompany")
    private String recipientCompany;

    @JsonProperty("numberOfBoxes")
    private Integer numberOfBoxes;

    @JsonProperty("estimatedArrivalDate")
    private String estimatedArrivalDate;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("lastUpdatedDate")
    private String lastUpdatedDate;
}
