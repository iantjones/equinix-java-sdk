package api.equinix.javasdk.customerportal.enums;

import api.equinix.javasdk.core.model.APIParam;

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
