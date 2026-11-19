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

package org.opencapacity.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The synthetic inventory of the {@link ReferenceProvider}, and the rules that grade a requirement
 * against it. Every metro, campus, facility, party, and number here is invented for testing. None
 * describes a real provider or a real site.
 */
public final class Inventory {

    /** The power factor the reference provider uses to convert kVA to kW. */
    public static final double POWER_FACTOR = 0.95;

    private static final List<String> REDUNDANCY = List.of("N", "N_PLUS_1", "N_PLUS_2", "TWO_N", "TWO_N_PLUS_1");

    private record Metro(String code, String name, String country, String standardCode, double latitude,
            double longitude, Map<Long, Boolean> parties) {
    }

    private static final class Facility {
        final String code;
        final String name;
        final String metro;
        final String campus;
        double availableKw;
        final double plannedKw;
        final String plannedDate;
        final double maxDensityKw;
        final Set<String> features;
        final String redundancy;
        final boolean dc;

        Facility(String code, String name, String metro, String campus, double availableKw, double plannedKw,
                String plannedDate, double maxDensityKw, Set<String> features, String redundancy, boolean dc) {
            this.code = code;
            this.name = name;
            this.metro = metro;
            this.campus = campus;
            this.availableKw = availableKw;
            this.plannedKw = plannedKw;
            this.plannedDate = plannedDate;
            this.maxDensityKw = maxDensityKw;
            this.features = features;
            this.redundancy = redundancy;
            this.dc = dc;
        }
    }

    private final Map<String, Metro> metros = new LinkedHashMap<>();
    private final Map<String, Facility> facilities = new LinkedHashMap<>();

    private Inventory() {
    }

    /**
     * Returns the example inventory: three invented metros with four invented facilities.
     *
     * @return a new inventory
     */
    public static Inventory example() {
        Inventory inventory = new Inventory();
        inventory.metros.put("FR", new Metro("FR", "Frankfurt", "DE", "DEFRA", 50.11, 8.68, Map.of(64500L, true, 64501L, false)));
        inventory.metros.put("AM", new Metro("AM", "Amsterdam", "NL", "NLAMS", 52.37, 4.90, Map.of(64500L, true)));
        inventory.metros.put("LD", new Metro("LD", "London", "GB", "GBLON", 51.51, -0.13, Map.of()));
        inventory.add(new Facility("FR-A", "Frankfurt A", "FR", "FR-CAMPUS-1", 1200, 4000, "2027-Q3", 80,
                Set.of("LIQUID_COOLING", "DIRECT_TO_CHIP_COOLING", "PRIVATE_CAGE"), "TWO_N", false));
        inventory.add(new Facility("FR-B", "Frankfurt B", "FR", "FR-CAMPUS-1", 300, 0, null, 15,
                Set.of("PRIVATE_CAGE", "PRIVATE_SUITE"), "N_PLUS_1", false));
        inventory.add(new Facility("AM-A", "Amsterdam A", "AM", null, 600, 2000, "2028-Q1", 40,
                Set.of("LIQUID_COOLING", "REAR_DOOR_HEAT_EXCHANGER", "PRIVATE_CAGE"), "TWO_N", false));
        inventory.add(new Facility("LD-A", "London A", "LD", null, 150, 800, "2027-Q1", 20,
                Set.of("PRIVATE_CAGE"), "N_PLUS_1", false));
        return inventory;
    }

    private void add(Facility facility) {
        facilities.put(facility.code, facility);
    }

    /**
     * Changes the power that a facility has available today. Call
     * {@link ReferenceProvider#reevaluate()} afterwards to send events for the change.
     *
     * @param facility the facility code, for example {@code FR-A}
     * @param kw the available power in kW
     */
    public synchronized void setAvailableKw(String facility, double kw) {
        facilities.get(facility).availableKw = kw;
    }

    // ------------------------------------------------------------------ locations

