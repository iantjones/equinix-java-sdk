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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eqixiac.opencapacity.model.AnswerDate;
import com.eqixiac.opencapacity.model.CapacityAnswer;
import com.eqixiac.opencapacity.model.CapacityRequirement;
import com.eqixiac.opencapacity.model.DatePrecision;
import com.eqixiac.opencapacity.model.DensityRange;
import com.eqixiac.opencapacity.model.Event;
import com.eqixiac.opencapacity.model.Feature;
import com.eqixiac.opencapacity.model.FlexibleDate;
import com.eqixiac.opencapacity.model.GeoPoint;
import com.eqixiac.opencapacity.model.Grade;
import com.eqixiac.opencapacity.model.Location;
import com.eqixiac.opencapacity.model.LocationLevel;
import com.eqixiac.opencapacity.model.Offering;
import com.eqixiac.opencapacity.model.Party;
import com.eqixiac.opencapacity.model.Power;
import com.eqixiac.opencapacity.model.PowerBasis;
import com.eqixiac.opencapacity.model.SignalState;
import com.eqixiac.opencapacity.model.SpaceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Unit tests for the value types of the OpenCapacity client. They need no provider. */
class ModelTest {

    @Test
    void flexibleDateReadsAndWritesTheFourForms() {
        FlexibleDate day = FlexibleDate.parse("2027-06-15");
        FlexibleDate month = FlexibleDate.parse("2027-06");
        FlexibleDate quarter = FlexibleDate.parse("2027-Q2");
        FlexibleDate year = FlexibleDate.parse("2027");
        assertEquals(DatePrecision.DAY, day.precision());
        assertEquals(DatePrecision.MONTH, month.precision());
        assertEquals(DatePrecision.QUARTER, quarter.precision());
        assertEquals(DatePrecision.YEAR, year.precision());
        assertEquals("2027-06-15", day.toString());
        assertEquals("2027-06", month.toString());
        assertEquals("2027-Q2", quarter.toString());
        assertEquals("2027", year.toString());
        assertEquals(LocalDate.of(2027, 4, 1), quarter.firstDay());
        assertEquals(LocalDate.of(2027, 6, 30), quarter.lastDay());
        assertEquals(LocalDate.of(2027, 12, 31), year.lastDay());
        assertEquals(FlexibleDate.quarter(2027, 2), quarter);
        assertEquals(FlexibleDate.quarter(2027, 2).hashCode(), quarter.hashCode());
        assertNotEquals(month, quarter, "a month and a quarter that end on the same day are different values");
        assertEquals(0, month.compareTo(quarter), "and compare as equal, by their last day");
        assertTrue(day.compareTo(year) < 0);
        assertEquals(FlexibleDate.day(LocalDate.of(2027, 6, 15)), day);
    }

    @Test
    void flexibleDateRejectsOtherForms() {
        for (String text : new String[] {"2027-Q5", "2027-13", "06/2027", "Q2-2027", "27", "2027-06-Q1", ""}) {
            assertThrows(IllegalArgumentException.class, () -> FlexibleDate.parse(text), text);
        }
        assertThrows(IllegalArgumentException.class, () -> FlexibleDate.quarter(2027, 0));
    }

    @Test
    void answerDateDistinguishesNowNotDisclosedAndADate() {
        assertTrue(AnswerDate.parse("NOW").isNow());
        assertTrue(AnswerDate.NOW.isDisclosed());
        assertFalse(AnswerDate.parse("NOT_DISCLOSED").isDisclosed());
        assertFalse(AnswerDate.parse(null).isDisclosed());
        assertFalse(AnswerDate.parse("SOON").isDisclosed(), "an unknown value reads as not disclosed");
        AnswerDate date = AnswerDate.parse("2027-Q3");
        assertEquals(FlexibleDate.quarter(2027, 3), date.date().orElseThrow());
        assertFalse(date.isNow());
        assertTrue(AnswerDate.NOW.date().isEmpty());
        assertEquals("2027-Q3", date.toString());
        assertEquals(AnswerDate.of(FlexibleDate.quarter(2027, 3)), date);
        assertEquals(date.hashCode(), AnswerDate.of(FlexibleDate.quarter(2027, 3)).hashCode());
    }

    @Test
    void densityRangeAcceptsAnySubsetAndRejectsDisorder() {
        assertEquals(8.0, DensityRange.kva().min(5).target(8).max(100).create().representative());
        assertEquals(100.0, DensityRange.kva().min(5).max(100).create().representative());
        assertEquals(5.0, DensityRange.kw().min(5).create().representative());
        assertThrows(IllegalArgumentException.class, () -> DensityRange.kw().create());
        assertThrows(IllegalArgumentException.class, () -> DensityRange.kw().min(10).max(5).create());
        assertThrows(IllegalArgumentException.class, () -> DensityRange.kw().target(0).create());
    }

