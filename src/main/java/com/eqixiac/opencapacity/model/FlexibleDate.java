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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A point in time at the precision the sender chooses: a day, a month, a quarter, or a year. A
 * value coarser than a day is a period. Two values are compared by the last day of each.
 */
public final class FlexibleDate implements Comparable<FlexibleDate> {

    private static final Pattern FORMAT =
            Pattern.compile("([0-9]{4})(?:-(0[1-9]|1[0-2])(?:-(0[1-9]|[12][0-9]|3[01]))?|-Q([1-4]))?");

    private final DatePrecision precision;
    private final LocalDate first;
    private final LocalDate last;

    private FlexibleDate(DatePrecision precision, LocalDate first, LocalDate last) {
        this.precision = precision;
        this.first = first;
        this.last = last;
    }

    /**
     * Returns a single day.
     *
     * @param date the day
     * @return the date
     */
    public static FlexibleDate day(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return new FlexibleDate(DatePrecision.DAY, date, date);
    }

    /**
     * Returns a single day.
     *
     * @param year the year
     * @param month the month, 1 to 12
     * @param day the day of the month
     * @return the date
     */
    public static FlexibleDate day(int year, int month, int day) {
        return day(LocalDate.of(year, month, day));
    }

    /**
     * Returns a month.
     *
     * @param year the year
     * @param month the month, 1 to 12
     * @return the date
     */
    public static FlexibleDate month(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return new FlexibleDate(DatePrecision.MONTH, yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    /**
     * Returns a quarter.
     *
     * @param year the year
     * @param quarter the quarter, 1 to 4
     * @return the date
     */
    public static FlexibleDate quarter(int year, int quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("quarter must be 1 to 4: " + quarter);
        }
        YearMonth start = YearMonth.of(year, quarter * 3 - 2);
        return new FlexibleDate(DatePrecision.QUARTER, start.atDay(1), start.plusMonths(2).atEndOfMonth());
    }

    /**
     * Returns a year.
     *
     * @param year the year
     * @return the date
     */
    public static FlexibleDate year(int year) {
        return new FlexibleDate(DatePrecision.YEAR, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    /**
     * Reads the wire form: {@code 2027-06-15}, {@code 2027-06}, {@code 2027-Q2}, or {@code 2027}.
     *
     * @param text the wire form
     * @return the date
     * @throws IllegalArgumentException if the text is in none of the four forms
     */
    @JsonCreator
    public static FlexibleDate parse(String text) {
        Objects.requireNonNull(text, "text");
        Matcher matcher = FORMAT.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Expected a date as 2027-06-15, 2027-06, 2027-Q2 or 2027, but got: " + text);
        }
        int year = Integer.parseInt(matcher.group(1));
        if (matcher.group(4) != null) {
            return quarter(year, Integer.parseInt(matcher.group(4)));
        }
        if (matcher.group(3) != null) {
            return day(year, Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }
        if (matcher.group(2) != null) {
            return month(year, Integer.parseInt(matcher.group(2)));
        }
        return year(year);
    }

    /**
     * Returns the precision.
     *
     * @return day, month, quarter, or year
     */
    public DatePrecision precision() {
        return precision;
    }

    /**
     * Returns the first day of the period.
     *
     * @return the first day
     */
    public LocalDate firstDay() {
        return first;
    }

    /**
     * Returns the last day of the period, which is the day used for comparison.
     *
     * @return the last day
     */
    public LocalDate lastDay() {
        return last;
    }

    @Override
    public int compareTo(FlexibleDate other) {
        return last.compareTo(other.last);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FlexibleDate date && precision == date.precision && first.equals(date.first);
    }

    @Override
    public int hashCode() {
        return Objects.hash(precision, first);
    }

    /**
     * Returns the wire form.
     *
     * @return the date as the specification writes it
     */
    @JsonValue
    @Override
    public String toString() {
        return switch (precision) {
            case DAY -> first.toString();
            case MONTH -> String.format("%04d-%02d", first.getYear(), first.getMonthValue());
            case QUARTER -> String.format("%04d-Q%d", first.getYear(), (first.getMonthValue() + 2) / 3);
            case YEAR -> String.format("%04d", first.getYear());
        };
    }
}