    synchronized List<ObjectNode> locations(ObjectMapper json, String level, String within) {
        List<ObjectNode> all = new ArrayList<>();
        all.add(json.createObjectNode().put("level", "REGION").put("code", "EMEA").put("name", "Europe, Middle East and Africa"));
        Set<String> countries = new java.util.LinkedHashSet<>();
        for (Metro metro : metros.values()) {
            if (countries.add(metro.country())) {
                ObjectNode country = json.createObjectNode().put("level", "COUNTRY").put("code", metro.country())
                        .put("name", metro.country());
                country.putObject("parent").put("level", "REGION").put("code", "EMEA");
                all.add(country);
            }
        }
        for (Metro metro : metros.values()) {
            ObjectNode node = json.createObjectNode().put("level", "METRO").put("code", metro.code())
                    .put("name", metro.name()).put("standardCode", metro.standardCode());
            node.putObject("parent").put("level", "COUNTRY").put("code", metro.country());
            all.add(node);
        }
        Set<String> campuses = new java.util.LinkedHashSet<>();
        for (Facility facility : facilities.values()) {
            if (facility.campus != null && campuses.add(facility.campus)) {
                ObjectNode campus = json.createObjectNode().put("level", "CAMPUS").put("code", facility.campus)
                        .put("name", facility.campus);
                campus.putObject("parent").put("level", "METRO").put("code", facility.metro);
                all.add(campus);
            }
        }
        for (Facility facility : facilities.values()) {
            ObjectNode node = json.createObjectNode().put("level", "FACILITY").put("code", facility.code)
                    .put("name", facility.name);
            node.putObject("parent").put("level", facility.campus != null ? "CAMPUS" : "METRO")
                    .put("code", facility.campus != null ? facility.campus : facility.metro);
            all.add(node);
        }
        all.removeIf(node -> level != null && !level.equals(node.path("level").asText()));
        all.removeIf(node -> within != null && !within.equals(node.path("parent").path("code").asText()));
        return all;
    }

    // ------------------------------------------------------------------ grading

    static double kilo(JsonNode power) {
        double value = power.path("value").asDouble();
        String unit = power.path("unit").asText();
        double thousand = "MW".equals(unit) || "MVA".equals(unit) ? value * 1000 : value;
        return unit.endsWith("VA") ? thousand * POWER_FACTOR : thousand;
    }

