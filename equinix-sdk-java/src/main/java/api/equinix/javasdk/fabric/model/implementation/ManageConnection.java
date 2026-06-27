package api.equinix.javasdk.fabric.model.implementation;

import api.equinix.javasdk.fabric.enums.ConnectionOperationType;
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
