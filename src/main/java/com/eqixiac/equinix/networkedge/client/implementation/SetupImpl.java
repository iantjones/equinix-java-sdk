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

package com.eqixiac.equinix.networkedge.client.implementation;

import com.eqixiac.equinix.core.http.ResponseHandler;
import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PaginatedList;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.core.enums.Region;
import com.eqixiac.equinix.NetworkEdge;
import com.eqixiac.equinix.networkedge.client.RequestBuilder;
import com.eqixiac.equinix.networkedge.client.Setup;
import com.eqixiac.equinix.networkedge.client.internal.*;
import com.eqixiac.equinix.networkedge.enums.DeviceManagementType;
import com.eqixiac.equinix.networkedge.enums.FileProcessType;
import com.eqixiac.equinix.networkedge.enums.LicenseType;
import com.eqixiac.equinix.networkedge.model.Account;
import com.eqixiac.equinix.networkedge.model.Metro;
import com.eqixiac.equinix.networkedge.model.implementation.AgreementStatus;
import com.eqixiac.equinix.networkedge.model.implementation.DowntimeNotification;
import com.eqixiac.equinix.networkedge.model.json.AccountJson;
import com.eqixiac.equinix.networkedge.model.json.MetroJson;
import com.eqixiac.equinix.networkedge.model.json.Pricing;
import com.eqixiac.equinix.networkedge.model.wrappers.AccountWrapper;
import com.eqixiac.equinix.networkedge.model.wrappers.MetroWrapper;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author ianjones
 */
@RequiredArgsConstructor
public class SetupImpl implements Setup {

    private final AccountClient<Account> serviceClientAccounts;

    private final MetroClient<Metro> serviceClientMetros;

    private final AgreementClient serviceClientAgreements;

    private final PricingClient serviceClientPricing;

    private final FilesClient serviceClientFiles;

    private final NotificationClient serviceClientNotifications;

    private final NetworkEdge serviceManager;

    public List<Account> listAccounts(MetroCode metroCode) {
        List<AccountJson> publicKeyList = serviceClientAccounts.list(metroCode);
        return ResponseHandler.mapList(publicKeyList, this.serviceClientAccounts, AccountWrapper::new);
    }

    public List<Account> listAllAccounts() {
        PaginatedList<Metro> metrosList = listMetros().loadAll();
        List<Account> accountList = new ArrayList<>();

        for(Metro metro : metrosList) {
            accountList.addAll(
                    ResponseHandler.mapList(serviceClientAccounts.list(metro.getMetroCode()),
                            this.serviceClientAccounts, AccountWrapper::new));
        }

        Predicate<Account> accountNumberNotNull = account -> account.getAccountNumber() != null;
        Predicate<Account> referenceIdNotNull = account -> account.getReferenceId() != null;

        return Stream.concat(
                accountList.stream().filter(accountNumberNotNull).filter(distinctByKey(Account::getAccountNumber)),
                accountList.stream().filter(referenceIdNotNull).filter(distinctByKey(Account::getReferenceId))).collect(Collectors.toList()
        );
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public PaginatedList<Metro> listMetros() {
        return listMetrosByRegion(null);
    }

    public PaginatedList<Metro> listMetrosByRegion(Region region) {
        Page<MetroJson> responsePage = serviceClientMetros.list(region);
        PaginatedList<Metro> metroList = ResponseHandler.mapPaginatedList(responsePage.getItems(), this.serviceClientMetros, MetroWrapper::new);
        return new PaginatedList<>(metroList, this.serviceClientMetros, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    public AgreementStatus getAgreementStatus(String accountNumber) {
        return serviceClientAgreements.getAgreementStatus(accountNumber);
    }

    public AgreementStatus createAgreement(String accountNumber, String termsVersionId) {
        return serviceClientAgreements.createAgreement(accountNumber, termsVersionId);
    }

    public String getVendorsTerms(String vendorPackage, LicenseType licenseType) {
        return serviceClientAgreements.getVendorsTerms(vendorPackage, licenseType);
    }

    public String getOrderTerms() {
        return serviceClientAgreements.getOrderTerms();
    }

    /**
     * {@inheritDoc}
     *
     */
    public Pricing getPricing(RequestBuilder.Pricing requestBuilder) {
        return serviceClientPricing.getPricing(requestBuilder);
    }

    /**
     * {@inheritDoc}
     *
     */
    public Pricing getPricing(String deviceUuid) {
        return serviceClientPricing.getPricing(deviceUuid);
    }

    public byte[] getOrderSummary(RequestBuilder.OrderSummary requestBuilder) {
        return serviceClientAccounts.getOrderSummary(requestBuilder);
    }

    public String uploadFile(MetroCode metroCode, String deviceTypeCode, FileProcessType processType,
                             DeviceManagementType deviceManagementType, LicenseType licenseType, String fileContents) {
        return serviceClientFiles.uploadFile(metroCode, deviceTypeCode, processType,
                deviceManagementType, licenseType, fileContents);
    }

    public DowntimeNotification listDowntimeNotifications() {
        return serviceClientNotifications.getDowntimeNotifications();
    }
}
