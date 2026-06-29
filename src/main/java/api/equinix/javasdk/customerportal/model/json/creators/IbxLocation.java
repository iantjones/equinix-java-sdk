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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * IBX, cage, account and cabinet location for a smart hands order. At least one cage is
 * required, and each cage requires a cage id and account number.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IbxLocation {

    @JsonProperty("ibx")
    private final String ibx;

    @JsonProperty("cages")
    private final List<Cage> cages;

    public IbxLocation(String ibx, List<Cage> cages) {
        this.ibx = ibx;
        this.cages = cages;
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Cage {

        @JsonProperty("cage")
        private final String cage;

        @JsonProperty("accountNumber")
        private final String accountNumber;

        @JsonProperty("cabinets")
        private final List<String> cabinets;

        public Cage(String cage, String accountNumber, List<String> cabinets) {
            this.cage = cage;
            this.accountNumber = accountNumber;
            this.cabinets = cabinets;
        }

        public Cage(String cage, String accountNumber) {
            this(cage, accountNumber, null);
        }
    }
}
