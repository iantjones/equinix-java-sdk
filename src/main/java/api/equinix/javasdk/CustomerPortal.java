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
import api.equinix.javasdk.core.auth.EquinixCredentialsProvider;
import api.equinix.javasdk.core.auth.EquinixStaticCredentialsProvider;
import api.equinix.javasdk.customerportal.client.CrossConnects;
import api.equinix.javasdk.customerportal.client.CustomerPortalConfig;
import api.equinix.javasdk.customerportal.client.Invoices;
import api.equinix.javasdk.customerportal.client.Orders;
import api.equinix.javasdk.customerportal.client.Shipments;
import api.equinix.javasdk.customerportal.client.SmartHandsRequests;
import api.equinix.javasdk.customerportal.client.TroubleTickets;
import api.equinix.javasdk.customerportal.client.WorkVisits;
import api.equinix.javasdk.customerportal.client.Notifications;
import api.equinix.javasdk.customerportal.client.Assets;
import api.equinix.javasdk.customerportal.client.Quotes;
import api.equinix.javasdk.customerportal.client.SupportPlans;
import api.equinix.javasdk.customerportal.client.OrderHistory;
import api.equinix.javasdk.customerportal.client.Lookups;
import api.equinix.javasdk.customerportal.client.Attachments;
import api.equinix.javasdk.customerportal.client.Reports;
import api.equinix.javasdk.customerportal.client.SecureCabinets;
import api.equinix.javasdk.customerportal.client.SupportCases;
import api.equinix.javasdk.customerportal.client.UnifiedNotifications;
import api.equinix.javasdk.customerportal.client.DigitalLoas;
import api.equinix.javasdk.customerportal.client.TroubleTicketOrders;
import api.equinix.javasdk.customerportal.client.BillingAccounts;
import api.equinix.javasdk.customerportal.client.BillingAccountsSearch;
import api.equinix.javasdk.customerportal.client.implementation.CrossConnectsImpl;
import api.equinix.javasdk.customerportal.client.implementation.CustomerPortalConfigImpl;
import api.equinix.javasdk.customerportal.client.implementation.InvoicesImpl;
import api.equinix.javasdk.customerportal.client.implementation.OrdersImpl;
import api.equinix.javasdk.customerportal.client.implementation.ShipmentsImpl;
import api.equinix.javasdk.customerportal.client.implementation.SmartHandsRequestsImpl;
import api.equinix.javasdk.customerportal.client.implementation.TroubleTicketsImpl;
import api.equinix.javasdk.customerportal.client.implementation.WorkVisitsImpl;
import api.equinix.javasdk.customerportal.client.implementation.NotificationsImpl;
import api.equinix.javasdk.customerportal.client.implementation.AssetsImpl;
import api.equinix.javasdk.customerportal.client.implementation.QuotesImpl;
import api.equinix.javasdk.customerportal.client.implementation.SupportPlansImpl;
import api.equinix.javasdk.customerportal.client.implementation.OrderHistoryImpl;
import api.equinix.javasdk.customerportal.client.implementation.LookupsImpl;
import api.equinix.javasdk.customerportal.client.implementation.AttachmentsImpl;
import api.equinix.javasdk.customerportal.client.implementation.ReportsImpl;
import api.equinix.javasdk.customerportal.client.implementation.SecureCabinetsImpl;
import api.equinix.javasdk.customerportal.client.implementation.SupportCasesImpl;
import api.equinix.javasdk.customerportal.client.implementation.UnifiedNotificationsImpl;
import api.equinix.javasdk.customerportal.client.implementation.DigitalLoasImpl;
import api.equinix.javasdk.customerportal.client.implementation.TroubleTicketOrdersImpl;
import api.equinix.javasdk.customerportal.client.implementation.BillingAccountsImpl;
import api.equinix.javasdk.customerportal.client.implementation.BillingAccountsSearchImpl;

/**
 * The primary entry point for accessing Equinix Customer Portal APIs.
 *
 * <p>The Customer Portal provides comprehensive operations management for Equinix colocation
 * facilities. This class offers typed access to all portal resources including cross-connects,
 * trouble tickets, work visits, smart hands requests, shipments, invoices, orders, assets,
 * reports, and more.</p>
 *
 * <p>All resource accessors use lazy initialization — internal clients are created on first access
 * and reused for subsequent calls.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * CustomerPortal portal = new CustomerPortal(credentials);
 *
 * // List cross-connects
 * PaginatedList<CrossConnect> crossConnects = portal.crossConnects().list();
 *
 * // List trouble tickets
 * PaginatedList<TroubleTicket> tickets = portal.troubleTickets().list();
 *
 * // Access invoice summaries
 * PaginatedList<InvoiceSummary> invoices = portal.invoices().summaries();
 * }</pre>
 *
 * @author ianjones
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 * @see api.equinix.javasdk.customerportal.client.CrossConnects
 * @see api.equinix.javasdk.customerportal.client.TroubleTickets
 * @see api.equinix.javasdk.customerportal.client.Invoices
 */
