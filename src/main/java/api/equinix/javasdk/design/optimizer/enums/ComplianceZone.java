package api.equinix.javasdk.design.optimizer.enums;

import api.equinix.javasdk.core.enums.Region;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Data sovereignty and compliance zones that restrict which regions
 * are eligible for workload placement.
 *
 * @see api.equinix.javasdk.design.optimizer.model.OptimizationConstraints
 */
@Getter
public enum ComplianceZone {

    EU_GDPR("EU General Data Protection Regulation", Collections.singletonList(Region.EMEA)),
    US_FEDRAMP("US Federal Risk and Authorization Management", Collections.singletonList(Region.AMER)),
    US_HIPAA("US Health Insurance Portability and Accountability Act", Collections.singletonList(Region.AMER)),
    APAC_GENERAL("Asia-Pacific data residency", Collections.singletonList(Region.APAC)),
    UK_POST_BREXIT("UK data protection post-Brexit", Collections.singletonList(Region.EMEA)),
    CHINA_MAINLAND("China mainland data localization", Collections.singletonList(Region.APAC)),
    CUSTOM("Custom compliance zone", Arrays.asList(Region.values()));

    private final String description;
    private final List<Region> allowedRegions;

    ComplianceZone(String description, List<Region> allowedRegions) {
        this.description = description;
        this.allowedRegions = allowedRegions;
    }
}
