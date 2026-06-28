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

package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.model.Service;
import api.equinix.javasdk.internetaccess.client.InternetAccessConfig;
import api.equinix.javasdk.internetaccess.client.InternetAccessIbxs;
import api.equinix.javasdk.internetaccess.client.InternetAccessPrices;
import api.equinix.javasdk.internetaccess.client.InternetAccessServices;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessIbxsImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessPricesImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessServicesImpl;

/**
 * The primary entry point for accessing the Equinix Internet Access (EIA) v2 API.
 *
 * <p>Equinix Internet Access provides managed internet connectivity services through Equinix
 * data centers. The {@link #services()} accessor exposes the full EIA v2 service lifecycle
 * (create / get / update / delete / search), the {@link #ibxs()} accessor exposes the EIA v2
 * product-availability lookup ({@code GET /internetAccess/v2/ibxs}), and the {@link #prices()}
 * accessor exposes the EIA v1 price search ({@code POST /internetAccess/v1/prices/search}).</p>
 *
 * <p>Each accessor uses lazy initialization — the internal client is created on first access and
 * reused for subsequent calls.</p>
 *
 * <h3>Scope (v1 selective)</h3>
 * <p>This domain intentionally surfaces only the EIA v2 service lifecycle plus a selective subset of
 * the EIA v1 read API — the {@link #ibxs() IBX} product-availability lookup and the
 * {@link #prices() price search}. The remaining EIA v1 read/lookup operations are deliberately
 * out of scope and are <strong>not</strong> exposed by the SDK, namely: accounts and account
 * agreements ({@code GET /internetAccess/v1/accounts}, {@code .../{accountNumber}},
 * {@code .../agreements}); terms and conditions ({@code GET /internetAccess/v1/terms}); operational
 * units and signature policies ({@code GET /internetAccess/v1/operationalUnits},
 * {@code .../signaturePolicies}); the default/product-configuration lookups
 * ({@code routingProtocolConfigurations}, dedicated/virtual bandwidth configurations, virtual
 * connection default configurations, customer route configurations, dedicated port default
 * configurations, port configurations); purchase orders
 * ({@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders}, {@code .../{number}});
 * the product-availability inventory ({@code cages}, {@code cabinets}, {@code patchPanels},
 * {@code connectionServices}); and order history ({@code GET /internetAccess/v1/orders/{orderUUID}}).
 * A single-IBX get ({@code GET /internetAccess/v1/ibxs/{ibx}}) is likewise not exposed; use the v2
 * {@link #ibxs() IBX list} instead.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * InternetAccess internetAccess = new InternetAccess(credentials);
 *
 * InternetAccessService service = internetAccess.services().define()
 *     .name("WebServers")
 *     .type(ServiceTypeV2.SINGLE)
 *     .connection("9b8c5042-b553-4d5e-a2ac-c73bf6d4fd81")
 *     .routingProtocol(BgpRoutingProtocolRequest.builder()
 *         .customerAsn(16220L)
 *         .exportPolicy(ExportPolicy.FULL)
 *         .build())
 *     .create();
 * }</pre>
 *
 * @author ianjones
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 */
public final class InternetAccess extends EquinixClient implements Service {

    private InternetAccessServices services;

    private InternetAccessIbxs ibxs;

    private InternetAccessPrices prices;

    final private InternetAccessConfig internetAccessConfig;

    /**
     * Creates a new Internet Access client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public InternetAccess(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * Creates a new Internet Access client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public InternetAccess(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_InternetAccess.json";
        equinixClient.appendApiParams(paramFile);

        this.internetAccessConfig = new InternetAccessConfigImpl(equinixClient);
    }

    /**
     * Returns the client for managing the Equinix Internet Access v2 service lifecycle
     * (create / get / update / delete / search).
     *
     * @return the {@link InternetAccessServices} client
     */
    public InternetAccessServices services() {
        if (this.services == null) {
            this.services = new InternetAccessServicesImpl(this.internetAccessConfig.getInternetAccessServiceClient(), this);
        }
        return services;
    }

    /**
     * Returns the client for the Equinix Internet Access v2 product-availability lookup —
     * the IBXs where EIA is available ({@code GET /internetAccess/v2/ibxs}).
     *
     * @return the {@link InternetAccessIbxs} client
     */
    public InternetAccessIbxs ibxs() {
        if (this.ibxs == null) {
            this.ibxs = new InternetAccessIbxsImpl(this.internetAccessConfig.getIbxClient(), this);
        }
        return ibxs;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 price search
     * ({@code POST /internetAccess/v1/prices/search}).
     *
     * @return the {@link InternetAccessPrices} client
     */
    public InternetAccessPrices prices() {
        if (this.prices == null) {
            this.prices = new InternetAccessPricesImpl(this.internetAccessConfig.getPriceClient(), this);
        }
        return prices;
    }
}
