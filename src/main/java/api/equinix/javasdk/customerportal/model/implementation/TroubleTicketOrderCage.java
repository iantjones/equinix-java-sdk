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

package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A cage (or suite) where the current user may place trouble ticket orders ({@code cage}), along
 * with its cage types and accounts.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TroubleTicketOrderCage {

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("cageTypes")
    private List<String> cageTypes;

    @JsonProperty("accounts")
    private List<TroubleTicketOrderAccount> accounts;
}
