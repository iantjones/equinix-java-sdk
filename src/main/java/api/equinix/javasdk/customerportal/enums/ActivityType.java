package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

public enum ActivityType implements APIParam {
    CREDIT_MRC,
    CREDIT_NRC,
    ONE_TIME_CHARGE,
    RECURRING_CHARGE,
    PRIOR_RECURRING_CHARGE,
    ADJUSTMENT_CHARGE,
    DEINSTALL
}
