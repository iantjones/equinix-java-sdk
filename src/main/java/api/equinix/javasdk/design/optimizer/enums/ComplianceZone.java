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
 * <p><strong>Multi-zone semantics</strong>: a deployment satisfies multiple requested zones when
 * <em>each zone is covered by at least one selected metro</em> (a deployment-level AND, to which
 * every metro contributes the zones its region is allowed by) &mdash; no single metro is expected
 * to sit inside every zone at once. Accordingly the candidate filter keeps any metro that helps
 * satisfy at least one requested zone, the compliance score grades a metro by the fraction of
 * requested zones its region is allowed by, and a requested zone the selected set leaves uncovered
 * is surfaced as a {@code COMPLIANCE_GAP} risk finding.</p>
 *
 * @see api.equinix.javasdk.design.optimizer.model.OptimizationConstraints
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
     * Placeholder zone that allows every region — it constrains nothing and always scores as
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
