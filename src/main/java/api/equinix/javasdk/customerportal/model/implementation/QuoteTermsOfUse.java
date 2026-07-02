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

import api.equinix.javasdk.customerportal.enums.TermsOfUseType;
import api.equinix.javasdk.customerportal.enums.TermsOfUsePeriod;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * A quote terms-of-use entry ({@code termsOfUse_Details}): a {@code value} measured in
 * {@code period} ({@code MONTHS} or {@code DAYS}) for a given term {@code type}
 * ({@code INITIAL_TERM}, {@code RENEWAL_TERM} or {@code NON_RENEWAL_NOTICE}).
 *
 * <p>The spec declares {@code value} as an integer, but its own response example returns decimal
 * strings (e.g. {@code "12.0"}), so the field is a {@link BigDecimal} to accept both forms.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteTermsOfUse {

    @JsonProperty("value")
    private BigDecimal value;

    @JsonProperty("period")
    private TermsOfUsePeriod period;

    @JsonProperty("type")
    private TermsOfUseType type;
}
