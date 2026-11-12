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

package com.eqixiac.equinix.fabric.beta.client.implementation;

import com.eqixiac.equinix.core.client.EquinixClient;
import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.fabric.beta.client.*;
import com.eqixiac.equinix.fabric.beta.client.internal.BetaClientFactory;

/**
 * Default {@link FabricBeta} hub: lazily creates one client per family over a shared
 * {@link BetaClientFactory}.
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 */
@Beta
public class FabricBetaImpl implements FabricBeta {

    private final BetaClientFactory clients;

    private volatile Asns asns;

    private volatile ExchangeServices exchangeServices;

    private volatile AppDomains appDomains;

    private volatile AppLinks appLinks;

    private volatile AppProcessors appProcessors;

    private volatile AppServices appServices;

    private volatile AppSubscriptions appSubscriptions;

    private volatile OpticalConnects opticalConnects;

    private volatile CrossConnects crossConnects;

    private volatile Loas loas;

    private volatile Tags tags;

    private volatile Logos logos;

    /**
     * Creates the hub over a core client whose catalogue already contains {@code json/apiParams_FabricBeta.json}.
     *
     * @param equinixClient the core client
     */
    public FabricBetaImpl(EquinixClient equinixClient) {
        this.clients = new BetaClientFactory(equinixClient);
    }

    @Override
    public Asns asns() {
        Asns c = this.asns;
        if (c == null) {
            c = new AsnsImpl(clients);
            this.asns = c;
        }
        return c;
    }

    @Override
    public ExchangeServices exchangeServices() {
        ExchangeServices c = this.exchangeServices;
        if (c == null) {
            c = new ExchangeServicesImpl(clients);
            this.exchangeServices = c;
        }
        return c;
    }

    @Override
    public AppDomains appDomains() {
        AppDomains c = this.appDomains;
        if (c == null) {
            c = new AppDomainsImpl(clients);
            this.appDomains = c;
        }
        return c;
    }

    @Override
    public AppLinks appLinks() {
        AppLinks c = this.appLinks;
        if (c == null) {
            c = new AppLinksImpl(clients);
            this.appLinks = c;
        }
        return c;
    }

    @Override
    public AppProcessors appProcessors() {
        AppProcessors c = this.appProcessors;
        if (c == null) {
            c = new AppProcessorsImpl(clients);
            this.appProcessors = c;
        }
        return c;
    }

    @Override
    public AppServices appServices() {
        AppServices c = this.appServices;
        if (c == null) {
            c = new AppServicesImpl(clients);
            this.appServices = c;
        }
        return c;
    }

    @Override
    public AppSubscriptions appSubscriptions() {
        AppSubscriptions c = this.appSubscriptions;
        if (c == null) {
            c = new AppSubscriptionsImpl(clients);
            this.appSubscriptions = c;
        }
        return c;
    }

    @Override
    public OpticalConnects opticalConnects() {
        OpticalConnects c = this.opticalConnects;
        if (c == null) {
            c = new OpticalConnectsImpl(clients);
            this.opticalConnects = c;
        }
        return c;
    }

    @Override
    public CrossConnects crossConnects() {
        CrossConnects c = this.crossConnects;
        if (c == null) {
            c = new CrossConnectsImpl(clients);
            this.crossConnects = c;
        }
        return c;
    }

    @Override
    public Loas loas() {
        Loas c = this.loas;
        if (c == null) {
            c = new LoasImpl(clients);
            this.loas = c;
        }
        return c;
    }

    @Override
    public Tags tags() {
        Tags c = this.tags;
        if (c == null) {
            c = new TagsImpl(clients);
            this.tags = c;
        }
        return c;
    }

    @Override
    public Logos logos() {
        Logos c = this.logos;
        if (c == null) {
            c = new LogosImpl(clients);
            this.logos = c;
        }
        return c;
    }
}
