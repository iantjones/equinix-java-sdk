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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.customerportal.enums.CarrierType;
import api.equinix.javasdk.customerportal.enums.ShipmentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ShipmentCreatorJson {

    @JsonProperty("type")
    private ShipmentType type;

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

    public ShipmentCreatorJson(ShipmentOperator.ShipmentBuilder builder) {
        this.type = builder.getType();
        this.carrierType = builder.getCarrierType();
        this.trackingNumber = builder.getTrackingNumber();
        this.ibxCode = builder.getIbxCode();
        this.accountNumber = builder.getAccountNumber();
        this.description = builder.getDescription();
        this.senderName = builder.getSenderName();
        this.senderCompany = builder.getSenderCompany();
        this.senderAddress = builder.getSenderAddress();
        this.recipientName = builder.getRecipientName();
        this.recipientCompany = builder.getRecipientCompany();
        this.numberOfBoxes = builder.getNumberOfBoxes();
        this.estimatedArrivalDate = builder.getEstimatedArrivalDate();
    }
}
