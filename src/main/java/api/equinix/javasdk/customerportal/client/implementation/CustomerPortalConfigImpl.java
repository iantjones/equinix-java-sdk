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

package api.equinix.javasdk.customerportal.client.implementation;

import api.equinix.javasdk.core.client.Config;
import api.equinix.javasdk.core.client.EquinixClient;
import api.equinix.javasdk.customerportal.client.CustomerPortalConfig;
import api.equinix.javasdk.customerportal.client.internal.implementation.CrossConnectClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.InvoiceDetailClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.InvoiceSummaryClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.OrderClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.ShipmentClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.SmartHandsClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.TroubleTicketClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.WorkVisitClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.NotificationClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.AssetClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.QuoteClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.SupportPlanClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.OrderHistoryClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.LookupClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.AttachmentClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.ReportClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.SecureCabinetClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.SupportCasesClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.UnifiedNotificationsClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.DigitalLoasClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.TroubleTicketOrderClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.BillingAccountClientImpl;
import api.equinix.javasdk.customerportal.client.internal.implementation.BillingAccountSearchClientImpl;
import lombok.Getter;

@Getter
public class CustomerPortalConfigImpl extends Config implements CustomerPortalConfig {

    private final InvoiceSummaryClientImpl invoiceSummaryClient;

    private final InvoiceDetailClientImpl invoiceDetailClient;

    private final CrossConnectClientImpl crossConnectClient;

    private final OrderClientImpl orderClient;

    private final TroubleTicketClientImpl troubleTicketClient;

    private final WorkVisitClientImpl workVisitClient;

    private final SmartHandsClientImpl smartHandsClient;

    private final ShipmentClientImpl shipmentClient;

    private final NotificationClientImpl notificationClient;

    private final AssetClientImpl assetClient;


    private final QuoteClientImpl quoteClient;

    private final SupportPlanClientImpl supportPlanClient;

    private final OrderHistoryClientImpl orderHistoryClient;

    private final LookupClientImpl lookupClient;

    private final AttachmentClientImpl attachmentClient;

    private final ReportClientImpl reportClient;


    private final SecureCabinetClientImpl secureCabinetClient;

    private final SupportCasesClientImpl supportCasesClient;

    private final UnifiedNotificationsClientImpl unifiedNotificationsClient;

    private final DigitalLoasClientImpl digitalLoasClient;

    private final TroubleTicketOrderClientImpl troubleTicketOrderClient;

    private final BillingAccountClientImpl billingAccountClient;

    private final BillingAccountSearchClientImpl billingAccountSearchClient;


    public CustomerPortalConfigImpl(EquinixClient equinixClient) {
        super(equinixClient);
        this.invoiceSummaryClient = new InvoiceSummaryClientImpl(this);
        this.invoiceDetailClient = new InvoiceDetailClientImpl(this);
        this.crossConnectClient = new CrossConnectClientImpl(this);
        this.orderClient = new OrderClientImpl(this);
        this.troubleTicketClient = new TroubleTicketClientImpl(this);
        this.workVisitClient = new WorkVisitClientImpl(this);
        this.smartHandsClient = new SmartHandsClientImpl(this);
        this.shipmentClient = new ShipmentClientImpl(this);
        this.notificationClient = new NotificationClientImpl(this);
        this.assetClient = new AssetClientImpl(this);
        this.quoteClient = new QuoteClientImpl(this);
        this.supportPlanClient = new SupportPlanClientImpl(this);
        this.orderHistoryClient = new OrderHistoryClientImpl(this);
        this.lookupClient = new LookupClientImpl(this);
        this.attachmentClient = new AttachmentClientImpl(this);
        this.reportClient = new ReportClientImpl(this);
        this.secureCabinetClient = new SecureCabinetClientImpl(this);
        this.supportCasesClient = new SupportCasesClientImpl(this);
        this.unifiedNotificationsClient = new UnifiedNotificationsClientImpl(this);
        this.digitalLoasClient = new DigitalLoasClientImpl(this);
        this.troubleTicketOrderClient = new TroubleTicketOrderClientImpl(this);
        this.billingAccountClient = new BillingAccountClientImpl(this);
        this.billingAccountSearchClient = new BillingAccountSearchClientImpl(this);
    }
}
