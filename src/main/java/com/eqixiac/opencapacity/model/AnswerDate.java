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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.Optional;

/**
 * A date in an answer: a {@link FlexibleDate}, {@link #NOW} when it is already the case, or
 * {@link #NOT_DISCLOSED} when the provider does not give the date. An answer date that is absent
 * means not disclosed. It never means now.
 */
public final class AnswerDate {

    /** Already the case. */
    public static final AnswerDate NOW = new AnswerDate("NOW", null);
    /** The provider does not give the date through the API. */
    public static final AnswerDate NOT_DISCLOSED = new AnswerDate("NOT_DISCLOSED", null);

    private final String text;
    private final FlexibleDate date;

    private AnswerDate(String text, FlexibleDate date) {
        this.text = text;
        this.date = date;
    }

    /**
     * Returns an answer date for a date.
     *
     * @param date the date
     * @return the answer date
     */
    public static AnswerDate of(FlexibleDate date) {
        Objects.requireNonNull(date, "date");
        return new AnswerDate(date.toString(), date);
    }

    /**
     * Reads the wire form. A value that is neither a date nor {@code NOW} reads as
     * {@link #NOT_DISCLOSED}.
     *
     * @param text the wire form, or {@code null}
     * @return the answer date
     */
    @JsonCreator
    public static AnswerDate parse(String text) {
        if ("NOW".equals(text)) {
            return NOW;
        }
        if (text == null || "NOT_DISCLOSED".equals(text)) {
            return NOT_DISCLOSED;
        }
        try {
            return of(FlexibleDate.parse(text));
        } catch (IllegalArgumentException unreadable) {
            return NOT_DISCLOSED;
        }
    }

    /**
     * Says whether the value is {@link #NOW}.
     *
     * @return {@code true} for now
     */
    public boolean isNow() {
        return this == NOW;
    }

    /**
     * Says whether the provider gave a date or {@link #NOW}.
     *
     * @return {@code false} for {@link #NOT_DISCLOSED}
     */
    public boolean isDisclosed() {
        return this != NOT_DISCLOSED;
    }

    /**
     * Returns the date.
     *
     * @return the date, or empty for {@link #NOW} and {@link #NOT_DISCLOSED}
     */
    public Optional<FlexibleDate> date() {
        return Optional.ofNullable(date);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AnswerDate answerDate && text.equals(answerDate.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    /**
     * Returns the wire form.
     *
     * @return the date as written, {@code NOW}, or {@code NOT_DISCLOSED}
     */
    @JsonValue
    @Override
    public String toString() {
        return text;
    }
}
