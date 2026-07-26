package com.eqixiac.equinix.design.optimizer.wizard;

import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic, collision-free name generator for the Deployment Wizard.
 *
 * <p>Equinix Fabric rejects any connection, Cloud Router or routing-protocol name whose length is
 * not greater than 0 and less than 24 characters (error {@code EQ-3142539}: <em>"connection name
 * length should be greater than 0 and less than 24 character"</em>). Full provider display names
 * blow straight past that limit &mdash; {@code globalpay-NY-to-amazon-web-services} is 35 characters
 * &mdash; so every generated name is routed through this one helper, which:</p>
 *
 * <ul>
 *   <li>maps provider labels to a compact token ({@code aws}, {@code azure}, {@code gcp}, ...)
 *       via {@link #providerToken(String)} instead of the full display name;</li>
 *   <li>caps every name to {@link #MAX_LEN} characters ({@code < 24}); and</li>
 *   <li>when a capped name would collide with one already issued, disambiguates it with a short,
 *       deterministic hash suffix so two resources in a plan never share a name.</li>
 * </ul>
 *
 * <p>Generation is deterministic: for a given optimization result and builder configuration the same
 * names are produced every time, because the disambiguating suffix is derived from the intended name
 * (via {@link String#hashCode()}, whose contract is fixed) rather than from any run-varying state.</p>
 *
 * <p>An instance is single-use per plan and is <strong>not</strong> thread-safe; the engine builds one
 * per {@code generatePlan} call.</p>
 */
final class PlanNames {

    /** Fabric's hard limit is {@code < 24} characters, so the longest legal name is 23. */
    static final int MAX_LEN = 23;

    /**
     * Longest router-name prefix the wizard composes names from before the universal cap takes over.
     * Keeps the common case ({@code prefix-METRO-to-token}) legible rather than hash-truncated; a
     * longer prefix is trimmed to this budget by {@link #validatePrefix(String)}.
     */
    static final int MAX_PREFIX_LEN = 12;

    /** Names already handed out by this generator, so later names dedupe around them. */
    private final Set<String> used = new HashSet<>();

    // ══════════════════════════════════════════════
    //  Provider tokens
    // ══════════════════════════════════════════════

    /**
     * Maps a Fabric provider label &mdash; a corporate name ("Amazon Web Services"), a product name
     * ("AWS Direct Connect", "Azure ExpressRoute") or a bare token ("AWS") &mdash; to a compact,
     * wire-safe token ({@code aws}, {@code azure}, {@code gcp}, {@code oci}, {@code ibm},
     * {@code alibaba}). Labels that match no well-known {@link CloudProviderType} fall back to a
     * sanitized, length-bounded slug of the label itself, so a third-party service profile still
     * yields a short, name-safe token.
     *
     * @param providerLabel the provider label from the optimization result; {@code null}/blank tolerated
     * @return a non-blank, lower-case token no longer than 8 characters
     */
    static String providerToken(String providerLabel) {
        if (providerLabel == null || providerLabel.isBlank()) {
            return "cloud";
        }
        for (CloudProviderType type : CloudProviderType.values()) {
            if (type.matchesServiceProfileName(providerLabel)) {
                return type.shortCode();
            }
        }
        String slug = slug(providerLabel);
        if (slug.isEmpty()) {
            return "cloud";
        }
        return slug.length() > 8 ? slug.substring(0, 8) : slug;
    }

    // ══════════════════════════════════════════════
    //  Prefix
    // ══════════════════════════════════════════════

    /**
     * Validates and normalizes a caller-supplied router-name prefix so the names composed from it
     * still fit Fabric's limit. The prefix is trimmed, reduced to name-safe characters, and &mdash;
     * because it is the stem of <em>every</em> generated resource name &mdash; bounded to
     * {@link #MAX_PREFIX_LEN} characters. A prefix longer than that budget is truncated (documented
     * truncation); the universal cap in {@link #unique(String)} still guarantees every final name is
     * legal even for a prefix at the budget.
     *
     * @param rawPrefix the caller-supplied prefix
     * @return the normalized prefix (trimmed, name-safe, at most {@link #MAX_PREFIX_LEN} characters)
     * @throws IllegalArgumentException if the prefix is {@code null}, blank, or contains no name-safe
     *         characters &mdash; there would be no stem left to compose names from
     */
    static String validatePrefix(String rawPrefix) {
        if (rawPrefix == null || rawPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Router name prefix must not be blank — it is the stem of every generated resource name.");
        }
        String cleaned = sanitizeSegment(rawPrefix.trim());
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(
                    "Router name prefix '" + rawPrefix + "' has no name-safe characters to compose names from.");
        }
        return cleaned.length() > MAX_PREFIX_LEN ? cleaned.substring(0, MAX_PREFIX_LEN) : cleaned;
    }

    // ══════════════════════════════════════════════
    //  Name allocation
    // ══════════════════════════════════════════════

    /**
     * Allocates a unique, name-safe, length-capped name from the given desired string. The result is
     * at most {@link #MAX_LEN} characters and never equal to a name already handed out by this
     * generator; a collision after capping is resolved with a short deterministic hash suffix.
     *
     * @param desired the intended name (composed from prefix, metro and token)
     * @return the allocated name
     */
    String unique(String desired) {
        String base = sanitizeSegment(desired == null ? "" : desired);
        String capped = trimDashes(cap(base));
        if (capped.isEmpty()) {
            capped = "conn";
        }
        if (used.add(capped)) {
            return capped;
        }
        return disambiguate(base.isEmpty() ? "conn" : base);
    }

    /**
     * Allocates a unique name of the form {@code head-suffix}, capped so the whole string is legal.
     * Used for routing-protocol names ({@code ...-DIRECT}, {@code ...-BGP}) derived from an
     * already-capped connection name: the base is truncated to leave room for the suffix, and a
     * collision (two connections whose bases truncate to the same head) is resolved with a hash.
     *
     * @param base   the base name to build on (already unique and capped)
     * @param suffix the trailing marker (for example {@code "DIRECT"} or {@code "BGP"})
     * @return the allocated name
     */
    String uniqueWithSuffix(String base, String suffix) {
        String cleanBase = sanitizeSegment(base == null ? "" : base);
        String cleanSuffix = sanitizeSegment(suffix == null ? "" : suffix);
        if (cleanSuffix.isEmpty()) {
            return unique(cleanBase);
        }
        int room = MAX_LEN - cleanSuffix.length() - 1; // -1 for the joining dash
        if (room < 1) {
            // The suffix alone leaves no room for even a one-character head — hash the whole thing.
            return disambiguate(cleanBase + "-" + cleanSuffix);
        }
        String head = trimDashes(cleanBase.length() > room ? cleanBase.substring(0, room) : cleanBase);
        if (head.isEmpty()) {
            head = "c";
        }
        String candidate = head + "-" + cleanSuffix;
        if (used.add(candidate)) {
            return candidate;
        }
        return disambiguate(cleanBase + "-" + cleanSuffix);
    }

    /**
     * Resolves a collision by appending a short deterministic hash of the intended name, truncating
     * the head to make room. Iterating the salt guarantees termination: the base-36 tag space
     * (&gt;1.6M values) dwarfs any plan's resource count, so a free slot is always found — in
     * practice on the first iteration.
     */
    private String disambiguate(String desired) {
        String base = sanitizeSegment(desired);
        if (base.isEmpty()) {
            base = "conn";
        }
        for (int salt = 0; salt < Integer.MAX_VALUE; salt++) {
            String tag = hashTag(base, salt);
            int room = MAX_LEN - tag.length() - 1;
            String head = trimDashes(base.length() > room ? base.substring(0, room) : base);
            if (head.isEmpty()) {
                head = "c";
            }
            String candidate = cap(head + "-" + tag);
            if (used.add(candidate)) {
                return candidate;
            }
        }
        // Unreachable for any realistically-sized plan.
        throw new IllegalStateException("could not allocate a unique name for '" + desired + "'");
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private static String cap(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= MAX_LEN ? s : s.substring(0, MAX_LEN);
    }

    /**
     * A short, deterministic base-36 tag (up to 4 characters) derived from the intended name and a
     * salt. Built from {@link String#hashCode()}, whose algorithm is specified, so the same plan
     * always yields the same names across JVMs and runs.
     */
    private static String hashTag(String base, int salt) {
        int h = (base.hashCode() * 31 + salt) & 0x7fffffff;
        String tag = Integer.toString(h, 36);
        return tag.length() > 4 ? tag.substring(tag.length() - 4) : tag;
    }

    /** Lower-cases and reduces to a name-safe slug: runs of non-{@code [a-z0-9]} become a single dash. */
    private static String slug(String raw) {
        return trimDashes(raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"));
    }

    /** Replaces every run of characters outside {@code [A-Za-z0-9_-]} with a single dash; trims dashes. */
    private static String sanitizeSegment(String raw) {
        return trimDashes(raw.replaceAll("[^A-Za-z0-9_-]+", "-"));
    }

    private static String trimDashes(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '-') {
            start++;
        }
        while (end > start && s.charAt(end - 1) == '-') {
            end--;
        }
        return s.substring(start, end);
    }
}
