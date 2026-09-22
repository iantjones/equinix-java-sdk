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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;
import java.util.Objects;

/**
 * A provider id from the Connection Coordinator specification: the {@code {provider}} segment of
 * the resource name {@code providers/{provider}}.
 *
 * <p><b>Beta</b>: derived from the open specification at commit {@code bbfc763} (see the package
 * documentation). The specification states that provider ids are centrally assigned and stored in
 * its repository; at that commit no registry file exists and {@code governance/IMPLEMENTERS.md}
 * lists no implementers.</p>
 *
 * <h2>Normalization</h2>
 * <p>{@link #of(String)} trims surrounding whitespace, removes one optional leading {@code /} and
 * one optional {@code providers/} prefix, and lower-cases the remainder with {@link Locale#ROOT}.
 * Both spellings of the specification's {@code ProviderSites.providerId} field are accepted: its
 * description gives the format {@code providers/{provider}} and its inline example uses the bare
 * id. Two {@code ProviderRef}s are equal when their normalized ids are equal. {@link #toString()}
 * returns the normalized id.</p>
 *
 * <h2>Well-known constants</h2>
 * <table>
 *   <caption>Well-known provider ids and their source</caption>
 *   <tr><th>Constant</th><th>Id</th><th>Source</th></tr>
 *   <tr><td>{@link #AWS}</td><td>{@code aws}</td>
 *       <td>Specification, {@code docs/Protocols.md}: {@code /providers/aws}</td></tr>
 *   <tr><td>{@link #GCP}</td><td>{@code gcp}</td>
 *       <td>Specification, {@code docs/Protocols.md}: {@code /providers/gcp}</td></tr>
 *   <tr><td>{@link #OCI}</td><td>{@code oci}</td>
 *       <td>Unverified. Not present in the specification; chosen to equal
 *       {@code CloudProviderType.ORACLE_CLOUD.shortCode()}</td></tr>
 *   <tr><td>{@link #AZURE}</td><td>{@code azure}</td>
 *       <td>Unverified. Not present in the specification; chosen to equal
 *       {@code CloudProviderType.AZURE.shortCode()}</td></tr>
 *   <tr><td>{@link #EQUINIX}</td><td>{@code equinix}</td>
 *       <td>Unverified. Not present in the specification; Equinix has no
 *       {@code CloudProviderType} constant</td></tr>
 * </table>
 *
 * <h2>Bridge from the Fabric cloud-provider enum</h2>
 * <p>This type lives in {@code core} and does not reference any domain package. The Fabric enum
 * {@code com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType} bridges to it
 * through its {@code shortCode()} method:</p>
 * <pre>{@code
 * ProviderRef aws = ProviderRef.of(CloudProviderType.AWS.shortCode());   // equals ProviderRef.AWS
 * }</pre>
 * <p>{@code shortCode()} returns {@code aws}, {@code azure}, {@code gcp}, {@code oci},
 * {@code ibm}, {@code alibaba} and, for {@code CloudProviderType.OTHER}, the placeholder
 * {@code cloud}. The last three produce a valid {@code ProviderRef} for which
 * {@link #isWellKnown()} is {@code false}; {@code cloud} does not identify a provider and must not
 * be used to build a resource name.</p>
 *
 * @author ianjones
 */
public final class ProviderRef implements Comparable<ProviderRef> {

    /** The resource-name prefix of a provider: {@code providers/}. */
    public static final String RESOURCE_PREFIX = "providers/";

    /** Amazon Web Services. Id {@code aws}, as written in the specification's {@code docs/Protocols.md}. */
    public static final ProviderRef AWS = new ProviderRef("aws");

    /** Google Cloud. Id {@code gcp}, as written in the specification's {@code docs/Protocols.md}. */
    public static final ProviderRef GCP = new ProviderRef("gcp");

    /** Oracle Cloud Infrastructure. Id {@code oci}; unverified, see the class table. */
    public static final ProviderRef OCI = new ProviderRef("oci");

    /** Microsoft Azure. Id {@code azure}; unverified, see the class table. */
    public static final ProviderRef AZURE = new ProviderRef("azure");

    /** Equinix. Id {@code equinix}; unverified, see the class table. */
    public static final ProviderRef EQUINIX = new ProviderRef("equinix");

    private final String id;

    private ProviderRef(String id) {
        this.id = id;
    }

    /**
     * Creates a {@code ProviderRef} from a provider id ({@code "aws"}, {@code " AWS "}) or a
     * provider resource name ({@code "providers/aws"}, {@code "/providers/aws"}).
     *
     * @param idOrResourceName the provider id or {@code providers/{id}} resource name
     * @return the provider reference with a normalized, lower-case id
     * @throws NullPointerException if {@code idOrResourceName} is null
     * @throws IllegalArgumentException if the id is blank after normalization, or contains
     *         {@code /} or whitespace (for example a longer resource name such as
     *         {@code providers/aws/environments/x})
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProviderRef of(String idOrResourceName) {
        Objects.requireNonNull(idOrResourceName, "idOrResourceName");
        String normalized = idOrResourceName.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith(RESOURCE_PREFIX)) {
            normalized = normalized.substring(RESOURCE_PREFIX.length());
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("provider id must not be blank");
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '/' || Character.isWhitespace(c)) {
                throw new IllegalArgumentException(
                        "provider id must be a single path segment without whitespace: '" + idOrResourceName + "'");
            }
        }
        return new ProviderRef(normalized);
    }

    /**
     * @return the normalized, lower-case provider id (for example {@code aws})
     */
    @JsonValue
    public String id() {
        return id;
    }

    /**
     * @return the specification resource name of this provider: {@code providers/{id}}
     */
    public String resourceName() {
        return RESOURCE_PREFIX + id;
    }

    /**
     * @return {@code true} when this id equals one of the constants declared on this class. A
     *         {@code false} result does not mean the id is invalid; the specification assigns ids
     *         outside this SDK
     */
    public boolean isWellKnown() {
        return equals(AWS) || equals(GCP) || equals(OCI) || equals(AZURE) || equals(EQUINIX);
    }

    /** Orders by normalized id, lexicographically. */
    @Override
    public int compareTo(ProviderRef other) {
        return id.compareTo(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderRef)) {
            return false;
        }
        return id.equals(((ProviderRef) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** @return the normalized id, identical to {@link #id()} */
    @Override
    public String toString() {
        return id;
    }
}
