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

package com.eqixiac.equinix.fabric.model.implementation.filter;

import com.eqixiac.equinix.core.http.ParameterMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A valid {@code POST /fabric/v4/cloudevents/search} filter, built so that the scope expression
 * the service requires is always present.
 *
 * <p>Live behaviour (observed 2026-11-11): without a {@code /subject}, {@code /equinixproject} or
 * {@code /equinixorganization} expression the service answers {@code HTTP 400 EQ-3045014}
 * "subject/equinixproject/equinixorganization must be in the filter list". A subject matches as a
 * prefix: {@code /fabric/v4/connections/{uuid}} also returns the events of that connection's
 * routing protocols. A trailing {@code *} is accepted, as in the spec example.</p>
 *
 * <p>UTC contract: {@code LocalDateTime} inputs are UTC wall clock.</p>
 */
public final class CloudEventQuery {

    /** Filter property of the event subject. */
    public static final String SUBJECT = "/subject";
    /** Filter property of the event's Equinix project. */
    public static final String PROJECT = "/equinixproject";
    /** Filter property of the event's Equinix organization. */
    public static final String ORGANIZATION = "/equinixorganization";
    /** Filter property of the event type. */
    public static final String TYPE = "/type";
    /** Filter property of the event time. */
    public static final String TIME = "/time";

    private final String scopeProperty;
    private final List<Object> scopeValues;
    private final List<Object> types = new ArrayList<>();
    private LocalDateTime from;
    private LocalDateTime to;

    private CloudEventQuery(String scopeProperty, List<Object> scopeValues) {
        this.scopeProperty = scopeProperty;
        this.scopeValues = scopeValues;
    }

    /**
     * Starts a query for one or more subject paths, for example {@code /fabric/v4/ports/{uuid}}.
     *
     * @param subjects the subject paths (at least one)
     * @return a new query
     */
    public static CloudEventQuery forSubjects(String... subjects) {
        Objects.requireNonNull(subjects, "subjects");
        if (subjects.length == 0) {
            throw new IllegalArgumentException("at least one subject is required");
        }
        List<Object> values = new ArrayList<>();
        for (String s : subjects) {
            requireText(s, "subject");
            values.add(s);
        }
        return new CloudEventQuery(SUBJECT, values);
    }

    /**
     * Starts a query for one asset, for example {@code forAsset("connections", uuid)} for subject
     * {@code /fabric/v4/connections/{uuid}} and everything below it.
     *
     * @param assetCollection the Fabric collection name ({@code connections}, {@code ports},
     *        {@code routers}, {@code serviceTokens}, ...)
     * @param assetUuid the asset uuid
     * @return a new query
     */
    public static CloudEventQuery forAsset(String assetCollection, String assetUuid) {
        requireText(assetCollection, "assetCollection");
        requireText(assetUuid, "assetUuid");
        return forSubjects("/fabric/v4/" + assetCollection + "/" + assetUuid);
    }

    /**
     * Starts a query for every event of one Equinix project.
     *
     * @param projectId the project id
     * @return a new query
     */
    public static CloudEventQuery forProject(String projectId) {
        requireText(projectId, "projectId");
        return new CloudEventQuery(PROJECT, List.of(projectId));
    }

    /**
     * Starts a query for every event of one Equinix organization.
     *
     * @param organizationId the organization id
     * @return a new query
     */
    public static CloudEventQuery forOrganization(String organizationId) {
        requireText(organizationId, "organizationId");
        return new CloudEventQuery(ORGANIZATION, List.of(organizationId));
    }

    /**
     * Restricts the events to the given types; a trailing wildcard is allowed, for example
     * {@code equinix.fabric.connection.*}.
     *
     * @param eventTypes the event types
     * @return this query
     */
    public CloudEventQuery types(String... eventTypes) {
        Objects.requireNonNull(eventTypes, "eventTypes");
        for (String t : eventTypes) {
            requireText(t, "event type");
            this.types.add(t);
        }
        return this;
    }

    /**
     * Restricts the events to those after {@code from}.
     *
     * @param from the exclusive lower bound, UTC wall clock
     * @return this query
     */
    public CloudEventQuery after(LocalDateTime from) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = null;
        return this;
    }

    /**
     * Restricts the events to those between {@code from} and {@code to}.
     *
     * @param from the start, UTC wall clock
     * @param to the end, UTC wall clock (not before {@code from})
     * @return this query
     */
    public CloudEventQuery between(LocalDateTime from, LocalDateTime to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        this.from = from;
        this.to = to;
        return this;
    }

    /**
     * Builds the search filter.
     *
     * @return a filter with the scope, the optional types and the optional time bound
     */
    public FilterPropertyList toFilter() {
        FilterPropertyList filter = Filter.filter().and();
        if (scopeValues.size() == 1 && !SUBJECT.equals(scopeProperty)) {
            filter.equals(scopeProperty, scopeValues.get(0));
        } else {
            filter.in(scopeProperty, new ArrayList<>(scopeValues));
        }
        if (!types.isEmpty()) {
            filter.in(TYPE, new ArrayList<>(types));
        }
        if (from != null && to != null) {
            filter.between(TIME, ParameterMapper.dateTimeForQuery(from), ParameterMapper.dateTimeForQuery(to));
        } else if (from != null) {
            filter.greaterThan(TIME, ParameterMapper.dateTimeForQuery(from));
        }
        return filter;
    }

    /**
     * Checks that a hand-built filter carries one of the scope expressions the search requires.
     *
     * @param filter the filter to check
     * @throws IllegalArgumentException when none of {@code /subject}, {@code /equinixproject} or
     *         {@code /equinixorganization} is present
     */
    public static void validate(FilterPropertyList filter) {
        if (filter == null || !(filter.hasProperty(SUBJECT) || filter.hasProperty(PROJECT) || filter.hasProperty(ORGANIZATION))) {
            throw new IllegalArgumentException(
                    "A cloud-events search filter needs /subject, /equinixproject or /equinixorganization; use CloudEventQuery");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
