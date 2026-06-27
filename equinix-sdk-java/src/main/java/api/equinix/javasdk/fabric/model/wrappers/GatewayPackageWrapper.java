package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.model.GatewayPackage;
import api.equinix.javasdk.fabric.model.json.GatewayPackageJson;
import lombok.Getter;
import lombok.experimental.Delegate;

public class GatewayPackageWrapper extends ResourceImpl<GatewayPackage> implements GatewayPackage {

    @Delegate
    private GatewayPackageJson jsonObject;
    @Getter
    private final Pageable<GatewayPackage> serviceClient;

    public GatewayPackageWrapper(GatewayPackageJson gatewayPackageJson, Pageable<GatewayPackage> serviceClient) {
        this.jsonObject = gatewayPackageJson;
        this.serviceClient = serviceClient;
    }
}
