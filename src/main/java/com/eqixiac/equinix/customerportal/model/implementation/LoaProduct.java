package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.customerportal.enums.LoaProductType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A product (service) that a Digital LOA document is valid for (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaProduct {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private LoaProductType type;

    @JsonProperty("crossConnect")
    private LoaCrossConnect crossConnect;
}
