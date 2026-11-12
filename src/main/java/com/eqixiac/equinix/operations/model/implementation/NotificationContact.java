package com.eqixiac.equinix.operations.model.implementation;

import com.eqixiac.equinix.operations.enums.NotificationContactType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A recipient of a unified notification event (Unified Notifications v2 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationContact {

    @JsonProperty("type")
    private NotificationContactType type;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("details")
    private List<NotificationContactDetail> details;
}
