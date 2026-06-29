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

import api.equinix.javasdk.internetaccess.enums.ServiceOrderStatus;
import api.equinix.javasdk.internetaccess.enums.ServiceOrderType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Order backing an Equinix Internet Access (EIA) v2 service, as returned in the service read model.
 *
 * <p>Carries the order's identity and lifecycle ({@code href}, {@code uuid}, {@code type},
 * {@code status}, {@code number}, {@code changeLog}) together with the submitted order detail
 * ({@code contacts}, {@code purchaseOrder}, {@code referenceNumber}, {@code signature}).</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceOrderReadModel {

    @JsonProperty("href")
    private String href;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private ServiceOrderType type;

    @JsonProperty("status")
    private ServiceOrderStatus status;

    @JsonProperty("number")
    private String number;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    @JsonProperty("contacts")
    private List<ContactItem> contacts;

    @JsonProperty("purchaseOrder")
    private ServicePurchaseOrder purchaseOrder;

    @JsonProperty("referenceNumber")
    private String referenceNumber;

    @JsonProperty("signature")
    private OrderSignature signature;
}
