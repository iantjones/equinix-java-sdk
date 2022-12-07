package api.equinix.javasdk.fabric.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.fabric.enums.GatewayPackageCode;
import api.equinix.javasdk.fabric.enums.GatewayPackageType;
import api.equinix.javasdk.fabric.enums.NatType;
import api.equinix.javasdk.fabric.model.GatewayPackage;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.json.GatewayPackageJson;
import lombok.Getter;

public class GatewayPackageWrapper extends ResourceImpl<GatewayPackage> implements GatewayPackage {

    private GatewayPackageJson jsonObject;
    @Getter
    private final Pageable<GatewayPackage> serviceClient;

    public GatewayPackageWrapper(GatewayPackageJson gatewayPackageJson, Pageable<GatewayPackage> serviceClient) {
        this.jsonObject = gatewayPackageJson;
        this.serviceClient = serviceClient;
    }
    
    public GatewayPackageCode getCode() {
        return this.jsonObject.getCode();
    }

    public String getHref() {
        return this.jsonObject.getHref();
    }

    public GatewayPackageType getType() {
        return this.jsonObject.getType();
    }

    public String getDescription() {
        return this.jsonObject.getDescription();
    }

    public Integer getTotalIPv4RoutesMax() {
        return this.jsonObject.getTotalIPv4RoutesMax();
    }

    public Integer getTotalIPv6RoutesMax() {
        return this.jsonObject.getTotalIPv6RoutesMax();
    }

    public Integer getStaticIPv4RoutesMax() {
        return this.jsonObject.getStaticIPv4RoutesMax();
    }

    public Integer getStaticIPv6RoutesMax() {
        return this.jsonObject.getStaticIPv6RoutesMax();
    }

    public Integer getNaclsMax() {
        return this.jsonObject.getNaclsMax();
    }

    public Integer getNaclRulesMax() {
        return this.jsonObject.getNaclRulesMax();
    }

    public Boolean getHaSupported() {
        return this.jsonObject.getHaSupported();
    }

    public Boolean getRouteFilterSupported() {
        return this.jsonObject.getRouteFilterSupported();
    }

    public NatType getNatType() {
        return this.jsonObject.getNatType();
    }

    public ChangeLog getChangeLog() {
        return this.jsonObject.getChangeLog();
    }
}
