package com.eqixiac.equinix.operations.model.implementation;

import com.eqixiac.equinix.operations.enums.ContactType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactDetail {

    @JsonProperty("type")
    private ContactType type;

    @JsonProperty("value")
    private String value;
}
