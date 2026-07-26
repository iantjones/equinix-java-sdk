package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.model.ContactDetailRef;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A means of contacting a unified notification recipient
 * (Customer Portal Unified Notifications v2 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationContactDetail implements ContactDetailRef {

    @JsonProperty("type")
    private String type;

    @JsonProperty("value")
    private String value;
}
