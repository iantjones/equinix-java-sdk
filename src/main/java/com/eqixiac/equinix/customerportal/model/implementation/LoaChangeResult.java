package com.eqixiac.equinix.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * The result of a Digital LOA change record (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaChangeResult {

    @JsonProperty("loa")
    private LoaChangeResultLoa loa;
}
