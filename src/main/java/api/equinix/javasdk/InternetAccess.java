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
import api.equinix.javasdk.internetaccess.client.InternetAccessAccounts;
import api.equinix.javasdk.internetaccess.client.InternetAccessCabinets;
import api.equinix.javasdk.internetaccess.client.InternetAccessCages;
import api.equinix.javasdk.internetaccess.client.InternetAccessConfig;
import api.equinix.javasdk.internetaccess.client.InternetAccessConnectionServices;
import api.equinix.javasdk.internetaccess.client.InternetAccessIbxs;
import api.equinix.javasdk.internetaccess.client.InternetAccessOperationalUnits;
import api.equinix.javasdk.internetaccess.client.InternetAccessOrders;
import api.equinix.javasdk.internetaccess.client.InternetAccessPatchPanels;
import api.equinix.javasdk.internetaccess.client.InternetAccessPrices;
import api.equinix.javasdk.internetaccess.client.InternetAccessProductConfigurations;
import api.equinix.javasdk.internetaccess.client.InternetAccessPurchaseOrders;
import api.equinix.javasdk.internetaccess.client.InternetAccessServices;
import api.equinix.javasdk.internetaccess.client.InternetAccessSignaturePolicies;
import api.equinix.javasdk.internetaccess.client.InternetAccessTerms;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessAccountsImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessCabinetsImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessCagesImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConfigImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessConnectionServicesImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessIbxsImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessOperationalUnitsImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessOrdersImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessPatchPanelsImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessPricesImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessProductConfigurationsImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessPurchaseOrdersImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessServicesImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessSignaturePoliciesImpl;
import api.equinix.javasdk.internetaccess.client.implementation.InternetAccessTermsImpl;

