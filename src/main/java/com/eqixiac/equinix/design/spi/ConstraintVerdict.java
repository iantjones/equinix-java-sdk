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

package com.eqixiac.equinix.design.spi;

/**
 * The outcome of checking one {@link Constraint} against one portfolio.
 *
 * @param constraintId  the {@link Constraint#id()}
 * @param satisfied     whether the portfolio is allowed
 * @param reason        why it failed, or what was verified; shown to the reader
 */
public record ConstraintVerdict(String constraintId, boolean satisfied, String reason) {

    /**
     * @param constraintId the constraint
     * @param reason       what was verified
     * @return a passing verdict
     */
    public static ConstraintVerdict pass(String constraintId, String reason) {
        return new ConstraintVerdict(constraintId, true, reason);
    }

    /**
     * @param constraintId the constraint
     * @param reason       why the portfolio is not allowed
     * @return a failing verdict
     */
    public static ConstraintVerdict fail(String constraintId, String reason) {
        return new ConstraintVerdict(constraintId, false, reason);
    }
}
