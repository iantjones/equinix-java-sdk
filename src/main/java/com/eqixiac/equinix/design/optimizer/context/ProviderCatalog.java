/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.eqixiac.equinix.design.optimizer.context;

import com.eqixiac.equinix.core.model.MetroId;
import com.eqixiac.equinix.design.optimizer.model.ProviderAvailability;
import com.eqixiac.equinix.design.optimizer.model.ServiceProfileOption;
import com.eqixiac.equinix.design.spec.CloudIntent;
import com.eqixiac.equinix.fabric.model.ServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.AccessPointTypeConfig;
import com.eqixiac.equinix.fabric.model.implementation.ServiceProfileMetro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Resolves each {@link CloudIntent} of a spec to the Fabric service profiles that serve it and the
 * metros those profiles publish: the source of {@code reachableClouds} per metro.
 *
 * <p>Matching: an explicit {@code serviceProfileUuid} wins, then a case-insensitive
 * {@code serviceProfileName} substring, then the provider's own profile-name rules
 * ({@code CloudProviderType.matchesServiceProfileName}). When several profiles serve the same metro,
 * one is chosen per metro by a total order (the profile publishing the cloud's {@code region} as a
 * seller region first, then one that publishes any seller region, then the smallest uuid), so the
 * result never depends on the order the search endpoint returned. Every matching profile's
 * bandwidth capability is still carried on {@code ProviderAvailability.profileOptions} so the
 * Deployment Wizard can pick a profile that covers the bandwidth it plans.</p>
 */
public final class ProviderCatalog {

    private final Map<String, Map<MetroId, ProviderAvailability>> byCloud;
    private final Map<String, Integer> profilesMatched;

    private ProviderCatalog(Map<String, Map<MetroId, ProviderAvailability>> byCloud,
                            Map<String, Integer> profilesMatched) {
        this.byCloud = byCloud;
        this.profilesMatched = profilesMatched;
    }

    /**
     * @param clouds   the spec's clouds
     * @param profiles the service profiles read from Fabric
     * @return the catalogue
     */
    public static ProviderCatalog resolve(List<CloudIntent> clouds, List<ServiceProfile> profiles) {
        Map<String, Map<MetroId, ProviderAvailability>> byCloud = new LinkedHashMap<>();
        Map<String, Integer> matchedCounts = new LinkedHashMap<>();
        for (CloudIntent cloud : clouds) {
            Map<MetroId, ProviderAvailability> chosen = new TreeMap<>(java.util.Comparator.comparing(MetroId::code));
            Map<MetroId, List<ServiceProfileOption>> options = new LinkedHashMap<>();
            int matched = 0;
            for (ServiceProfile profile : profiles) {
                if (profile == null || !matches(profile, cloud)) {
                    continue;
                }
                matched++;
                List<ServiceProfileMetro> metros = profile.metros();
                if (metros == null) {
                    continue;
                }
                List<Integer> tiers = new ArrayList<>();
                boolean allowCustom = false;
                if (profile.getAccessPointTypeConfigs() != null) {
                    for (AccessPointTypeConfig cfg : profile.getAccessPointTypeConfigs()) {
                        if (cfg == null) {
                            continue;
                        }
                        allowCustom |= Boolean.TRUE.equals(cfg.getAllowCustomBandwidth());
                        if (cfg.getSupportedBandwidths() != null) {
                            tiers.addAll(cfg.getSupportedBandwidths());
                        }
                    }
                }
                for (ServiceProfileMetro spm : metros) {
                    if (spm == null || spm.metroId() == null) {
                        continue;
                    }
                    List<String> regions = spm.getSellerRegions() == null ? List.of()
                            : new ArrayList<>(spm.getSellerRegions().keySet());
                    ProviderAvailability candidate = ProviderAvailability.builder()
                            .providerLabel(cloud.id()).available(true).sellerRegions(regions)
                            .serviceProfileUuid(profile.getUuid()).build();
                    ProviderAvailability incumbent = chosen.get(spm.metroId());
                    if (incumbent == null || outranks(candidate, incumbent, cloud.region())) {
                        chosen.put(spm.metroId(), candidate);
                    }
                    options.computeIfAbsent(spm.metroId(), k -> new ArrayList<>())
                            .add(ServiceProfileOption.builder().serviceProfileUuid(profile.getUuid())
                                    .sellerRegions(regions).supportedBandwidths(new ArrayList<>(tiers))
                                    .allowCustomBandwidth(allowCustom).vcBandwidthMax(spm.getVcBandwidthMax())
                                    .build());
                }
            }
            Map<MetroId, ProviderAvailability> withOptions = new LinkedHashMap<>();
            chosen.forEach((metro, availability) -> withOptions.put(metro, availability.toBuilder()
                    .profileOptions(options.getOrDefault(metro, List.of())).build()));
            byCloud.put(cloud.id(), Collections.unmodifiableMap(withOptions));
            matchedCounts.put(cloud.id(), matched);
        }
        return new ProviderCatalog(byCloud, matchedCounts);
    }

