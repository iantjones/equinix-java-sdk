package com.eqixiac.equinix.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A patch panel referenced by a Digital LOA cross-connect port (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaPatchPanel {

    @JsonProperty("id")
    private String id;

    @JsonProperty("cabinetSpaceId")
    private String cabinetSpaceId;

    @JsonProperty("cageSpaceId")
    private String cageSpaceId;

    @JsonProperty("location")
    private LoaLocation location;
}