public final class CustomerPortal extends EquinixClient {

    private Invoices invoices;

    private CrossConnects crossConnects;

    private Orders orders;

    private TroubleTickets troubleTickets;

    private WorkVisits workVisits;

    private SmartHandsRequests smartHandsRequests;

    private Shipments shipments;

    private Notifications notifications;

    private Assets assets;


    private Quotes quotes;

    private SupportPlans supportPlans;

    private OrderHistory orderHistory;

    private Lookups lookups;

    private Attachments attachments;

    private Reports reports;


    private SecureCabinets secureCabinets;

    private SupportCases supportCases;

    private UnifiedNotifications unifiedNotifications;

    private DigitalLoas digitalLoas;

    private TroubleTicketOrders troubleTicketOrders;

    private BillingAccounts billingAccounts;

    private BillingAccountsSearch billingAccountsSearch;


    final private CustomerPortalConfig customerPortalConfig;

    /**
     * Creates a new Customer Portal client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public CustomerPortal(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * Creates a new Customer Portal client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment; {@code false} for production
     */
    public CustomerPortal(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        this(new EquinixStaticCredentialsProvider(equinixCredentials), isSandBoxed);
    }

    /**
     * Creates a new Customer Portal client whose credentials are resolved through the given provider.
     * Authentication occurs automatically on the first API call.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     */
    public CustomerPortal(EquinixCredentialsProvider credentialsProvider) {
        this(credentialsProvider, false);
    }

    /**
     * Creates a new Customer Portal client over a custom credentials provider, with optional sandbox mode.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment; {@code false} for production
     */
    public CustomerPortal(EquinixCredentialsProvider credentialsProvider, boolean isSandBoxed) {
        super(credentialsProvider, isSandBoxed);

        String paramFile = "json/apiParams_CustomerPortal.json";
        equinixClient.appendApiParams(paramFile);

        this.customerPortalConfig = new CustomerPortalConfigImpl(equinixClient);
    }

    /**
     * Creates a new CustomerPortal client with explicit {@link EquinixConfig} options.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public CustomerPortal(EquinixCredentials equinixCredentials, EquinixConfig config) {
        this(new EquinixStaticCredentialsProvider(equinixCredentials), config);
    }

    /**
     * Creates a new CustomerPortal client over a custom credentials provider with explicit
     * {@link EquinixConfig} options.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public CustomerPortal(EquinixCredentialsProvider credentialsProvider, EquinixConfig config) {
        super(credentialsProvider, config);

        String paramFile = "json/apiParams_CustomerPortal.json";
        equinixClient.appendApiParams(paramFile);

        this.customerPortalConfig = new CustomerPortalConfigImpl(equinixClient);
    }

    /**
     * Package-private constructor for {@link Equinix} sessions: builds this domain client over a
     * shared core client (one OAuth token + connection pool across domains).
     */
    CustomerPortal(api.equinix.javasdk.core.client.EquinixClient sharedCore) {
        super(sharedCore);
        equinixClient.appendApiParams("json/apiParams_CustomerPortal.json");
        this.customerPortalConfig = new CustomerPortalConfigImpl(equinixClient);
    }

    /**
     * Returns the client for accessing invoice summaries and details. Provides billing
     * information including line items, charges, and payment history.
     *
     * @return the {@link Invoices} client for invoice operations
     */
    public Invoices invoices() {
        if (this.invoices == null) {
            this.invoices = new InvoicesImpl(this.customerPortalConfig.getInvoiceSummaryClient(),
                    this.customerPortalConfig.getInvoiceDetailClient(), this);
        }
        return invoices;
    }

    /**
     * Returns the client for managing cross-connect orders within Equinix IBX data centers.
     * Cross-connects are physical cable connections between customer cages/cabinets and other
     * network participants within the same facility.
     *
     * @return the {@link CrossConnects} client for cross-connect operations
     */
    public CrossConnects crossConnects() {
        if (this.crossConnects == null) {
            this.crossConnects = new CrossConnectsImpl(this.customerPortalConfig.getCrossConnectClient(), this);
        }
        return crossConnects;
    }

