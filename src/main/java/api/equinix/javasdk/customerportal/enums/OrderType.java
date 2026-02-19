package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

public enum OrderType implements APIParam {
    NEW_CROSS_CONNECT,
    UPGRADE,
    DISCONNECT,
    ADD_ON
}
