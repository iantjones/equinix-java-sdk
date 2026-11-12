package com.eqixiac.equinix.design.optimizer.enums;

import com.eqixiac.equinix.core.enums.Region;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Regulatory zones a design must stay within, each allowing a set of Equinix regions.
 *
 * <p>Enforced by the {@code compliance_zones} constraint: every metro of a design must be in a region
 * that at least one of the requested zones allows. Region granularity is coarse (all of EMEA for
 * {@code EU_GDPR}); for country-level data sovereignty use {@code DesignConstraints.geoZones}, which
 * matches the live Fabric geo scopes of each metro.</p>
 */
@Getter
public enum ComplianceZone {

    /** EU General Data Protection Regulation: satisfied by metros in {@code Region.EMEA}. */
    EU_GDPR("EU General Data Protection Regulation", Collections.singletonList(Region.EMEA)),

    /** US FedRAMP data residency: satisfied by metros in {@code Region.AMER}. */
    US_FEDRAMP("US Federal Risk and Authorization Management", Collections.singletonList(Region.AMER)),

    /** US HIPAA data residency: satisfied by metros in {@code Region.AMER}. */
    US_HIPAA("US Health Insurance Portability and Accountability Act", Collections.singletonList(Region.AMER)),

    /** Asia-Pacific data residency: satisfied by metros in {@code Region.APAC}. */
    APAC_GENERAL("Asia-Pacific data residency", Collections.singletonList(Region.APAC)),

    /** UK data protection post-Brexit: satisfied by metros in {@code Region.EMEA}. */
    UK_POST_BREXIT("UK data protection post-Brexit", Collections.singletonList(Region.EMEA)),

    /** China mainland data localization: satisfied by metros in {@code Region.APAC}. */
    CHINA_MAINLAND("China mainland data localization", Collections.singletonList(Region.APAC)),

    /**
     * Placeholder zone that allows every region - it constrains nothing and always scores as
     * covered. Use it to reserve a zone slot for requirements the built-in constants do not model.
     */
    CUSTOM("Custom compliance zone", Arrays.asList(Region.values()));

    private final String description;
    private final List<Region> allowedRegions;

    ComplianceZone(String description, List<Region> allowedRegions) {
        this.description = description;
        this.allowedRegions = allowedRegions;
    }
}
