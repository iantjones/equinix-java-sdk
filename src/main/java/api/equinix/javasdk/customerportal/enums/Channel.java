package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Channel used to place an order on an invoice line item (Billing v2 API).
 */
public enum Channel implements APIParam {
    API,
    PORTAL,
    MOBILE,
    OFFLINE,
    OTHER
}