/**
 * The primary entry point for accessing the Equinix Internet Access (EIA) v2 API.
 *
 * <p>Equinix Internet Access provides managed internet connectivity services through Equinix
 * data centers. The {@link #services()} accessor exposes the full EIA v2 service lifecycle
 * (create / get / update / delete / search) and the {@link #ibxs()} accessor exposes the EIA v2
 * product-availability lookup ({@code GET /internetAccess/v2/ibxs}). The remaining accessors expose
 * the EIA v1 read API used when building EIA requests — price search, accounts/agreements, terms and
 * conditions, operational units, signature policies, the attribute/default-configuration lookups,
 * purchase orders, the product-availability inventory (cages/cabinets/patch panels/connection
 * services), order history, and a single-IBX get.</p>
 *
 * <p>Each accessor uses lazy initialization — the internal client is created on first access and
 * reused for subsequent calls.</p>
 *
 * <h3>Scope (v1 selective)</h3>
 * <p>This domain surfaces the EIA v2 service lifecycle plus the read-only EIA v1 API. The v1 surfaces
 * exposed are:</p>
 * <ul>
 *   <li>{@link #prices() Price search} — {@code POST /internetAccess/v1/prices/search}.</li>
 *   <li>{@link #accounts() Accounts} — {@code GET /internetAccess/v1/accounts},
 *       {@code .../{accountNumber}} and {@code .../{accountNumber}/agreements}.</li>
 *   <li>{@link #termsAndConditions() Terms and conditions} — {@code GET /internetAccess/v1/terms}.</li>
 *   <li>{@link #operationalUnits() Operational units} — {@code GET /internetAccess/v1/operationalUnits}.</li>
 *   <li>{@link #signaturePolicies() Signature policies} — {@code GET /internetAccess/v1/signaturePolicies}.</li>
 *   <li>{@link #productConfigurations() Attribute/default configurations} —
 *       {@code routingProtocolConfigurations}, {@code dedicatedBandwidthConfigurations},
 *       {@code virtualBandwidthConfigurations}, {@code virtualConnectionDefaultConfigurations},
 *       {@code customerRouteConfigurations}, {@code dedicatedPortDefaultConfigurations} and
 *       {@code portConfigurations} (all under {@code GET /internetAccess/v1/}).</li>
 *   <li>{@link #purchaseOrders() Purchase orders} —
 *       {@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders} and {@code .../{number}}.</li>
 *   <li>Product-availability inventory — {@link #cages() cages}, {@link #cabinets() cabinets},
 *       {@link #patchPanels() patch panels} and {@link #connectionServices() connection services}
 *       (all under {@code GET /internetAccess/v1/}).</li>
 *   <li>{@link #orders() Order history} — {@code GET /internetAccess/v1/orders/{orderUUID}}.</li>
 *   <li>Single-IBX get — {@code GET /internetAccess/v1/ibxs/{ibx}}, via
 *       {@link InternetAccessIbxs#getByCode(String) ibxs().getByCode(...)}.</li>
 * </ul>
 * <p>The v1 service create/delete/search/details operations are intentionally <strong>not</strong>
 * exposed, as they are superseded by the {@link #services() v2 service lifecycle}.</p>
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

    private InternetAccessAccounts accounts;

    private InternetAccessTerms termsAndConditions;

    private InternetAccessOperationalUnits operationalUnits;

    private InternetAccessSignaturePolicies signaturePolicies;

    private InternetAccessProductConfigurations productConfigurations;

    private InternetAccessPurchaseOrders purchaseOrders;

    private InternetAccessOrders orders;

    private InternetAccessCages cages;

    private InternetAccessCabinets cabinets;

    private InternetAccessPatchPanels patchPanels;

    private InternetAccessConnectionServices connectionServices;

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
     * Returns the client for the Equinix Internet Access product-availability IBX lookups — the
     * v2 list of IBXs where EIA is available ({@code GET /internetAccess/v2/ibxs}) and the v1
     * single-IBX get ({@code GET /internetAccess/v1/ibxs/{ibx}}).
     *
     * @return the {@link InternetAccessIbxs} client
     */
    public InternetAccessIbxs ibxs() {
        if (this.ibxs == null) {
            this.ibxs = new InternetAccessIbxsImpl(this.internetAccessConfig.getIbxClient(),
                    this.internetAccessConfig.getIbxV1Client(), this);
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

    /**
     * Returns the client for the Equinix Internet Access v1 accounts lookups — the accounts
     * available for EIA ordering ({@code GET /internetAccess/v1/accounts}), a single account
     * ({@code .../{accountNumber}}) and account agreement statuses
     * ({@code .../{accountNumber}/agreements}).
     *
     * @return the {@link InternetAccessAccounts} client
     */
    public InternetAccessAccounts accounts() {
        if (this.accounts == null) {
            this.accounts = new InternetAccessAccountsImpl(this.internetAccessConfig.getAccountClient(), this);
        }
        return accounts;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 terms-and-conditions lookup
     * ({@code GET /internetAccess/v1/terms}).
     *
     * @return the {@link InternetAccessTerms} client
     */
    public InternetAccessTerms termsAndConditions() {
        if (this.termsAndConditions == null) {
            this.termsAndConditions = new InternetAccessTermsImpl(this.internetAccessConfig.getTermsClient(), this);
        }
        return termsAndConditions;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 operational-units lookup
     * ({@code GET /internetAccess/v1/operationalUnits}).
     *
     * @return the {@link InternetAccessOperationalUnits} client
     */
    public InternetAccessOperationalUnits operationalUnits() {
        if (this.operationalUnits == null) {
            this.operationalUnits = new InternetAccessOperationalUnitsImpl(this.internetAccessConfig.getOperationalUnitClient(), this);
        }
        return operationalUnits;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 signature-policies lookup
     * ({@code GET /internetAccess/v1/signaturePolicies}).
     *
     * @return the {@link InternetAccessSignaturePolicies} client
     */
    public InternetAccessSignaturePolicies signaturePolicies() {
        if (this.signaturePolicies == null) {
            this.signaturePolicies = new InternetAccessSignaturePoliciesImpl(this.internetAccessConfig.getSignaturePolicyClient(), this);
        }
        return signaturePolicies;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 attribute-eligibility / default
     * configuration lookups — routing, dedicated/virtual bandwidth, virtual-connection default,
     * customer-route, dedicated-port default and port configurations (all under
     * {@code GET /internetAccess/v1/}).
     *
     * @return the {@link InternetAccessProductConfigurations} client
     */
    public InternetAccessProductConfigurations productConfigurations() {
        if (this.productConfigurations == null) {
            this.productConfigurations = new InternetAccessProductConfigurationsImpl(
                    this.internetAccessConfig.getRoutingConfigurationClient(),
                    this.internetAccessConfig.getDedicatedBandwidthConfigurationClient(),
                    this.internetAccessConfig.getVirtualBandwidthConfigurationClient(),
                    this.internetAccessConfig.getVirtualConnectionDefaultConfigurationClient(),
                    this.internetAccessConfig.getCustomerRouteConfigurationClient(),
                    this.internetAccessConfig.getDedicatedPortDefaultConfigurationClient(),
                    this.internetAccessConfig.getPortConfigurationClient(),
                    this);
        }
        return productConfigurations;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 purchase-order lookups — the purchase
     * orders for an account ({@code GET /internetAccess/v1/accounts/{accountNumber}/purchaseOrders})
     * and a single purchase order ({@code .../{number}}).
     *
     * @return the {@link InternetAccessPurchaseOrders} client
     */
    public InternetAccessPurchaseOrders purchaseOrders() {
        if (this.purchaseOrders == null) {
            this.purchaseOrders = new InternetAccessPurchaseOrdersImpl(this.internetAccessConfig.getPurchaseOrderClient(), this);
        }
        return purchaseOrders;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 order lookup
     * ({@code GET /internetAccess/v1/orders/{orderUUID}}).
     *
     * @return the {@link InternetAccessOrders} client
     */
    public InternetAccessOrders orders() {
        if (this.orders == null) {
            this.orders = new InternetAccessOrdersImpl(this.internetAccessConfig.getOrderClient(), this);
        }
        return orders;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 cages product-availability lookup
     * ({@code GET /internetAccess/v1/cages}).
     *
     * @return the {@link InternetAccessCages} client
     */
    public InternetAccessCages cages() {
        if (this.cages == null) {
            this.cages = new InternetAccessCagesImpl(this.internetAccessConfig.getCageClient(), this);
        }
        return cages;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 cabinets product-availability lookup
     * ({@code GET /internetAccess/v1/cabinets}).
     *
     * @return the {@link InternetAccessCabinets} client
     */
    public InternetAccessCabinets cabinets() {
        if (this.cabinets == null) {
            this.cabinets = new InternetAccessCabinetsImpl(this.internetAccessConfig.getCabinetClient(), this);
        }
        return cabinets;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 patch-panels product-availability lookup
     * ({@code GET /internetAccess/v1/patchPanels}).
     *
     * @return the {@link InternetAccessPatchPanels} client
     */
    public InternetAccessPatchPanels patchPanels() {
        if (this.patchPanels == null) {
            this.patchPanels = new InternetAccessPatchPanelsImpl(this.internetAccessConfig.getPatchPanelClient(), this);
        }
        return patchPanels;
    }

    /**
     * Returns the client for the Equinix Internet Access v1 connection-services product-availability
     * lookup ({@code GET /internetAccess/v1/connectionServices}).
     *
     * @return the {@link InternetAccessConnectionServices} client
     */
    public InternetAccessConnectionServices connectionServices() {
        if (this.connectionServices == null) {
            this.connectionServices = new InternetAccessConnectionServicesImpl(this.internetAccessConfig.getConnectionServiceClient(), this);
        }
        return connectionServices;
    }
}
