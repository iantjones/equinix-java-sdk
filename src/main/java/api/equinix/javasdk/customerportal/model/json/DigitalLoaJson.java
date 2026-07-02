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

import api.equinix.javasdk.customerportal.enums.LoaState;
import api.equinix.javasdk.customerportal.model.DigitalLoa;
import api.equinix.javasdk.customerportal.model.implementation.LoaChangeLog;
import api.equinix.javasdk.customerportal.model.implementation.LoaChangeReference;
import api.equinix.javasdk.customerportal.model.implementation.LoaLink;
import api.equinix.javasdk.customerportal.model.implementation.LoaParty;
import api.equinix.javasdk.customerportal.model.implementation.LoaProduct;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DigitalLoaJson implements DigitalLoa {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("token")
    private String token;

    @JsonProperty("state")
    private LoaState state;

    @JsonProperty("products")
    private List<LoaProduct> products;

    @JsonProperty("requestor")
    private LoaParty requestor;

    @JsonProperty("provider")
    private LoaParty provider;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("expiryDateTime")
    private String expiryDateTime;

    @JsonProperty("changeLog")
    private LoaChangeLog changeLog;

    @JsonProperty("change")
    private LoaChangeReference change;

    @JsonProperty("draft")
    private Boolean draft;

    @JsonProperty("links")
    private List<LoaLink> links;

    @JsonProperty("href")
    private String href;
}
