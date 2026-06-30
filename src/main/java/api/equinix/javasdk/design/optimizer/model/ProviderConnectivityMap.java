package api.equinix.javasdk.design.optimizer.model;

import api.equinix.javasdk.core.model.MetroId;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps each recommended metro to the providers available there.
 */
@Value
public class ProviderConnectivityMap {

    Map<MetroId, List<ProviderAvailability>> metroProviders;

    public List<ProviderAvailability> forMetro(MetroId metro) {
        return metroProviders.getOrDefault(metro, Collections.emptyList());
    }

    public boolean isAvailable(MetroId metro, String providerLabel) {
        return forMetro(metro).stream()
                .anyMatch(p -> p.getProviderLabel().equals(providerLabel) && p.isAvailable());
    }
}
