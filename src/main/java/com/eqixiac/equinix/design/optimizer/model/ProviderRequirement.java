package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * A provider or service that must (or should) be reachable from the recommended metros.
 * Can reference a well-known {@link CloudProviderType} or a Fabric service profile by name.
 */
@Value
@Builder
public class ProviderRequirement {

    /**
     * A well-known cloud provider type (e.g., AWS, AZURE, GCP). Mutually exclusive
     * with {@link #serviceProfileName} and {@link #serviceProfileUuid} -- set one
     * of the three to identify the provider.
     */
    CloudProviderType cloudProvider;

    /**
     * A Fabric service profile name to match (case-insensitive substring match).
     * Use this when the provider is not a well-known {@link CloudProviderType}.
     */
    String serviceProfileName;

    /**
     * A specific Fabric service profile UUID. Provides exact matching when the
     * profile UUID is known, bypassing name-based lookups.
     */
    String serviceProfileUuid;

    String label;

    List<String> preferredSellerRegions;

    boolean required;

    /**
     * Returns a display label for this requirement.
     */
    public String displayLabel() {
        if (label != null) return label;
        if (cloudProvider != null) return cloudProvider.getProviderName();
        if (serviceProfileName != null) return serviceProfileName;
        return serviceProfileUuid != null ? serviceProfileUuid : "Unknown Provider";
    }
}
