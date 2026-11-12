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
package com.eqixiac.equinix.fabric.beta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an SDK type that maps a Fabric v4 API family the OpenAPI spec labels <strong>Beta</strong>.
 *
 * <p>Beta endpoints can change shape or be withdrawn by Equinix without the usual deprecation period, and
 * need the account to be entitled to them. Types carrying this annotation follow the spec version in
 * {@link #specVersion()} and may change in a minor SDK release when the spec changes.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE})
public @interface Beta {

    /**
     * The Fabric v4 spec version the annotated type was written against.
     *
     * @return the spec version
     */
    String specVersion() default "4.34.0";

    /**
     * The date the spec was read for the annotated type.
     *
     * @return an ISO-8601 date
     */
    String specDate() default "2026-11-11";
}
