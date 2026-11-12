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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A bundle of extra objectives and constraints contributed by a library on the classpath.
 * Register an implementation in {@code META-INF/services/com.eqixiac.equinix.design.spi.DesignPlugin}
 * and the engines pick it up through {@link #discover()}. A contributed objective takes part in
 * ranking only when the spec gives its id a positive weight; a contributed constraint always applies.
 */
public interface DesignPlugin {

    /** @return a short name for diagnostics */
    String name();

    /** @return objectives to add; never {@code null} */
    default List<Objective> objectives() {
        return List.of();
    }

    /** @return constraints to add; never {@code null} */
    default List<Constraint> constraints() {
        return List.of();
    }

    /**
     * A currency converter this plug-in contributes (for example a live FX feed). It ranks below rates
     * pinned in the spec's {@code commercial.exchangeRates} and above the SDK's bundled reference rates.
     *
     * @return the converter, or {@code null} for none
     */
    default CurrencyConverter currencyConverter() {
        return null;
    }

    /**
     * Loads every plug-in registered on the classpath.
     *
     * @return the plug-ins, in {@link ServiceLoader} order
     */
    static List<DesignPlugin> discover() {
        List<DesignPlugin> plugins = new ArrayList<>();
        for (DesignPlugin plugin : ServiceLoader.load(DesignPlugin.class)) {
            plugins.add(plugin);
        }
        return List.copyOf(plugins);
    }
}
