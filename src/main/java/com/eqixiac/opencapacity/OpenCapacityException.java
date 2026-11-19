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

package com.eqixiac.opencapacity;

import com.eqixiac.opencapacity.model.Problem;
import java.util.Optional;

/**
 * A request to an OpenCapacity provider failed. {@link #status()} is the HTTP status, or 0 when no
 * response arrived. {@link #problem()} is the provider's problem document when it sent one.
 */
public class OpenCapacityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final transient Problem problem;

    /**
     * Creates an exception for a response.
     *
     * @param message what failed and what to do next
     * @param status the HTTP status, or 0
     * @param problem the provider's problem document, or {@code null}
     */
    public OpenCapacityException(String message, int status, Problem problem) {
        super(message);
        this.status = status;
        this.problem = problem;
    }

    /**
     * Creates an exception for a failure without a response.
     *
     * @param message what failed
     * @param cause the cause
     */
    public OpenCapacityException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
        this.problem = null;
    }

    /**
     * Returns the HTTP status.
     *
     * @return the status, or 0 when no response arrived
     */
    public int status() {
        return status;
    }

    /**
     * Returns the provider's problem document.
     *
     * @return the problem, or empty if the provider sent none
     */
    public Optional<Problem> problem() {
        return Optional.ofNullable(problem);
    }

    /**
     * Returns the provider's stable error code, for example {@code INCONSISTENT_DEMAND}.
     *
     * @return the code, or empty if the provider sent none
     */
    public Optional<String> code() {
        return problem().map(Problem::code);
    }
}
