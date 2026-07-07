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

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.customerportal.client.DigitalLoas;
import api.equinix.javasdk.customerportal.client.internal.DigitalLoasClient;
import api.equinix.javasdk.customerportal.model.BetaTermsAgreement;
import api.equinix.javasdk.customerportal.model.DigitalLoa;
import api.equinix.javasdk.customerportal.model.DigitalLoaChange;
import api.equinix.javasdk.customerportal.model.LoaCustomerOrganization;
import api.equinix.javasdk.customerportal.model.PrivateBetaPermission;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaCreateRequest;
import api.equinix.javasdk.customerportal.model.json.creators.DigitalLoaSearchRequest;
import api.equinix.javasdk.customerportal.model.json.creators.PrivateBetaAccessRequest;

import java.util.List;
import java.util.Map;

public class DigitalLoasImpl implements DigitalLoas {

    private final CustomerPortal serviceManager;

    private final DigitalLoasClient serviceClient;

    public DigitalLoasImpl(DigitalLoasClient serviceClient, CustomerPortal serviceManager) {
        this.serviceClient = serviceClient;
        this.serviceManager = serviceManager;
    }

    public DigitalLoa create(DigitalLoaCreateRequest request) {
        return this.serviceClient.create(request);
    }

    public DigitalLoa findByUuid(String uuid) {
        return this.serviceClient.findByUuid(uuid);
    }

    public List<? extends DigitalLoa> search(DigitalLoaSearchRequest request) {
        return this.serviceClient.search(request, null, null, null);
    }

    public List<? extends DigitalLoa> search(DigitalLoaSearchRequest request, Integer offset, Integer limit, List<String> sort) {
        return this.serviceClient.search(request, offset, limit, sort);
    }

    public DigitalLoa update(String uuid, List<Map<String, Object>> operations) {
        return this.serviceClient.update(uuid, operations);
    }

    public Boolean cancel(String uuid) {
        return this.serviceClient.cancel(uuid);
    }

    public DigitalLoa performAction(String uuid, Map<String, Object> action) {
        return this.serviceClient.performAction(uuid, action);
    }

    public Boolean createRequest(Map<String, Object> request) {
        return this.serviceClient.createRequest(request);
    }

    public List<? extends DigitalLoaChange> findChangesByLoaUuid(String uuid) {
        return this.serviceClient.findChangesByLoaUuid(uuid);
    }

    public DigitalLoaChange findChangeByUuid(String uuid, String changeUuid) {
        return this.serviceClient.findChangeByUuid(uuid, changeUuid);
    }

    public List<? extends LoaCustomerOrganization> listOrganizations(String ibx) {
        return this.serviceClient.listOrganizations(ibx, null);
    }

    public List<? extends LoaCustomerOrganization> listOrganizations(String ibx, List<String> productTypes) {
        return this.serviceClient.listOrganizations(ibx, productTypes);
    }

    public PrivateBetaPermission isPrivateBetaAllowed() {
        return this.serviceClient.isPrivateBetaAllowed();
    }

    public Boolean createPrivateBetaAccessRequest(PrivateBetaAccessRequest request) {
        return this.serviceClient.createPrivateBetaAccessRequest(request);
    }

    public BetaTermsAgreement getBetaTermsAgreement() {
        return this.serviceClient.getBetaTermsAgreement();
    }

    public BetaTermsAgreement updateBetaTermsAgreement(Boolean agreementAccepted) {
        return this.serviceClient.updateBetaTermsAgreement(agreementAccepted);
    }
}
