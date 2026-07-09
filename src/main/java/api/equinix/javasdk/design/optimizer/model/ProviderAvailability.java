package api.equinix.javasdk.design.optimizer.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Indicates whether a specific provider is available at a metro,
 * and if so, which seller regions are supported.
 */
@Value
@Builder
public class ProviderAvailability {

    String providerLabel;
    boolean available;
    List<String> sellerRegions;
    String serviceProfileUuid;
}
