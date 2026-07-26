package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.enums.TermsOfUsePeriod;
import com.eqixiac.equinix.customerportal.enums.TermsOfUseType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A terms-of-use entry on an invoice line item (Billing v2 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TermsOfUse {

    @JsonProperty("value")
    private Integer value;

    @JsonProperty("type")
    private TermsOfUseType type;

    @JsonProperty("period")
    private TermsOfUsePeriod period;
}
