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

import com.eqixiac.equinix.core.model.MetroId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The set of metros one candidate design deploys in. Order carries no meaning: the metros are held
 * sorted by code without duplicates, so two portfolios with the same metros are equal.
 *
 * @param metros the metros, sorted by code; never empty
 */
public record MetroPortfolio(List<MetroId> metros) {

    /** Sorts, de-duplicates and rejects an empty or null-containing set. */
    public MetroPortfolio {
        if (metros == null || metros.isEmpty()) {
            throw new IllegalArgumentException("A portfolio needs at least one metro");
        }
        List<MetroId> sorted = new ArrayList<>(new LinkedHashSet<>(metros));
        if (sorted.contains(null)) {
            throw new IllegalArgumentException("A portfolio must not contain a null metro");
        }
        sorted.sort(Comparator.comparing(MetroId::code));
        metros = List.copyOf(sorted);
    }

    /**
     * @param metros the metros
     * @return a portfolio of them
     */
    public static MetroPortfolio of(Collection<MetroId> metros) {
        return new MetroPortfolio(metros == null ? null : new ArrayList<>(metros));
    }

    /**
     * @param codes metro codes, for example {@code "DC", "LD"}
     * @return a portfolio of them
     */
    public static MetroPortfolio ofCodes(String... codes) {
        List<MetroId> ids = new ArrayList<>();
        for (String code : codes) {
            ids.add(MetroId.of(code));
        }
        return new MetroPortfolio(ids);
    }

    /** @return how many metros the portfolio has */
    public int size() {
        return metros.size();
    }

    /**
     * @param metro a metro
     * @return whether the portfolio includes it
     */
    public boolean contains(MetroId metro) {
        return metros.contains(metro);
    }

    /** @return the codes joined with {@code +}, for example {@code DC+LD}: a stable, readable key */
    public String key() {
        return metros.stream().map(MetroId::code).collect(Collectors.joining("+"));
    }

    @Override
    public String toString() {
        return key();
    }
}
