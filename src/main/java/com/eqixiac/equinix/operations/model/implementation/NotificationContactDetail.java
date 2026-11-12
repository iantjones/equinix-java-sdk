package com.eqixiac.equinix.operations.model.implementation;

import com.eqixiac.equinix.operations.model.ContactDetailRef;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A means of contacting a unified notification recipient
 * (Unified Notifications v2 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationContactDetail implements ContactDetailRef {

    @JsonProperty("type")
    private String type;

    @JsonProperty("value")
    private String value;
}
