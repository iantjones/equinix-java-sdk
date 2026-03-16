package api.equinix.javasdk.fabric.optimizer.model;

import api.equinix.javasdk.core.enums.MetroCode;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps each recommended metro to the providers available there.
 */
@Value
public class ProviderConnectivityMap {

    Map<MetroCode, List<ProviderAvailability>> metroProviders;

    public List<ProviderAvailability> forMetro(MetroCode metro) {
        return metroProviders.getOrDefault(metro, Collections.emptyList());
    }

    public boolean isAvailable(MetroCode metro, String providerLabel) {
        return forMetro(metro).stream()
                .anyMatch(p -> p.getProviderLabel().equals(providerLabel) && p.isAvailable());
    }
}