    /**
     * Returns the client for managing orders for Equinix services and products. Orders
     * represent requests for new services, upgrades, or changes to existing infrastructure.
     *
     * @return the {@link Orders} client for order operations
     */
    public Orders orders() {
        if (this.orders == null) {
            this.orders = new OrdersImpl(this.customerPortalConfig.getOrderClient(), this);
        }
        return orders;
    }

    /**
     * Returns the client for managing trouble tickets for reporting and tracking issues.
     * Trouble tickets allow customers to report problems with their colocation infrastructure
     * and track resolution progress.
     *
     * @return the {@link TroubleTickets} client for trouble ticket operations
     */
    public TroubleTickets troubleTickets() {
        if (this.troubleTickets == null) {
            this.troubleTickets = new TroubleTicketsImpl(this.customerPortalConfig.getTroubleTicketClient(), this);
        }
        return troubleTickets;
    }

    /**
     * Returns the client for scheduling and managing work visits to Equinix IBX data centers.
     * Work visits coordinate on-site access for customer personnel to perform maintenance
     * or installation tasks within their colocation space.
     *
     * @return the {@link WorkVisits} client for work visit operations
     */
    public WorkVisits workVisits() {
        if (this.workVisits == null) {
            this.workVisits = new WorkVisitsImpl(this.customerPortalConfig.getWorkVisitClient(), this);
        }
        return workVisits;
    }

    /**
     * Returns the client for requesting Smart Hands services. Smart Hands provides remote
     * technical support at Equinix facilities, allowing trained technicians to perform
     * tasks on behalf of the customer.
     *
     * @return the {@link SmartHandsRequests} client for Smart Hands operations
     */
    public SmartHandsRequests smartHandsRequests() {
        if (this.smartHandsRequests == null) {
            this.smartHandsRequests = new SmartHandsRequestsImpl(this.customerPortalConfig.getSmartHandsClient(), this);
        }
        return smartHandsRequests;
    }

    /**
     * Returns the client for managing equipment shipments to and from Equinix data centers.
     * Shipments track inbound and outbound deliveries of hardware and equipment to
     * customer colocation spaces.
     *
     * @return the {@link Shipments} client for shipment operations
     */
    public Shipments shipments() {
        if (this.shipments == null) {
            this.shipments = new ShipmentsImpl(this.customerPortalConfig.getShipmentClient(), this);
        }
        return shipments;
    }

    /**
     * Returns the client for managing notification preferences and delivery. Notifications
     * keep customers informed about events affecting their colocation infrastructure,
     * including maintenance windows and operational alerts.
     *
     * @return the {@link Notifications} client for notification operations
     */
    public Notifications notifications() {
        if (this.notifications == null) {
            this.notifications = new NotificationsImpl(this.customerPortalConfig.getNotificationClient(), this);
        }
        return notifications;
    }

    /**
     * Returns the client for managing infrastructure assets within Equinix facilities.
     * Assets represent physical equipment such as cages, cabinets, power circuits, and
     * other deployed infrastructure.
     *
     * @return the {@link Assets} client for asset operations
     */
    public Assets assets() {
        if (this.assets == null) {
            this.assets = new AssetsImpl(this.customerPortalConfig.getAssetClient(), this);
        }
        return assets;
    }


    /**
     * Returns the client for requesting and managing service quotes. Quotes provide
     * pricing estimates for new Equinix services, expansions, or changes to existing
     * infrastructure before committing to an order.
     *
     * @return the {@link Quotes} client for quote operations
     */
    public Quotes quotes() {
        if (this.quotes == null) {
            this.quotes = new QuotesImpl(this.customerPortalConfig.getQuoteClient(), this);
        }
        return quotes;
    }

    /**
     * Returns the client for accessing support plan information and entitlements. Support
     * plans define the level of service and response times available to the customer
     * for technical and operational support.
     *
     * @return the {@link SupportPlans} client for support plan operations
     */
    public SupportPlans supportPlans() {
        if (this.supportPlans == null) {
            this.supportPlans = new SupportPlansImpl(this.customerPortalConfig.getSupportPlanClient(), this);
        }
        return supportPlans;
    }

    /**
     * Returns the client for querying historical order records. Order history provides
     * a read-only view of past orders, including completed, cancelled, and in-progress
     * service requests.
     *
     * @return the {@link OrderHistory} client for order history operations
     */
    public OrderHistory orderHistory() {
        if (this.orderHistory == null) {
            this.orderHistory = new OrderHistoryImpl(this.customerPortalConfig.getOrderHistoryClient(), this);
        }
        return orderHistory;
    }

