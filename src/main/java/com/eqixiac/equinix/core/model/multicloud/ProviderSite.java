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

package com.eqixiac.equinix.core.model.multicloud;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * One side of a specification environment: a provider and that provider's own name for a site
 * ({@code environment.yaml#/ProviderSites}: {@code providerId}, {@code site},
 * {@code displayName}).
 *
 * <p><b>Beta</b>: derived from the open specification at commit {@code bbfc763} (see the package
 * documentation).</p>
 *
 * <table>
 *   <caption>Fields</caption>
 *   <tr><th>Field</th><th>Specification field</th><th>Constraint</th></tr>
 *   <tr><td>{@code providerRef}</td><td>{@code providerId}</td><td>required</td></tr>
 *   <tr><td>{@code site}</td><td>{@code site}</td>
 *       <td>required, not blank; surrounding whitespace is trimmed; case is preserved</td></tr>
 *   <tr><td>{@code displayName}</td><td>{@code displayName}</td>
 *       <td>optional; {@code null} when the provider supplied none</td></tr>
 * </table>
 *
 * <p>A site name is provider-specific: a cloud region for a cloud provider (the specification's
 * examples are {@code us-east-1} and {@code us-east4}). The specification does not define what an
 * Equinix site name would be; a metro code is an assumption, not a verified value.</p>
 *
 * <p>Equality compares all three fields exactly. Use {@link #matches(ProviderRef, String)} for a
 * comparison that ignores {@code displayName} and the case of {@code site}.</p>
 *
 * <p>Construct with {@link #of(ProviderRef, String)} or the Lombok builder
 * ({@code ProviderSite.builder().providerRef(...).site(...).displayName(...).build()}).</p>
 *
 * @author ianjones
 */
@Value
public class ProviderSite {

    /** The provider this site belongs to. Never null. */
    ProviderRef providerRef;

    /** The provider-specific site name, for example {@code us-east4}. Never blank. */
    String site;

    /** A name for the site suitable for customer-facing display, or {@code null}. */
    String displayName;

    @Builder
    private ProviderSite(ProviderRef providerRef, String site, String displayName) {
        this.providerRef = Objects.requireNonNull(providerRef, "providerRef");
        Objects.requireNonNull(site, "site");
        String trimmed = site.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("site must not be blank");
        }
        this.site = trimmed;
        this.displayName = displayName;
    }

    /**
     * Creates a site without a display name.
     *
     * @param providerRef the provider
     * @param site the provider-specific site name
     * @return the provider site
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if {@code site} is blank
     */
    public static ProviderSite of(ProviderRef providerRef, String site) {
        return new ProviderSite(providerRef, site, null);
    }

    /**
     * Tests whether this is the given provider's given site. {@code displayName} is not compared.
     * The site comparison trims {@code otherSite} and ignores case.
     *
     * @param provider the provider to compare; {@code null} never matches
     * @param otherSite the site name to compare; {@code null} never matches
     * @return {@code true} when the provider is equal and the site names match
     */
    public boolean matches(ProviderRef provider, String otherSite) {
        return provider != null
                && otherSite != null
                && providerRef.equals(provider)
                && site.equalsIgnoreCase(otherSite.trim());
    }
}
