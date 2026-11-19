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

import java.util.List;

/**
 * The body of a failed request: an RFC 9457 problem document with a stable code and field-level
 * errors.
 *
 * @param type a URI that identifies the kind of problem
 * @param title a short description of the kind of problem
 * @param status the HTTP status
 * @param code the stable, machine-readable error code, for example {@code INCONSISTENT_DEMAND}
 * @param detail what went wrong in this request, or {@code null}
 * @param instance a URI for this occurrence, or {@code null}
 * @param errors the field-level errors
 */
public record Problem(
        String type, String title, Integer status, String code, String detail, String instance,
        List<FieldError> errors) {

    /** Creates a problem. */
    public Problem {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
