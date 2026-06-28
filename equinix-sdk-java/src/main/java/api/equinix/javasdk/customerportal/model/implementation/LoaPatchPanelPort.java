package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A patch panel port endpoint of a Digital LOA cross-connect (diLOA v1 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoaPatchPanelPort {

    @JsonProperty("circuitId")
    private String circuitId;

    @JsonProperty("connectorType")
    private String connectorType;

    @JsonProperty("patchPanel")
    private LoaPatchPanel patchPanel;

    @JsonProperty("portA")
    private Integer portA;

    @JsonProperty("portB")
    private Integer portB;
}
