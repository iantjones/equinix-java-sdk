package com.eqixiac.equinix.customerportal.enums;

import com.eqixiac.equinix.core.model.APIParam;

/**
 * Relationship of a HATEOAS link to its Digital LOA document (diLOA v1 API).
 */
public enum LoaLinkRel implements APIParam {
    self,
    info,
    update,
    delete,
    statistics,
    events
}
