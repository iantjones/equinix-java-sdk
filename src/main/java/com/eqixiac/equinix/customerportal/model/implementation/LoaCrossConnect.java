package com.eqixiac.equinix.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A cross-connect covered by a Digital LOA product (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaCrossConnect {

    @JsonProperty("connectionService")
    private String connectionService;

    @JsonProperty("mediaType")
    private String mediaType;

    @JsonProperty("protocolType")
    private String protocolType;

    @JsonProperty("aSide")
    private LoaPatchPanelPort aSide;

    @JsonProperty("zSide")
    private LoaPatchPanelPort zSide;
}
