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

package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.fabric.enums.PortPurchaseOrderType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Purchase order information associated with a port order (the Fabric v4
 * {@code PortOrderPurchaseOrder} schema). {@code type} and the deprecated
 * {@code selectionType} take the values {@code EXEMPTION}, {@code EXISTING},
 * {@code NEW} or {@code BLANKET}.
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOrderPurchaseOrder {

    @JsonProperty("number")
    private String number;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("attachmentId")
    private String attachmentId;

    /**
     * @deprecated the API marks {@code selectionType} deprecated; use {@code getType()} instead.
     */
    @Deprecated
    @JsonProperty("selectionType")
    private PortPurchaseOrderType selectionType;

    @JsonProperty("type")
    private PortPurchaseOrderType type;
}
