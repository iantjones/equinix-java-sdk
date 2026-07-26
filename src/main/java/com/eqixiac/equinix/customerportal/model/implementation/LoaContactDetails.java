package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.enums.ContactDetailType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A means of contacting a Digital LOA party (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaContactDetails {

    @JsonProperty("type")
    private ContactDetailType type;

    @JsonProperty("value")
    private String value;

    @JsonProperty("notes")
    private String notes;
}
