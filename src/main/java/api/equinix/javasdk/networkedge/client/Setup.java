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

package api.equinix.javasdk.networkedge.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.networkedge.enums.DeviceManagementType;
import api.equinix.javasdk.networkedge.enums.FileProcessType;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.Account;
import api.equinix.javasdk.networkedge.model.Metro;
import api.equinix.javasdk.networkedge.model.implementation.AgreementStatus;
import api.equinix.javasdk.networkedge.model.implementation.DowntimeNotification;
import api.equinix.javasdk.networkedge.model.json.Pricing;

import java.util.List;

/**
 * Client interface for Network Edge setup and configuration operations. Provides access to
 * account listings, metro availability, agreement management, vendor terms, order terms,
 * pricing information, and generic file uploads.
 *
 * @author ianjones
 */
public interface Setup {

    /**
     * Lists available Accounts in the provided metro.
     *
     * @param metroCode the {@link api.equinix.javasdk.core.enums.MetroCode} of the accounts.
     * @return {@link java.util.List}
     */
    List<Account> listAccounts(MetroCode metroCode);

    /**
     * Lists available Accounts in all available metros. Note that this can be a slow operation as it checks
     * every metro and then de-dupes accounts based on accountId and then referenceId.
     *
     * @return {@link java.util.List}
     */
    List<Account> listAllAccounts();

    /**
     * Lists available Metros with Network Edge present.
     *
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<Metro> listMetros();

    /**
     * Lists available Metros with Network Edge present in the specified Region.
     *
     * @param region the {@link api.equinix.javasdk.core.enums.Region} of the metros.
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<Metro> listMetrosByRegion(Region region);

    /**
     * Returns the Agreement Status for the provided account number.
     *
     * @param accountNumber the unique account identifier.
     * @return {@link java.lang.String}
     */
    AgreementStatus getAgreementStatus(String accountNumber);

    /**
     * Creates an Agreement for the provided account number. The termsVersionId can be obtained from getAgreementStatus().
     *
     * @param accountNumber the unique account identifier.
     * @param termsVersionId the unique identifier of the agreement terms version.
     * @return {@link api.equinix.javasdk.networkedge.model.implementation.AgreementStatus}
     */
    AgreementStatus createAgreement(String accountNumber, String termsVersionId);

    /**
     * Returns the vendor terms for the specified vendor package and license type.
     *
     * @param vendorPackage the desired vendor package.
     * @param licenseType the desired {@link api.equinix.javasdk.networkedge.enums.LicenseType}.
     * @return {@link java.lang.String}
     */
    String getVendorsTerms(String vendorPackage, LicenseType licenseType);

    /**
     * Returns the order terms for Network Edge.
     *
     * @return {@link java.lang.String}
     */
    String getOrderTerms();

    /**
     * Gets pricing information based on the parameters provided.
     *
     * @param requestBuilder the desired query parameters.
     * @return {@link api.equinix.javasdk.networkedge.model.json.Pricing}
     */
    Pricing getPricing(RequestBuilder.Pricing requestBuilder);

    /**
     * Gets pricing information for the specified Device.
     *
     * @param deviceUuid the unique identifier of the Device.
     * @return {@link api.equinix.javasdk.networkedge.model.json.Pricing}
     */
    Pricing getPricing(String deviceUuid);

    /**
     * Gets the order summary document based on the parameters provided.
     *
     * @param requestBuilder the desired query parameters.
     * @return the order summary as a byte array.
     */
    byte[] getOrderSummary(RequestBuilder.OrderSummary requestBuilder);

    /**
     * Uploads a generic file (a license file or a cloud-init / bootstrap file) and returns the
     * unique id of the uploaded file. The returned file id can subsequently be referenced when
     * creating a device or triggering an RMA (e.g. as {@code cloudInitFileId} or {@code licenseFileId}).
     *
     * @param metroCode the {@link api.equinix.javasdk.core.enums.MetroCode} the file applies to.
     * @param deviceTypeCode the device type code the file applies to (e.g. {@code C8000V}).
     * @param processType whether the file is a {@code LICENSE} or {@code CLOUD_INIT} file.
     * @param deviceManagementType the {@link api.equinix.javasdk.networkedge.enums.DeviceManagementType}, or {@code null}.
     * @param licenseType the {@link api.equinix.javasdk.networkedge.enums.LicenseType}, or {@code null}.
     * @param fileContents the contents of the file to upload.
     * @return {@link java.lang.String} the unique id of the uploaded file.
     */
    String uploadFile(MetroCode metroCode, String deviceTypeCode, FileProcessType processType,
                      DeviceManagementType deviceManagementType, LicenseType licenseType, String fileContents);

    /**
     * Returns the current planned and unplanned downtime notifications for Network Edge APIs
     * and infrastructure.
     *
     * @return {@link api.equinix.javasdk.networkedge.model.implementation.DowntimeNotification}
     */
    DowntimeNotification listDowntimeNotifications();
}
