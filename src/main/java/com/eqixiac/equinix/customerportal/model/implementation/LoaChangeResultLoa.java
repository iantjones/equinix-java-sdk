package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.enums.LoaState;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * The Digital LOA state captured in a change record result (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaChangeResultLoa {

    @JsonProperty("state")
    private LoaState state;
}
