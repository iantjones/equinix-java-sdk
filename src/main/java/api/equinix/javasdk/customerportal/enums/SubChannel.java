package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Sub-channel used to place an order on an invoice line item (Billing v2 API).
 */
public enum SubChannel implements APIParam {
    QUOTE,
    IX,
    ECX,
    ECP,
    CSC,
    NRC_CHARGE_UPLOAD
}
