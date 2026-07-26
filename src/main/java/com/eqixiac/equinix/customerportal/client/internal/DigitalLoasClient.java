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

package com.eqixiac.equinix.customerportal.client.internal;

import com.eqixiac.equinix.customerportal.model.BetaTermsAgreement;
import com.eqixiac.equinix.customerportal.model.DigitalLoa;
import com.eqixiac.equinix.customerportal.model.DigitalLoaChange;
import com.eqixiac.equinix.customerportal.model.LoaCustomerOrganization;
import com.eqixiac.equinix.customerportal.model.PrivateBetaPermission;
import com.eqixiac.equinix.customerportal.model.json.creators.DigitalLoaCreateRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.DigitalLoaSearchRequest;
import com.eqixiac.equinix.customerportal.model.json.creators.PrivateBetaAccessRequest;

import java.util.List;
import java.util.Map;

public interface DigitalLoasClient {

    DigitalLoa create(DigitalLoaCreateRequest request);

    DigitalLoa findByUuid(String uuid);

    List<? extends DigitalLoa> search(DigitalLoaSearchRequest request, Integer offset, Integer limit, List<String> sort);

    DigitalLoa update(String uuid, List<Map<String, Object>> operations);

    Boolean cancel(String uuid);

    DigitalLoa performAction(String uuid, Map<String, Object> action);

    Boolean createRequest(Map<String, Object> request);

    List<? extends DigitalLoaChange> findChangesByLoaUuid(String uuid);

    DigitalLoaChange findChangeByUuid(String uuid, String changeUuid);

    List<? extends LoaCustomerOrganization> listOrganizations(String ibx, List<String> productTypes);

    PrivateBetaPermission isPrivateBetaAllowed();

    Boolean createPrivateBetaAccessRequest(PrivateBetaAccessRequest request);

    BetaTermsAgreement getBetaTermsAgreement();

    BetaTermsAgreement updateBetaTermsAgreement(Boolean agreementAccepted);
}
