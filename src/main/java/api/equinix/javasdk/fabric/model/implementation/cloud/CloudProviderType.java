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

package api.equinix.javasdk.fabric.model.implementation.cloud;

import lombok.Getter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Enumeration of supported cloud providers for Equinix Fabric interconnection.
 *
 * <p>Each constant represents a cloud provider whose direct connectivity product can be
 * integrated with Equinix Fabric connections. The enum provides the display name and
 * the cloud provider's connectivity product name for reference.</p>
 *
 * <p>Each constant also carries the set of terms a Fabric <em>service profile</em> may be
 * named with for that provider &mdash; the Fabric product name, the corporate name, the
 * constant name, and curated aliases &mdash; so that callers matching marketplace profiles
 * to a provider can do so with {@code matchesServiceProfileName(String)} instead of guessing
 * at one particular spelling. Real marketplace profiles are named after the <em>product</em>
 * ("AWS Direct Connect", "Azure ExpressRoute"), not after the corporation
 * ("Amazon Web Services"), so matching on a single name is not sufficient.</p>
 *
 * <h2>What counts as evidence</h2>
 *
 * <p>A profile-name match is used to decide that a cloud is <em>reachable</em> from a metro, so a
 * false positive is worse than a false negative: it silently promises an on-ramp that does not
 * exist. The alias sets are therefore restricted to terms that are distinctive to the provider:</p>
 *
 * <ul>
 *   <li><strong>Admissible</strong> &mdash; coined brand tokens unique to the provider
 *       ({@code aws}, {@code azure}, {@code expressroute}, {@code gcp}, {@code oci},
 *       {@code fastconnect}, {@code aliyun}), including their spaced spellings
 *       ({@code express route}, {@code fast connect}), and corporate names whose Fabric
 *       presence <em>is</em> that corporation's cloud ({@code amazon}, {@code google},
 *       {@code oracle}, {@code ibm}, {@code alibaba}).</li>
 *   <li><strong>Not admissible</strong> &mdash; descriptive product phrases assembled from
 *       ordinary industry words: "direct connect", "direct link", "express connect",
 *       "cloud interconnect", "partner interconnect", "dedicated interconnect". Network service
 *       providers brand their own on-ramps with exactly these words, so "&lt;NSP&gt; Direct
 *       Connect" is not evidence of AWS.</li>
 *   <li><strong>Not admissible</strong> &mdash; {@code microsoft} on its own: Microsoft's Fabric
 *       presence is broader than Azure ("Microsoft 365"), so the bare corporate token does not
 *       identify an ExpressRoute on-ramp.</li>
 * </ul>
 *
 * <p>Every real product name still resolves, because each one carries a brand token: "AWS Direct
 * Connect" and "Amazon Web Services Direct Connect" both match {@link #AWS}, "IBM Cloud Direct
 * Link" matches {@link #IBM_CLOUD}, and seller-prefixed or region-suffixed variants
 * ("Equinix AWS Direct Connect", "AWS Direct Connect - Sydney") match too.</p>
 *
 * @author ianjones
 * @see CloudProviderConnectionAdapter
 */
@Getter
public enum CloudProviderType {

    /**
     * Amazon Web Services &mdash; Direct Connect.
     * Uses AWS Account ID as the authentication key.
     *
     * <p>"direct connect" is deliberately <em>not</em> an alias: it is a phrase many network
     * service providers brand their own on-ramps with.</p>
     */
    AWS("AWS Direct Connect", "Amazon Web Services",
            "aws", "amazon"),

    /**
     * Microsoft Azure &mdash; ExpressRoute.
     * Uses ExpressRoute Service Key (GUID) as the authentication key.
     *
     * <p>The bare token "microsoft" is deliberately <em>not</em> an alias: Microsoft sells Fabric
     * profiles that are not ExpressRoute on-ramps ("Microsoft 365").</p>
     */
    AZURE("Azure ExpressRoute", "Microsoft Azure",
            "azure", "expressroute", "express route"),

    /**
     * Google Cloud Platform &mdash; Cloud Interconnect.
     * Uses GCP Pairing Key as the authentication key.
     *
     * <p>"cloud interconnect", "partner interconnect" and "dedicated interconnect" are
     * deliberately <em>not</em> aliases: they are generic interconnection wording. Every Fabric
     * profile for this product carries "Google" or "GCP".</p>
     */
    GOOGLE_CLOUD("Google Cloud Interconnect", "Google Cloud Platform",
            "google", "gcp"),

    /**
     * Oracle Cloud Infrastructure &mdash; FastConnect.
     * Uses Oracle OCID of the virtual circuit as the authentication key.
     */
    ORACLE_CLOUD("Oracle FastConnect", "Oracle Cloud Infrastructure",
            "oracle", "oci", "fastconnect", "fast connect"),

    /**
     * IBM Cloud &mdash; Direct Link.
     * Uses IBM Cloud Direct Link identifier as the authentication key.
     *
     * <p>"direct link" is deliberately <em>not</em> an alias: like "direct connect" it is generic
     * carrier wording rather than evidence of IBM.</p>
     */
    IBM_CLOUD("IBM Cloud Direct Link", "IBM Cloud",
            "ibm"),

