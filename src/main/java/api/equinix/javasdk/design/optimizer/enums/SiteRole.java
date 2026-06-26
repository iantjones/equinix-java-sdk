package api.equinix.javasdk.design.optimizer.enums;

import lombok.Getter;

/**
 * The business role of a user-defined site. Influences how the optimizer
 * weights proximity and latency to that site.
 *
 * @see api.equinix.javasdk.design.optimizer.model.UserSite
 */
@Getter
public enum SiteRole {

    HEADQUARTERS("Corporate headquarters", 1.5),
    EMPLOYEE_HUB("Major employee concentration", 1.2),
    PRIMARY_MARKET("Key customer or revenue market", 1.3),
    MANUFACTURING("Manufacturing or production facility", 1.0),
    BRANCH_OFFICE("Smaller branch or satellite office", 0.8),
    CUSTOMER_CONCENTRATION("Region with high customer density", 1.3),
    DATA_CENTER("Existing data center or colo facility", 1.1),
    RESEARCH_LAB("R&D or research facility", 1.1);

    private final String description;
    private final double importanceMultiplier;

    SiteRole(String description, double importanceMultiplier) {
        this.description = description;
        this.importanceMultiplier = importanceMultiplier;
    }
}
