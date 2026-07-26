package com.eqixiac.equinix.fabric.model.wrappers;

import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.core.model.ResourceImpl;
import com.eqixiac.equinix.fabric.model.GatewayPackage;
import com.eqixiac.equinix.fabric.model.json.GatewayPackageJson;
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