    /**
     * Alibaba Cloud &mdash; Express Connect.
     * Uses Alibaba Cloud Account ID as the authentication key.
     *
     * <p>"express connect" is deliberately <em>not</em> an alias: it is two ordinary industry
     * words rather than a coined brand token. Fabric names this profile "Alibaba Cloud Express
     * Connect", which matches on "alibaba".</p>
     */
    ALIBABA_CLOUD("Alibaba Express Connect", "Alibaba Cloud",
            "alibaba", "aliyun"),

    /**
     * Custom or unlisted cloud provider.
     * Use this for providers not covered by the built-in constants.
     *
     * <p>Deliberately carries no match terms: it is a placeholder, not a nameable
     * provider, so {@code matchesServiceProfileName(String)} always returns
     * {@code false} for it. Identify such providers by service profile name or UUID
     * instead.</p>
     */
    OTHER("Custom Provider", "Other");

    private final String displayName;
    private final String providerName;

    /**
     * Normalized, lower-case terms that identify this provider in a Fabric service
     * profile name. Empty for {@link #OTHER}.
     *
     * <p>Holds the curated aliases plus this constant's own display name, provider name and
     * constant name. The three self-names are subsumed by the aliases in every current case &mdash;
     * they are carried so that a provider always matches its own names by construction, and they
     * cannot widen matching because a longer whole-phrase term only matches names that already
     * contain the shorter alias.</p>
     */
    private final List<String> matchTerms;

    CloudProviderType(String displayName, String providerName, String... aliases) {
        this.displayName = displayName;
        this.providerName = providerName;
        if (aliases.length == 0) {
            this.matchTerms = List.of();
        }
        else {
            Set<String> terms = new LinkedHashSet<>();
            addTerm(terms, displayName);
            addTerm(terms, providerName);
            addTerm(terms, name());
            for (String alias : aliases) {
                addTerm(terms, alias);
            }
            this.matchTerms = List.copyOf(terms);
        }
    }

    /**
     * Returns {@code true} when the given Fabric service profile name identifies this
     * provider.
     *
     * <p>Matching is case-insensitive and token-aware: the profile name and every match term are
     * normalized to lower-case, single-space-separated alphanumeric tokens, and a term matches only
     * where it appears in the profile name on whole-token boundaries. That keeps short terms such
     * as {@code "aws"} from matching inside unrelated words &mdash; the Warsaw metro and a profile
     * named "Kawasaki" do not satisfy a required-AWS constraint &mdash; while seller prefixes and
     * region suffixes are tolerated ("Equinix AWS Direct Connect", "AWS Direct Connect - Sydney").</p>
     *
     * <p>Matching is <strong>one-directional only</strong>: the profile name must contain a match
     * term, never the other way round. A profile whose name is merely a sub-phrase of one of this
     * provider's terms &mdash; "Web Services", "Cloud Platform", "Cloud Infrastructure", "Direct
     * Connect" &mdash; does <em>not</em> resolve, because such a name is equally consistent with a
     * third-party seller's own product and claiming a cloud is reachable where it is not is the
     * more damaging error.</p>
     *
     * @param serviceProfileName the Fabric service profile name; {@code null} is tolerated
     * @return {@code true} if the name identifies this provider
     */
    public boolean matchesServiceProfileName(String serviceProfileName) {
        if (serviceProfileName == null || matchTerms.isEmpty()) return false;

        String candidate = normalize(serviceProfileName);
        if (candidate.isEmpty()) return false;

        String paddedCandidate = " " + candidate + " ";
        for (String term : matchTerms) {
            if (paddedCandidate.contains(" " + term + " ")) return true;
        }
        return false;
    }

    /**
     * A compact, wire-safe token for this provider &mdash; {@code aws}, {@code azure}, {@code gcp},
     * {@code oci}, {@code ibm} or {@code alibaba} &mdash; suitable for composing Fabric resource
     * names that must stay within Fabric's 24-character limit.
     *
     * <p>Distinct from {@link #name()} lower-cased, which would yield {@code google_cloud} and
     * {@code oracle_cloud} rather than the conventional {@code gcp}/{@code oci}. {@link #OTHER}, a
     * placeholder rather than a nameable provider, returns the generic {@code cloud}.</p>
     *
     * @return the short provider token, always non-blank and already lower-case
     */
    public String shortCode() {
        return switch (this) {
            case AWS -> "aws";
            case AZURE -> "azure";
            case GOOGLE_CLOUD -> "gcp";
            case ORACLE_CLOUD -> "oci";
            case IBM_CLOUD -> "ibm";
            case ALIBABA_CLOUD -> "alibaba";
            case OTHER -> "cloud";
        };
    }

    private static void addTerm(Set<String> terms, String raw) {
        String normalized = normalize(raw);
        if (!normalized.isEmpty()) terms.add(normalized);
    }

    /**
     * Lower-cases the input and collapses every run of non-alphanumeric characters into a
     * single space, so "Oracle Cloud Infrastructure -FastConnect" and "GOOGLE_CLOUD" become
     * "oracle cloud infrastructure fastconnect" and "google cloud" respectively.
     *
     * <p>Written without static state so it is safe to call from the enum constructor.</p>
     */
    private static String normalize(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        boolean pendingSpace = false;
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                if (pendingSpace && sb.length() > 0) sb.append(' ');
                pendingSpace = false;
                sb.append(ch);
            }
            else {
                pendingSpace = true;
            }
        }
        return sb.toString();
    }
}