    /** @return per cloud id, the chosen profile at every metro the cloud is reachable from */
    public Map<String, Map<MetroId, ProviderAvailability>> byCloud() {
        return Collections.unmodifiableMap(byCloud);
    }

    /**
     * @param cloudId a spec cloud id
     * @return how many service profiles matched the cloud's selector, whatever their coverage
     */
    public int profilesMatched(String cloudId) {
        return profilesMatched.getOrDefault(cloudId, 0);
    }

    /**
     * @param cloud a spec cloud
     * @return why the cloud reaches no metro, or {@code null} when it reaches at least one
     */
    public String unresolvedReason(CloudIntent cloud) {
        Map<MetroId, ProviderAvailability> metros = byCloud.get(cloud.id());
        if (metros != null && !metros.isEmpty()) {
            return null;
        }
        int matched = profilesMatched(cloud.id());
        return matched == 0
                ? "no Fabric service profile matches cloud '" + cloud.id() + "' (" + selector(cloud) + ")"
                : matched + " service profile(s) match cloud '" + cloud.id() + "' but publish no metro";
    }

    static boolean matches(ServiceProfile profile, CloudIntent cloud) {
        if (cloud.serviceProfileUuid() != null) {
            return cloud.serviceProfileUuid().equals(profile.getUuid());
        }
        if (cloud.serviceProfileName() != null) {
            return profile.getName() != null && profile.getName().toLowerCase(Locale.ROOT)
                    .contains(cloud.serviceProfileName().toLowerCase(Locale.ROOT));
        }
        return cloud.provider() != null && cloud.provider().matchesServiceProfileName(profile.getName());
    }

    private static String selector(CloudIntent cloud) {
        if (cloud.serviceProfileUuid() != null) {
            return "service profile uuid " + cloud.serviceProfileUuid();
        }
        if (cloud.serviceProfileName() != null) {
            return "profile name containing '" + cloud.serviceProfileName() + "'";
        }
        return "provider " + cloud.provider();
    }

    private static boolean outranks(ProviderAvailability candidate, ProviderAvailability incumbent, String region) {
        int byRegion = Integer.compare(fitness(candidate, region), fitness(incumbent, region));
        if (byRegion != 0) {
            return byRegion > 0;
        }
        String a = candidate.getServiceProfileUuid();
        String b = incumbent.getServiceProfileUuid();
        if (Objects.equals(a, b)) {
            return false;
        }
        if (a == null) {
            return false;
        }
        return b == null || a.compareTo(b) < 0;
    }

    private static int fitness(ProviderAvailability availability, String region) {
        List<String> regions = availability.getSellerRegions();
        if (regions == null || regions.isEmpty()) {
            return 0;
        }
        return region != null && regions.contains(region) ? 3 : 1;
    }
}
