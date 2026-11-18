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
package com.eqixiac.equinix.design.connectivity;

import com.eqixiac.equinix.Equinix;
import com.eqixiac.equinix.internetaccess.enums.ConnectionType;
import com.eqixiac.equinix.internetaccess.model.Ibx;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Where Network Edge and Internet Access are offered, as metro codes.
 *
 * @param networkEdgeMetros    metros that offer Network Edge; empty when unknown
 * @param internetAccessMetros metros that offer Internet Access; empty when unknown
 * @param notes                what could not be determined, for the report
 */
public record ProductAvailability(Set<String> networkEdgeMetros, Set<String> internetAccessMetros,
                                  List<String> notes) {

    /** Makes the collections immutable and the codes upper case. */
    public ProductAvailability {
        networkEdgeMetros = upper(networkEdgeMetros);
        internetAccessMetros = upper(internetAccessMetros);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /**
     * @param networkEdgeMetros    metros that offer Network Edge
     * @param internetAccessMetros metros that offer Internet Access
     * @return a fixed availability, for tests and offline runs
     */
    public static ProductAvailability of(Set<String> networkEdgeMetros, Set<String> internetAccessMetros) {
        return new ProductAvailability(networkEdgeMetros, internetAccessMetros, List.of());
    }

    /** @return an availability that knows nothing */
    public static ProductAvailability unknown() {
        return new ProductAvailability(Set.of(), Set.of(), List.of("product availability was not read"));
    }

    /**
     * Reads availability from the live APIs with two GET requests: the Network Edge metro list and the
     * Internet Access availability for virtual connections. A lookup that fails leaves its set empty and
     * adds a note; it does not throw.
     *
     * @param session an open session
     * @return the availability
     */
    public static ProductAvailability read(Equinix session) {
        Set<String> networkEdge = new TreeSet<>();
        Set<String> internetAccess = new TreeSet<>();
        List<String> notes = new ArrayList<>();
        try {
            for (com.eqixiac.equinix.networkedge.model.Metro metro : session.networkEdge().setup().listMetros()) {
                if (metro.getMetroCode() != null) {
                    networkEdge.add(metro.getMetroCode().name());
                }
            }
        } catch (RuntimeException e) {
            notes.add("Network Edge metros could not be read: " + e.getClass().getSimpleName());
        }
        try {
            for (Ibx ibx : session.internetAccess().ibxs().availability(ConnectionType.IA_VC)) {
                if (ibx.getMetroCode() != null) {
                    internetAccess.add(ibx.getMetroCode());
                }
            }
        } catch (RuntimeException e) {
            notes.add("Internet Access availability could not be read: " + e.getClass().getSimpleName());
        }
        return new ProductAvailability(networkEdge, internetAccess, notes);
    }

    private static Set<String> upper(Set<String> codes) {
        Set<String> out = new TreeSet<>();
        if (codes != null) {
            for (String code : codes) {
                if (code != null && !code.isBlank()) {
                    out.add(code.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        return java.util.Collections.unmodifiableSet(out);
    }
}
