package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.enums.ContactAvailability;
import com.eqixiac.equinix.customerportal.enums.LoaContactType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A contact person for a Digital LOA party (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaContact {

    @JsonProperty("type")
    private LoaContactType type;

    @JsonProperty("registeredUser")
    private String registeredUser;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("availability")
    private ContactAvailability availability;

    @JsonProperty("details")
    private List<LoaContactDetails> details;
}