    static LocalDate lastDay(String date) {
        if (date.contains("Q")) {
            int quarter = date.charAt(date.length() - 1) - '0';
            return YearMonth.of(Integer.parseInt(date.substring(0, 4)), quarter * 3).atEndOfMonth();
        }
        return switch (date.length()) {
            case 4 -> LocalDate.of(Integer.parseInt(date), 12, 31);
            case 7 -> YearMonth.parse(date).atEndOfMonth();
            default -> LocalDate.parse(date);
        };
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    synchronized List<ObjectNode> answer(ObjectMapper json, JsonNode location, JsonNode requirement, LocalDate today,
            ReferenceProvider.Disclosure disclosure, double reviewThresholdKw, Set<String> recognized) {
        String level = location.path("level").asText();
        String code = location.path("code").asText();
        List<ObjectNode> results = new ArrayList<>();
        if ("AREA".equals(level)) {
            for (Metro metro : metros.values()) {
                double distance = distanceKm(location.path("center").path("latitude").asDouble(),
                        location.path("center").path("longitude").asDouble(), metro.latitude(), metro.longitude());
                if (distance <= location.path("radiusKm").asDouble()) {
                    results.add(grade(json, metroRef(json, metro), location, inMetro(metro.code()), requirement, today,
                            disclosure, reviewThresholdKw, recognized));
                }
            }
        } else if ("REGION".equals(level) || "COUNTRY".equals(level)) {
            List<Facility> group = new ArrayList<>();
            for (Facility facility : facilities.values()) {
                if ("REGION".equals(level) ? "EMEA".equals(code) : metros.get(facility.metro).country().equals(code)) {
                    group.add(facility);
                }
            }
            if (!group.isEmpty()) {
                results.add(grade(json, location, null, group, requirement, today, disclosure, reviewThresholdKw, recognized));
            }
        } else if ("METRO".equals(level)) {
            boolean standard = "STANDARD".equals(location.path("scheme").asText());
            for (Metro metro : metros.values()) {
                if (code.equals(standard ? metro.standardCode() : metro.code())) {
                    results.add(grade(json, location, null, inMetro(metro.code()), requirement, today, disclosure,
                            reviewThresholdKw, recognized));
                }
            }
        } else {
            List<Facility> group = new ArrayList<>();
            for (Facility facility : facilities.values()) {
                if (code.equals("CAMPUS".equals(level) ? facility.campus : facility.code)) {
                    group.add(facility);
                }
            }
            if (!group.isEmpty()) {
                results.add(grade(json, metroRef(json, metros.get(group.get(0).metro)), location, group, requirement,
                        today, disclosure, reviewThresholdKw, recognized));
            }
        }
        if (results.isEmpty()) {
            ObjectNode notServed = json.createObjectNode();
            notServed.set("location", location);
            notServed.put("grade", "UNAVAILABLE");
            ArrayNode features = notServed.putArray("supportedFeatures");
            for (JsonNode desired : requirement.path("desiredFeatures")) {
                features.addObject().put("feature", desired.path("feature").asText()).put("support", "UNAVAILABLE")
                        .put("required", desired.path("required").asBoolean());
            }
            notServed.putArray("reasons").add("LOCATION_NOT_SERVED");
            results.add(notServed);
        }
        return results;
    }

    private static ObjectNode metroRef(ObjectMapper json, Metro metro) {
        return json.createObjectNode().put("level", "METRO").put("code", metro.code());
    }

    private List<Facility> inMetro(String metro) {
        return facilities.values().stream().filter(facility -> facility.metro.equals(metro)).toList();
    }

    private ObjectNode grade(ObjectMapper json, JsonNode answered, JsonNode requested, List<Facility> group,
            JsonNode requirement, LocalDate today, ReferenceProvider.Disclosure disclosure, double reviewThresholdKw,
            Set<String> recognized) {
        JsonNode demand = requirement.path("demand");
        JsonNode density = demand.path("density");
        Double perCabinetKw = null;
        if (!density.isMissingNode()) {
            double representative = density.has("target") ? density.get("target").asDouble()
                    : density.has("max") ? density.get("max").asDouble() : density.get("min").asDouble();
            perCabinetKw = "KVA".equals(density.path("unit").asText()) ? representative * POWER_FACTOR : representative;
        }
        int count = demand.path("cabinets").path("count").asInt(0);
        double demandKw = demand.has("power") ? kilo(demand.get("power")) : count * perCabinetKw;
        JsonNode timeframe = requirement.path("timeframe");
        LocalDate needed = timeframe.has("neededForService") ? lastDay(timeframe.get("neededForService").asText()) : today;
        LocalDate latest = timeframe.has("latestAcceptable") ? lastDay(timeframe.get("latestAcceptable").asText()) : needed;

        List<String> reasons = new ArrayList<>();
        boolean unrecognizedRequired = false;
        List<String> requiredFeatures = new ArrayList<>();
        for (JsonNode desired : requirement.path("desiredFeatures")) {
            if (desired.path("required").asBoolean()) {
                String feature = desired.path("feature").asText();
                requiredFeatures.add(feature);
                unrecognizedRequired |= !recognized.contains(feature);
            }
        }
        String wantedRedundancy = requirement.path("redundancy").path("power").asText(null);
        boolean wantsDc = "DC".equals(requirement.path("powerFeed").path("current").asText());

        List<Facility> eligible = new ArrayList<>(group);
        final Double perCabinet = perCabinetKw;
        if (eligible.removeIf(facility -> !facility.features.containsAll(requiredFeatures)) && eligible.isEmpty()) {
            reasons.add("FEATURE");
        }
        if (perCabinet != null && !eligible.isEmpty()
                && eligible.removeIf(facility -> facility.maxDensityKw < perCabinet) && eligible.isEmpty()) {
            reasons.add("DENSITY");
        }
        if (wantedRedundancy != null && !eligible.isEmpty()
                && eligible.removeIf(facility -> REDUNDANCY.indexOf(facility.redundancy) < REDUNDANCY.indexOf(wantedRedundancy))
                && eligible.isEmpty()) {
            reasons.add("REDUNDANCY");
        }
        if (wantsDc && !eligible.isEmpty() && eligible.removeIf(facility -> !facility.dc) && eligible.isEmpty()) {
            reasons.add("POWER_FEED");
        }

        double nowKw = eligible.stream().mapToDouble(facility -> facility.availableKw).sum();
        double plannedInTime = 0;
        double plannedAll = 0;
        String plannedDate = null;
        for (Facility facility : eligible) {
            if (facility.plannedKw > 0) {
                plannedAll += facility.plannedKw;
                plannedDate = facility.plannedDate;
                if (!lastDay(facility.plannedDate).isAfter(latest)) {
                    plannedInTime += facility.plannedKw;
                }
            }
        }

        String grade;
        String status = null;
        String service = null;
        Integer partialMax = null;
        if (unrecognizedRequired) {
            grade = "NOT_DISCLOSED";
        } else if (eligible.isEmpty()) {
            grade = "UNAVAILABLE";
        } else if (demandKw > reviewThresholdKw) {
            grade = nowKw + plannedAll > 0 ? "POSSIBLE" : "UNAVAILABLE";
            reasons.add(nowKw + plannedAll > 0 ? "REVIEW_REQUIRED" : "POWER");
        } else if (nowKw >= demandKw) {
            grade = "AVAILABLE";
            status = "IN_SERVICE";
            service = "NOW";
        } else if (nowKw + plannedInTime >= demandKw) {
            grade = "AVAILABLE";
            status = "PLANNED";
            service = plannedDate;
        } else if (nowKw + plannedAll >= demandKw) {
            grade = "POSSIBLE";
            status = "PLANNED";
            service = plannedDate;
            reasons.add("TIMING");
        } else if (demand.path("cabinets").has("minimum") && perCabinet != null
                && nowKw + plannedInTime >= demand.path("cabinets").path("minimum").asInt() * perCabinet) {
            grade = "POSSIBLE";
            status = nowKw >= demand.path("cabinets").path("minimum").asInt() * perCabinet ? "IN_SERVICE" : "PLANNED";
            partialMax = (int) Math.floor((nowKw + plannedInTime) / perCabinet);
            reasons.add("SPACE");
        } else {
            grade = "UNAVAILABLE";
            reasons.add("POWER");
        }

        ArrayNode interconnection = json.createArrayNode();
        for (JsonNode wanted : requirement.path("interconnection")) {
            long asn = wanted.path("party").path("asn").asLong(-1);
            String presence = "UNAVAILABLE";
            for (Facility facility : group) {
                Boolean listed = metros.get(facility.metro).parties().get(asn);
                if (Boolean.TRUE.equals(listed)) {
                    presence = "AVAILABLE";
                } else if (listed != null && !"AVAILABLE".equals(presence)) {
                    presence = "NOT_DISCLOSED";
                }
            }
            boolean required = wanted.path("required").asBoolean();
            if (required && "UNAVAILABLE".equals(presence) && !"NOT_DISCLOSED".equals(grade)) {
                grade = "UNAVAILABLE";
                reasons.add("INTERCONNECTION");
            }
            ObjectNode entry = interconnection.addObject();
            entry.set("party", wanted.path("party"));
            entry.put("within", wanted.path("within").asText("METRO")).put("presence", presence).put("required", required);
            if (disclosure == ReferenceProvider.Disclosure.FULL && "AVAILABLE".equals(presence)) {
                entry.putObject("roundTripMs").put("min", 0.3).put("max", 1.5);
            }
        }

        boolean positive = "AVAILABLE".equals(grade) || "POSSIBLE".equals(grade);
        ObjectNode result = json.createObjectNode();
        result.set("location", answered);
        if (requested != null) {
            result.set("requested", requested);
        }
        result.put("grade", grade);
        boolean banded = disclosure != ReferenceProvider.Disclosure.GRADE_ONLY;
        if (banded && positive && status != null) {
            ObjectNode timing = result.putObject("timing").put("status", status);
            timing.put("availableForSale", "NOW");
            if (service != null) {
                timing.put("availableForService", service);
            }
        }
        if (banded && positive) {
            result.putObject("density")
                    .put("max", eligible.stream().mapToDouble(facility -> facility.maxDensityKw).max().orElse(0))
                    .put("unit", "KW");
            if (partialMax != null) {
                result.putObject("cabinets").put("min", demand.path("cabinets").path("minimum").asInt()).put("max", partialMax);
            } else if (count > 0 && "AVAILABLE".equals(grade)) {
                result.putObject("cabinets").put("min", count);
            }
        }
        if (disclosure == ReferenceProvider.Disclosure.FULL && "AVAILABLE".equals(grade)) {
            result.putObject("power").put("min", demandKw).put("unit", "KW");
        }
        ArrayNode features = result.putArray("supportedFeatures");
        for (JsonNode desired : requirement.path("desiredFeatures")) {
            String feature = desired.path("feature").asText();
            boolean required = desired.path("required").asBoolean();
            String support;
            if (!recognized.contains(feature)) {
                support = "NOT_DISCLOSED";
            } else if (required && positive) {
                support = "AVAILABLE";
            } else if (!required && !banded) {
                support = "NOT_DISCLOSED";
            } else {
                List<Facility> pool = required || eligible.isEmpty() ? group : eligible;
                support = pool.stream().anyMatch(facility -> facility.features.contains(feature)) ? "AVAILABLE" : "UNAVAILABLE";
            }
            features.addObject().put("feature", feature).put("support", support).put("required", required);
        }
        if (banded && !interconnection.isEmpty()) {
            result.set("interconnection", interconnection);
        }
        if (disclosure == ReferenceProvider.Disclosure.FULL && !reasons.isEmpty() && !"AVAILABLE".equals(grade)) {
            ArrayNode list = result.putArray("reasons");
            reasons.stream().distinct().forEach(list::add);
            result.put("nextStepUrl", "https://provider.example/contact");
        }
        return result;
    }
}
