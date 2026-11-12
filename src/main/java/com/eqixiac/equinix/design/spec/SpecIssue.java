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

package com.eqixiac.equinix.design.spec;

/**
 * One problem found by {@link DesignSpec#validate()}.
 *
 * @param severity  {@link Severity#ERROR} makes the spec unusable; {@link Severity#WARNING} degrades a result
 * @param path      where in the document, for example {@code workloads[1].clouds[0]}
 * @param message   what is wrong and how to fix it
 */
public record SpecIssue(Severity severity, String path, String message) {

    /** How serious a {@link SpecIssue} is. */
    public enum Severity {
        /** The spec cannot be used until this is fixed. */
        ERROR,
        /** The spec can be used, but a result will be weaker or partly unknown. */
        WARNING
    }

    @Override
    public String toString() {
        return severity + " " + path + ": " + message;
    }
}