    /**
     * Returns the client for performing reference data lookups such as IBX locations and
     * metro codes. Lookups provide the enumerated values and reference data needed when
     * creating or filtering other portal resources.
     *
     * @return the {@link Lookups} client for reference data lookup operations
     */
    public Lookups lookups() {
        if (this.lookups == null) {
            this.lookups = new LookupsImpl(this.customerPortalConfig.getLookupClient(), this);
        }
        return lookups;
    }

    /**
     * Returns the client for managing file attachments on portal resources like tickets
     * and cases. Attachments allow customers to upload supporting documentation such as
     * diagrams, photos, and configuration files.
     *
     * @return the {@link Attachments} client for attachment operations
     */
    public Attachments attachments() {
        if (this.attachments == null) {
            this.attachments = new AttachmentsImpl(this.customerPortalConfig.getAttachmentClient(), this);
        }
        return attachments;
    }

    /**
     * Returns the client for generating and accessing operational reports. Reports provide
     * aggregated views of facility usage, power consumption, capacity, and other
     * operational metrics.
     *
     * @return the {@link Reports} client for report operations
     */
    public Reports reports() {
        if (this.reports == null) {
            this.reports = new ReportsImpl(this.customerPortalConfig.getReportClient(), this);
        }
        return reports;
    }

    /**
     * Returns the client for managing secure cabinet access and configurations. Secure
     * cabinets provide enhanced physical security controls for sensitive equipment
     * within Equinix colocation facilities.
     *
     * @return the {@link SecureCabinets} client for secure cabinet operations
     */
    public SecureCabinets secureCabinets() {
        if (this.secureCabinets == null) {
            this.secureCabinets = new SecureCabinetsImpl(this.customerPortalConfig.getSecureCabinetClient(), this);
        }
        return secureCabinets;
    }

    /**
     * Returns the client for creating and managing support cases (the v2 Support tickets API).
     * Support cases track non-incident requests and inquiries raised against the customer's
     * Equinix services.
     *
     * @return the {@link SupportCases} client for support case operations
     */
    public SupportCases supportCases() {
        if (this.supportCases == null) {
            this.supportCases = new SupportCasesImpl(this.customerPortalConfig.getSupportCasesClient(), this);
        }
        return supportCases;
    }

    /**
     * Returns the client for searching unified notifications (the {@code /notifications/v2} events
     * API). Unified notifications aggregate IBX and network events into a single searchable feed.
     *
     * @return the {@link UnifiedNotifications} client for unified notification operations
     */
    public UnifiedNotifications unifiedNotifications() {
        if (this.unifiedNotifications == null) {
            this.unifiedNotifications = new UnifiedNotificationsImpl(this.customerPortalConfig.getUnifiedNotificationsClient(), this);
        }
        return unifiedNotifications;
    }

    /**
     * Returns the client for managing Digital Letters of Authorization (DiLOAs). DiLOAs authorize
     * third parties to provision cross-connects to the customer's assets.
     *
     * @return the {@link DigitalLoas} client for digital LOA operations
     */
    public DigitalLoas digitalLoas() {
        if (this.digitalLoas == null) {
            this.digitalLoas = new DigitalLoasImpl(this.customerPortalConfig.getDigitalLoasClient(), this);
        }
        return digitalLoas;
    }

    /**
     * Returns the client for placing trouble-ticket orders (the v1 trouble-ticket order API).
     * Provides the supported types, permitted locations and order placement for reporting
     * infrastructure issues as orders.
     *
     * @return the {@link TroubleTicketOrders} client for trouble-ticket order operations
     */
    public TroubleTicketOrders troubleTicketOrders() {
        if (this.troubleTicketOrders == null) {
            this.troubleTicketOrders = new TroubleTicketOrdersImpl(this.customerPortalConfig.getTroubleTicketOrderClient(), this);
        }
        return troubleTicketOrders;
    }

    /**
     * Returns the client for accessing finance billing-account summaries and downloading invoice
     * documents (the v1 finance API).
     *
     * @return the {@link BillingAccounts} client for billing-account operations
     */
    public BillingAccounts billingAccounts() {
        if (this.billingAccounts == null) {
            this.billingAccounts = new BillingAccountsImpl(this.customerPortalConfig.getBillingAccountClient(), this);
        }
        return billingAccounts;
    }

    /**
     * Returns the client for searching billing accounts and looking them up by number or id
     * (the billing v2 billing-accounts API).
     *
     * @return the {@link BillingAccountsSearch} client for billing-account search operations
     */
    public BillingAccountsSearch billingAccountsSearch() {
        if (this.billingAccountsSearch == null) {
            this.billingAccountsSearch = new BillingAccountsSearchImpl(this.customerPortalConfig.getBillingAccountSearchClient(), this);
        }
        return billingAccountsSearch;
    }
}
