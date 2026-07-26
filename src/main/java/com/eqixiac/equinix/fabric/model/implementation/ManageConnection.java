package com.eqixiac.equinix.fabric.model.implementation;

import com.eqixiac.equinix.fabric.enums.ConnectionOperationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ManageConnection {

    @JsonProperty("type")
    ConnectionOperationType type;

    @JsonProperty("description")
    String description;

    @JsonProperty("data")
    Object data;
}
