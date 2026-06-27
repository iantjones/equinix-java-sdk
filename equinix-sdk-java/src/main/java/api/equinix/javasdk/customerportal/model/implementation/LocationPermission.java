package api.equinix.javasdk.customerportal.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

@Getter
public class LocationPermission {

    @Getter static TypeReference<LocationPermission> listTypeRef = new TypeReference<>() {};

    @JsonProperty("ibx")
    private String ibx;

    @JsonProperty("accessRestricted")
    private Boolean accessRestricted;

    @JsonProperty("specialPrivilege")
    private Boolean specialPrivilege;
}
