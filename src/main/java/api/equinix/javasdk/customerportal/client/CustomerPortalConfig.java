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
import api.equinix.javasdk.customerportal.client.internal.SupportCaseClient;
import api.equinix.javasdk.customerportal.client.internal.QuoteClient;
import api.equinix.javasdk.customerportal.client.internal.SupportPlanClient;
import api.equinix.javasdk.customerportal.client.internal.OrderHistoryClient;
import api.equinix.javasdk.customerportal.client.internal.LookupClient;
import api.equinix.javasdk.customerportal.client.internal.AttachmentClient;
import api.equinix.javasdk.customerportal.client.internal.ReportClient;
import api.equinix.javasdk.customerportal.client.internal.DigitalLOAClient;
import api.equinix.javasdk.customerportal.client.internal.SecureCabinetClient;
import api.equinix.javasdk.customerportal.client.internal.UnifiedNotificationClient;
import api.equinix.javasdk.customerportal.client.internal.BillingCreditClient;
import api.equinix.javasdk.customerportal.model.Asset;
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.BillingCredit;
import api.equinix.javasdk.customerportal.model.CrossConnect;
import api.equinix.javasdk.customerportal.model.DigitalLOA;
import api.equinix.javasdk.customerportal.model.InvoiceDetail;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.LookupLocation;
import api.equinix.javasdk.customerportal.model.Notification;
import api.equinix.javasdk.customerportal.model.Order;
import api.equinix.javasdk.customerportal.model.OrderHistoryItem;
import api.equinix.javasdk.customerportal.model.Quote;
import api.equinix.javasdk.customerportal.model.Report;
import api.equinix.javasdk.customerportal.model.Reseller;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.SecureCabinet;
import api.equinix.javasdk.customerportal.model.Shipment;
import api.equinix.javasdk.customerportal.model.SmartHands;
import api.equinix.javasdk.customerportal.model.SupportCase;
import api.equinix.javasdk.customerportal.model.SupportPlan;
import api.equinix.javasdk.customerportal.model.TroubleTicket;
import api.equinix.javasdk.customerportal.model.UnifiedNotification;
import api.equinix.javasdk.customerportal.model.WorkVisit;

public interface CustomerPortalConfig {

    InvoiceSummaryClient<InvoiceSummary> getInvoiceSummaryClient();

    InvoiceDetailClient<InvoiceDetail> getInvoiceDetailClient();

    ResellerClient<Reseller> getResellerClient();

    ResellerCustomerClient<ResellerCustomer> getResellerCustomerClient();

    CrossConnectClient<CrossConnect> getCrossConnectClient();

    OrderClient<Order> getOrderClient();

    TroubleTicketClient<TroubleTicket> getTroubleTicketClient();

    WorkVisitClient<WorkVisit> getWorkVisitClient();

    SmartHandsClient<SmartHands> getSmartHandsClient();

    ShipmentClient<Shipment> getShipmentClient();

    NotificationClient<Notification> getNotificationClient();

    AssetClient<Asset> getAssetClient();

    SupportCaseClient<SupportCase> getSupportCaseClient();

    QuoteClient<Quote> getQuoteClient();

    SupportPlanClient<SupportPlan> getSupportPlanClient();

    OrderHistoryClient<OrderHistoryItem> getOrderHistoryClient();

    LookupClient<LookupLocation> getLookupClient();

    AttachmentClient<Attachment> getAttachmentClient();

    ReportClient<Report> getReportClient();

    DigitalLOAClient<DigitalLOA> getDigitalLOAClient();

    SecureCabinetClient<SecureCabinet> getSecureCabinetClient();

    UnifiedNotificationClient<UnifiedNotification> getUnifiedNotificationClient();

    BillingCreditClient<BillingCredit> getBillingCreditClient();
}
