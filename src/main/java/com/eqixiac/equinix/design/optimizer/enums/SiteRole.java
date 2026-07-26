package com.eqixiac.equinix.design.optimizer.enums;

import lombok.Getter;

/**
 * The business role of a user-defined site. Influences how the optimizer
 * weights proximity and latency to that site: each role carries an
 * {@code importanceMultiplier} applied to the site's base weight (explicit weight,
 * else normalized headcount, else a role-based inferred weight).
 *
 * @see com.eqixiac.equinix.design.optimizer.model.UserSite
 */
@Getter
public enum SiteRole {

    /** Corporate headquarters — the heaviest role (multiplier 1.5). */
    HEADQUARTERS("Corporate headquarters", 1.5),

    /** Major employee concentration (multiplier 1.2). */
    EMPLOYEE_HUB("Major employee concentration", 1.2),

    /** Key customer or revenue market (multiplier 1.3). */
    PRIMARY_MARKET("Key customer or revenue market", 1.3),

    /** Manufacturing or production facility (multiplier 1.0). */
    MANUFACTURING("Manufacturing or production facility", 1.0),

    /** Smaller branch or satellite office — the default role, and the lightest (multiplier 0.8). */
    BRANCH_OFFICE("Smaller branch or satellite office", 0.8),

    /** Region with high customer density (multiplier 1.3). */
    CUSTOMER_CONCENTRATION("Region with high customer density", 1.3),

    /** Existing data center or colocation facility (multiplier 1.1). */
    DATA_CENTER("Existing data center or colo facility", 1.1),

    /** R&amp;D or research facility (multiplier 1.1). */
    RESEARCH_LAB("R&D or research facility", 1.1);

    private final String description;
    private final double importanceMultiplier;

    SiteRole(String description, double importanceMultiplier) {
        this.description = description;
        this.importanceMultiplier = importanceMultiplier;
    }
}
