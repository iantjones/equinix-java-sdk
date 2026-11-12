package com.eqixiac.equinix.design.cost;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Registered for every test class (ServiceLoader + {@code junit.jupiter.extensions.autodetection.enabled}): sets
 * {@value CurrencyConverters#OFFLINE_PROPERTY}{@code =true} so no test reaches the live FX API through a default
 * converter. Tests of the live converter inject their own HTTP fetch.
 */
public final class OfflineFxExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        System.setProperty(CurrencyConverters.OFFLINE_PROPERTY, "true");
    }
}