    @Test
    void requirementBuilderSaysWhatIsMissing() {
        IllegalStateException noLocation = assertThrows(IllegalStateException.class,
                () -> CapacityRequirement.define().power(Power.mw(1)).create());
        assertTrue(noLocation.getMessage().contains("addLocation"), noLocation.getMessage());
        CapacityRequirement.Builder builder = CapacityRequirement.define().addLocation(Location.metro("FR"));
        assertThrows(IllegalStateException.class, builder::create);
        assertThrows(IllegalStateException.class, () -> builder.cabinets(4).create(), "cabinets need a density");
        builder.density(DensityRange.kw().min(10).max(20).create());
        assertThrows(IllegalStateException.class, () -> builder.power(Power.kw(100)).create(), "above count times max");
        assertThrows(IllegalStateException.class, () -> builder.power(Power.kw(30)).create(), "below count times min");
        CapacityRequirement fits = builder.power(Power.mw(0.06)).powerBasis(PowerBasis.CIRCUIT_CAPACITY).powerFactor(0.9)
                .spaceType(SpaceType.CAGE).offering(Offering.RETAIL_COLOCATION)
                .addRampStep(FlexibleDate.year(2027), 100).create();
        assertEquals(60.0, fits.demand().power().inKilo());
        assertEquals(1, fits.timeframe().ramp().size());
        assertNull(CapacityRequirement.define().addLocation(Location.metro("FR")).power(Power.kw(1)).create().timeframe(),
                "no dates means an immediate need and no timeframe on the wire");
        assertEquals(fits, fits.toBuilder().create());
    }

    @Test
    void valueTypesValidateTheirArguments() {
        assertThrows(IllegalArgumentException.class, () -> Feature.of("liquid cooling"));
        assertEquals("X_EXAMPLE", Feature.of("X_EXAMPLE").toString());
        assertThrows(IllegalArgumentException.class, () -> Power.kw(0));
        assertThrows(IllegalArgumentException.class, () -> new Party(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(91, 0));
        assertThrows(IllegalArgumentException.class, () -> Location.within(50, 8, 1001));
        assertThrows(IllegalArgumentException.class, () -> Location.metro("F"));
        assertFalse(Location.metro("FR").isArea());
        assertTrue(Party.asn(64500).matches(new Party("Example Cloud", 64500L, null)));
        assertTrue(Party.named("example cloud").matches(Party.named("Example Cloud")));
        assertFalse(Party.asn(64500).matches(null));
    }

    @Test
    void unknownEnumerationValuesAreTolerated() throws JsonProcessingException {
        assertEquals(Grade.NOT_DISCLOSED, Grade.fromValue("LIKELY"));
        assertEquals(Grade.NOT_DISCLOSED, Grade.fromValue(null));
        assertTrue(Grade.POSSIBLE.isPositive());
        assertFalse(Grade.UNAVAILABLE.isPositive());
        assertEquals(SignalState.UNKNOWN, SignalState.fromValue("ARCHIVED"));
        assertEquals(LocationLevel.UNKNOWN, LocationLevel.fromValue(null));
        CapacityAnswer answer = Transport.MAPPER.readValue(
                "{\"answerId\":\"a\",\"grade\":\"LIKELY\",\"newField\":1,\"validUntil\":\"2026-11-19T10:00:00Z\","
                        + "\"results\":[{\"location\":{\"level\":\"METRO\",\"code\":\"FR\"},\"reasons\":[\"WEATHER\"]}]}",
                CapacityAnswer.class);
        assertEquals(Grade.NOT_DISCLOSED, answer.grade());
        assertEquals(Grade.NOT_DISCLOSED, answer.results().get(0).grade(), "a missing grade reads as not disclosed");
        assertTrue(answer.results().get(0).supportedFeatures().isEmpty());
        assertTrue(answer.isExpired(Instant.parse("2026-11-20T00:00:00Z")));
    }

    @Test
    void eventsRejectTextThatIsNotAnEvent() {
        assertThrows(OpenCapacityException.class, () -> Events.parse("not json"));
        assertThrows(OpenCapacityException.class, () -> Events.parse("{\"id\":\"x\"}"));
        Event answer = Events.parse("{\"type\":\"" + Event.ANSWER_UPDATED + "\"}");
        assertEquals(0, answer.sequence());
        assertThrows(IllegalArgumentException.class, () -> Events.challengeResponse(answer));
        Event verification = Events.parse("{\"type\":\"" + Event.SUBSCRIPTION_VERIFICATION
                + "\",\"data\":{\"subscriptionId\":\"s\",\"sequence\":1,\"challenge\":\"abc\"}}");
        assertEquals("{\"challenge\":\"abc\"}", Events.challengeResponse(verification));
    }
}
