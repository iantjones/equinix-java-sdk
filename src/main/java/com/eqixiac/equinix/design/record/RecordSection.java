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


package com.eqixiac.equinix.design.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One section of a {@link DesignDecisionRecord}: its content, or why there is none.
 *
 * @param status  whether the section was produced
 * @param reason  why it was skipped or failed; {@code null} when included
 * @param content the section's content; {@code null} unless included
 * @param <T>     the content type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecordSection<T>(Status status, String reason, T content) {

    /** Whether a section was produced. */
    public enum Status {
        /** Produced from the engines' results. */
        INCLUDED,
        /** Not produced because its inputs are absent (for example no egress volume for a TCO view). */
        SKIPPED,
        /** Not produced because an engine failed; {@code reason} names the error. */
        FAILED
    }

    /** Requires content exactly when included, and a reason otherwise. */
    public RecordSection {
        if (status == null) {
            throw new IllegalArgumentException("A section needs a status");
        }
        if (status == Status.INCLUDED && content == null) {
            throw new IllegalArgumentException("An included section needs content");
        }
        if (status != Status.INCLUDED && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A skipped or failed section needs a reason");
        }
    }

    /**
     * @param content the content
     * @param <T>     its type
     * @return an included section
     */
    public static <T> RecordSection<T> included(T content) {
        return new RecordSection<>(Status.INCLUDED, null, content);
    }

    /**
     * @param reason why the section has no content
     * @param <T>    the content type
     * @return a skipped section
     */
    public static <T> RecordSection<T> skipped(String reason) {
        return new RecordSection<>(Status.SKIPPED, reason, null);
    }

    /**
     * @param reason what failed
     * @param <T>    the content type
     * @return a failed section
     */
    public static <T> RecordSection<T> failed(String reason) {
        return new RecordSection<>(Status.FAILED, reason, null);
    }

    /** @return {@code true} when the section has content */
    @JsonIgnore
    public boolean isIncluded() {
        return status == Status.INCLUDED;
    }
}
