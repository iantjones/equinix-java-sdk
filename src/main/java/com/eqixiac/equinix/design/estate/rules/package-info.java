/**
 * The default estate-analysis rules. Each rule is a small, pure {@code EstateRule}: it reads an
 * {@code EstateInventory} and optional {@code UtilizationSnapshot}, prices through the
 * {@code RuleContext}'s {@code EstatePricing}, and returns findings. None calls an API or changes a
 * resource. {@link com.eqixiac.equinix.design.estate.rules.DefaultRules#all()} lists them.
 */
package com.eqixiac.equinix.design.estate.rules;
