/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.opencapacity.model;

import java.time.Instant;
import java.util.List;

/**
 * A provider's graded answer to a requirement. An answer is an indication. It is not an offer, a
 * quote, or a reservation, and it says nothing after {@code validUntil}.
 *
 * @param answerId the provider's identifier of the answer
 * @param reference the reference sent with the query, or {@code null}
 * @param generatedAt when the provider produced the answer
 * @param validUntil when the answer stops applying
 * @param grade the best grade among the results
 * @param results one result per location
 */
public record CapacityAnswer(
        String answerId, String reference, Instant generatedAt, Instant validUntil, Grade grade,
        List<LocationResult> results) {

    /** Creates an answer. */
    public CapacityAnswer {
        grade = grade == null ? Grade.NOT_DISCLOSED : grade;
        results = results == null ? List.of() : List.copyOf(results);
    }

    /**
     * Says whether the answer no longer applies.
     *
     * @param now the time to compare with
     * @return {@code true} if {@code now} is after {@code validUntil}
     */
    public boolean isExpired(Instant now) {
        return validUntil != null && now.isAfter(validUntil);
    }

    /**
     * Returns the results with a grade.
     *
     * @param wanted the grade
     * @return the results graded {@code wanted}, in the provider's order
     */
    public List<LocationResult> resultsGraded(Grade wanted) {
        return results.stream().filter(result -> result.grade() == wanted).toList();
    }
}
