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

package com.eqixiac.equinix.design.estate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Which part of an Equinix estate an analysis reads.
 *
 * <p>An account can see far more than one customer's resources (the test account behind this SDK's
 * live checks sees hundreds of routers and connections and dozens of projects owned by other
 * people), so the analyser never reads "everything" by accident. A scope names project ids, account
 * numbers, explicit resource ids, or a combination; an unscoped run must be requested with
 * {@link #entireAccount(int)}, which still caps how many resources of each type are read.</p>
 *
 * <p>Matching rules ({@link #admits(String, String, String)}): a resource is in scope when it
 * matches every non-empty project/account criterion, or when its id is listed explicitly.</p>
 *
 * @param projectIds         Fabric project ids; empty for no project criterion
 * @param accountNumbers     account numbers as text; empty for no account criterion
 * @param resourceIds        explicit resource ids (ports, connections, routers, networks, tokens,
 *                           devices) that are in scope whatever their project or account
 * @param entireAccount      {@code true} only when the caller explicitly asked for an unscoped run
 * @param maxResourcesPerType the most resources of one type the collector reads before it stops and
 *                           records the type as truncated
 */
public record EstateScope(Set<String> projectIds, Set<String> accountNumbers, Set<String> resourceIds,
                          boolean entireAccount, int maxResourcesPerType) {

    /** The default per-type ceiling: large enough for one customer's estate, small enough to stay polite. */
    public static final int DEFAULT_MAX_RESOURCES_PER_TYPE = 2000;

    /** Normalises the sets (sorted, immutable, blanks dropped) and validates the ceiling. */
    public EstateScope {
        projectIds = clean(projectIds);
        accountNumbers = clean(accountNumbers);
        resourceIds = clean(resourceIds);
        if (maxResourcesPerType <= 0) {
            throw new IllegalArgumentException("maxResourcesPerType must be positive");
        }
    }

    /**
     * A scope of one or more Fabric projects.
     *
     * @param projectIds the project ids
     * @return the scope
     */
    public static EstateScope projects(String... projectIds) {
        return new EstateScope(Set.of(projectIds), Set.of(), Set.of(), false, DEFAULT_MAX_RESOURCES_PER_TYPE);
    }

    /**
     * A scope of one or more billing accounts. Fabric searches cannot filter every resource type
     * by account, so an account scope reads up to {@link #maxResourcesPerType()} of each type and
     * keeps the matching ones; a larger estate is reported as truncated.
     *
     * @param accountNumbers the account numbers
     * @return the scope
     */
    public static EstateScope accounts(String... accountNumbers) {
        return new EstateScope(Set.of(), Set.of(accountNumbers), Set.of(), false, DEFAULT_MAX_RESOURCES_PER_TYPE);
    }

    /**
     * A scope of explicitly named resources.
     *
     * @param resourceIds the resource ids
     * @return the scope
     */
    public static EstateScope resources(Collection<String> resourceIds) {
        return new EstateScope(Set.of(), Set.of(), Set.copyOf(resourceIds), false, DEFAULT_MAX_RESOURCES_PER_TYPE);
    }

    /**
     * An explicitly requested unscoped run over everything the credentials can see, capped at
     * {@code maxResourcesPerType} resources of each type.
     *
     * @param maxResourcesPerType the per-type ceiling
     * @return the scope
     */
    public static EstateScope entireAccount(int maxResourcesPerType) {
        return new EstateScope(Set.of(), Set.of(), Set.of(), true, maxResourcesPerType);
    }

    /**
     * Returns a copy with additional explicit resource ids.
     *
     * @param ids the ids to add
     * @return the new scope
     */
    public EstateScope plusResources(String... ids) {
        Set<String> all = new TreeSet<>(resourceIds);
        all.addAll(List.of(ids));
        return new EstateScope(projectIds, accountNumbers, all, entireAccount, maxResourcesPerType);
    }

    /**
     * Returns a copy with a different per-type ceiling.
     *
     * @param max the ceiling
     * @return the new scope
     */
    public EstateScope withMaxResourcesPerType(int max) {
        return new EstateScope(projectIds, accountNumbers, resourceIds, entireAccount, max);
    }

    /**
     * Whether the scope names no project, account or resource.
     *
     * @return {@code true} for an unscoped run
     */
    @JsonIgnore
    public boolean isUnscoped() {
        return projectIds.isEmpty() && accountNumbers.isEmpty() && resourceIds.isEmpty();
    }

    /**
     * Whether a resource is in scope.
     *
     * @param projectId     the resource's project id, or {@code null} when the API does not report one
     * @param accountNumber the resource's account number, or {@code null}
     * @param resourceId    the resource id
     * @return {@code true} when the resource belongs to the scope
     */
    public boolean admits(String projectId, String accountNumber, String resourceId) {
        if (resourceId != null && resourceIds.contains(resourceId)) {
            return true;
        }
        if (projectIds.isEmpty() && accountNumbers.isEmpty()) {
            return resourceIds.isEmpty();
        }
        boolean projectOk = projectIds.isEmpty() || (projectId != null && projectIds.contains(projectId));
        boolean accountOk = accountNumbers.isEmpty() || (accountNumber != null && accountNumbers.contains(accountNumber));
        return projectOk && accountOk;
    }

    /**
     * A one-line description for reports.
     *
     * @return the description
     */
    public String describe() {
        if (isUnscoped()) {
            return "entire account (explicitly requested), at most " + maxResourcesPerType + " per resource type";
        }
        StringBuilder b = new StringBuilder();
        if (!projectIds.isEmpty()) {
            b.append("projects ").append(projectIds);
        }
        if (!accountNumbers.isEmpty()) {
            b.append(b.isEmpty() ? "" : "; ").append("accounts ").append(accountNumbers);
        }
        if (!resourceIds.isEmpty()) {
            b.append(b.isEmpty() ? "" : "; ").append(resourceIds.size()).append(" explicit resource id(s)");
        }
        return b.toString();
    }

    private static Set<String> clean(Set<String> values) {
        TreeSet<String> out = new TreeSet<>();
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    out.add(v.trim());
                }
            }
        }
        return java.util.Collections.unmodifiableSortedSet(out);
    }
}
