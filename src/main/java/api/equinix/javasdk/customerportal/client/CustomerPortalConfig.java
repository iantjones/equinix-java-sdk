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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.customerportal.client.internal.CrossConnectClient;
import api.equinix.javasdk.customerportal.client.internal.InvoiceDetailClient;
import api.equinix.javasdk.customerportal.client.internal.InvoiceSummaryClient;
import api.equinix.javasdk.customerportal.client.internal.OrderClient;
import api.equinix.javasdk.customerportal.client.internal.ResellerClient;
import api.equinix.javasdk.customerportal.client.internal.ResellerCustomerClient;
import api.equinix.javasdk.customerportal.client.internal.ShipmentClient;
import api.equinix.javasdk.customerportal.client.internal.SmartHandsClient;
import api.equinix.javasdk.customerportal.client.internal.TroubleTicketClient;
import api.equinix.javasdk.customerportal.client.internal.WorkVisitClient;
import api.equinix.javasdk.customerportal.client.internal.NotificationClient;
import api.equinix.javasdk.customerportal.client.internal.AssetClient;
import api.equinix.javasdk.customerportal.client.internal.QuoteClient;
import api.equinix.javasdk.customerportal.client.internal.SupportPlanClient;
import api.equinix.javasdk.customerportal.client.internal.OrderHistoryClient;
import api.equinix.javasdk.customerportal.client.internal.LookupClient;
import api.equinix.javasdk.customerportal.client.internal.AttachmentClient;
import api.equinix.javasdk.customerportal.client.internal.ReportClient;
import api.equinix.javasdk.customerportal.client.internal.SecureCabinetClient;
import api.equinix.javasdk.customerportal.client.internal.SupportCasesClient;
import api.equinix.javasdk.customerportal.client.internal.UnifiedNotificationsClient;
import api.equinix.javasdk.customerportal.client.internal.DigitalLoasClient;
import api.equinix.javasdk.customerportal.client.internal.TroubleTicketOrderClient;
import api.equinix.javasdk.customerportal.client.internal.BillingAccountClient;
import api.equinix.javasdk.customerportal.client.internal.BillingAccountSearchClient;
import api.equinix.javasdk.customerportal.model.Asset;
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.BillingAccount;
import api.equinix.javasdk.customerportal.model.BillingAccountV2;
import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.Quote;
import api.equinix.javasdk.customerportal.model.Report;
import api.equinix.javasdk.customerportal.model.Reseller;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.SupportPlan;
import api.equinix.javasdk.customerportal.model.TroubleTicket;

public interface CustomerPortalConfig {

    InvoiceSummaryClient<InvoiceSummary> getInvoiceSummaryClient();

    InvoiceDetailClient<InvoiceDetail> getInvoiceDetailClient();

    ResellerClient<Reseller> getResellerClient();

    ResellerCustomerClient<ResellerCustomer> getResellerCustomerClient();

    CrossConnectClient getCrossConnectClient();

    OrderClient<Order> getOrderClient();

    TroubleTicketClient<TroubleTicket> getTroubleTicketClient();

    WorkVisitClient getWorkVisitClient();

    SmartHandsClient getSmartHandsClient();

    ShipmentClient getShipmentClient();

    NotificationClient getNotificationClient();

    AssetClient<Asset> getAssetClient();


    QuoteClient<Quote> getQuoteClient();

    SupportPlanClient<SupportPlan> getSupportPlanClient();

    OrderHistoryClient getOrderHistoryClient();

    LookupClient getLookupClient();

    AttachmentClient<Attachment> getAttachmentClient();

    ReportClient<Report> getReportClient();


    SecureCabinetClient getSecureCabinetClient();

    SupportCasesClient getSupportCasesClient();

    UnifiedNotificationsClient getUnifiedNotificationsClient();

    DigitalLoasClient getDigitalLoasClient();

    TroubleTicketOrderClient getTroubleTicketOrderClient();

    BillingAccountClient<BillingAccount> getBillingAccountClient();

    BillingAccountSearchClient<BillingAccountV2> getBillingAccountSearchClient();

}
