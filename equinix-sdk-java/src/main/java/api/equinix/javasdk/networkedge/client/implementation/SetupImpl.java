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

package api.equinix.javasdk.networkedge.client.implementation;

import api.equinix.javasdk.core.http.Utils;
import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.core.enums.Region;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.networkedge.client.RequestBuilder;
import api.equinix.javasdk.networkedge.client.Setup;
import api.equinix.javasdk.networkedge.client.internal.*;
import api.equinix.javasdk.networkedge.enums.LicenseType;
import api.equinix.javasdk.networkedge.model.Account;
import api.equinix.javasdk.networkedge.model.Metro;
import api.equinix.javasdk.networkedge.model.implementation.AgreementStatus;
import api.equinix.javasdk.networkedge.model.implementation.DNSLookup;
import api.equinix.javasdk.networkedge.model.json.AccountJson;
import api.equinix.javasdk.networkedge.model.json.MetroJson;
import api.equinix.javasdk.networkedge.model.json.Pricing;
import api.equinix.javasdk.networkedge.model.wrappers.AccountWrapper;
import api.equinix.javasdk.networkedge.model.wrappers.MetroWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>SetupImpl class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class SetupImpl implements Setup {

    private final NetworkEdge serviceManager;

    private final AccountClient<Account> serviceClientAccounts;
    private final MetroClient<Metro> serviceClientMetros;
    private final DNSClient<DNSLookup> serviceClientDNS;
    private final AgreementClient serviceClientAgreements;
    private final PricingClient serviceClientPricing;

    /**
     * <p>Constructor for SetupImpl.</p>
     *
     * @param serviceClientAccounts a {@link api.equinix.javasdk.networkedge.client.internal.AccountClient} object.
     * @param serviceClientMetros a {@link api.equinix.javasdk.networkedge.client.internal.MetroClient} object.
     * @param serviceClientDNS a {@link api.equinix.javasdk.networkedge.client.internal.DNSClient} object.
     * @param serviceClientAgreements a {@link api.equinix.javasdk.networkedge.client.internal.AgreementClient} object.
     * @param serviceClientPricing a {@link api.equinix.javasdk.networkedge.client.internal.PricingClient} object.
     * @param serviceManager a {@link api.equinix.javasdk.NetworkEdge} object.
     */
    public SetupImpl(AccountClient<Account> serviceClientAccounts, MetroClient<Metro> serviceClientMetros,
                     DNSClient<DNSLookup> serviceClientDNS, AgreementClient serviceClientAgreements,
                     PricingClient serviceClientPricing, NetworkEdge serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceClientAccounts = serviceClientAccounts;
        this.serviceClientMetros = serviceClientMetros;
        this.serviceClientDNS = serviceClientDNS;
        this.serviceClientAgreements = serviceClientAgreements;
        this.serviceClientPricing = serviceClientPricing;
    }

    /** {@inheritDoc} */
    public List<Account> listAccounts(MetroCode metroCode) {
        List<AccountJson> publicKeyList = serviceClientAccounts.list(metroCode);
        return Utils.mapList(publicKeyList, this.serviceClientAccounts, AccountWrapper::new);
    }

    /**
     * <p>listAllAccounts.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Account> listAllAccounts() {
        PaginatedList<Metro> metrosList = listMetros().loadAll();
        List<Account> accountList = new ArrayList<>();

        for(Metro metro : metrosList) {
            accountList.addAll(
                    Utils.mapList(serviceClientAccounts.list(metro.getMetroCode()),
                            this.serviceClientAccounts, AccountWrapper::new));
        }

        Predicate<Account> accountNumberNotNull = account -> account.getAccountNumber() != null;
        Predicate<Account> referenceIdNotNull = account -> account.getReferenceId() != null;

        return Stream.concat(
                accountList.stream().filter(accountNumberNotNull).filter(distinctByKey(Account::getAccountNumber)),
                accountList.stream().filter(referenceIdNotNull).filter(distinctByKey(Account::getReferenceId))).collect(Collectors.toList()
        );
    }

    /**
     * <p>distinctByKey.</p>
     *
     * @param keyExtractor a {@link java.util.function.Function} object.
     * @param <T> a T object.
     * @return a {@link java.util.function.Predicate} object.
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * <p>listMetros.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<Metro> listMetros() {
        return listMetrosByRegion(null);
    }

    /** {@inheritDoc} */
    public PaginatedList<Metro> listMetrosByRegion(Region region) {
        Page<Metro, MetroJson> responsePage = serviceClientMetros.list(region);
        PaginatedList<Metro> metroList = Utils.mapPaginatedList(responsePage.getItems(), this.serviceClientMetros, MetroWrapper::new);
        return new PaginatedList<>(metroList, this.serviceClientMetros, responsePage.getAssociatedRequest(), responsePage.getAssociatedResponse(), responsePage.getPagination());
    }

    /**
     * {@inheritDoc}
     *
     * <p>dnsLookup.</p>
     */
    public Map<String, DNSLookup> dnsLookup(RequestBuilder.DNSLookup requestBuilder) {
        return this.serviceClientDNS.dnsLookup(requestBuilder);
    }

    /** {@inheritDoc} */
    public AgreementStatus getAgreementStatus(String accountNumber) {
        return serviceClientAgreements.getAgreementStatus(accountNumber);
    }

    /** {@inheritDoc} */
    public AgreementStatus createAgreement(String accountNumber, String termsVersionId) {
        return serviceClientAgreements.createAgreement(accountNumber, termsVersionId);
    }

    /** {@inheritDoc} */
    public String getVendorsTerms(String vendorPackage, LicenseType licenseType) {
        return serviceClientAgreements.getVendorsTerms(vendorPackage, licenseType);
    }

    /**
     * <p>getOrderTerms.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getOrderTerms() {
        return serviceClientAgreements.getOrderTerms();
    }

    /**
     * {@inheritDoc}
     *
     * <p>getPricing.</p>
     */
    public Pricing getPricing(RequestBuilder.Pricing requestBuilder) {
        return serviceClientPricing.getPricing(requestBuilder);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceUuid a {@link java.lang.String} object.
     * @return a {@link api.equinix.javasdk.networkedge.model.json.Pricing} object.
     */
    public Pricing getPricing(String deviceUuid) {
        return serviceClientPricing.getPricing(deviceUuid);
    }

    public byte[] getOrderSummary(RequestBuilder.OrderSummary requestBuilder) {
        return serviceClientAccounts.getOrderSummary(requestBuilder);
    }
}
