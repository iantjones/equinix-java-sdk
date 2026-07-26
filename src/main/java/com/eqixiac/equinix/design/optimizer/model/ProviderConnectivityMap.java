package com.eqixiac.equinix.design.optimizer.model;

import com.eqixiac.equinix.core.model.MetroId;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps each recommended metro to the providers available there.
 */
@Value
public class ProviderConnectivityMap {

    /** Per-metro availability entries, one per requested provider. */
    Map<MetroId, List<ProviderAvailability>> metroProviders;

    /**
     * The availability entries for one metro.
     *
     * @param metro the metro to look up
     * @return the metro's provider entries, or an empty list for a metro not in the map
     */
    public List<ProviderAvailability> forMetro(MetroId metro) {
        return metroProviders.getOrDefault(metro, Collections.emptyList());
    }

    /**
     * Whether the named provider is available at the metro.
     *
     * <p>The label is matched by <strong>exact, case-sensitive equality</strong> against each
     * entry's provider label, which is the requirement's {@code displayLabel()} — the custom
     * label if one was set, else the {@code CloudProviderType} provider name, else the service
     * profile name/uuid. Pass that same label; a lowercase or partial name matches nothing.</p>
     *
     * @param metro         the metro to check
     * @param providerLabel the requirement's display label
     * @return {@code true} when an available entry with exactly that label exists at the metro
     */
    public boolean isAvailable(MetroId metro, String providerLabel) {
        return forMetro(metro).stream()
                .anyMatch(p -> p.getProviderLabel().equals(providerLabel) && p.isAvailable());
    }
}
