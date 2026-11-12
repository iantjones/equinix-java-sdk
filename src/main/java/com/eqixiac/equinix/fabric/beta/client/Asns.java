/*
 * Copyright 2026 Ian Jones. All Rights Reserved.
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

package com.eqixiac.equinix.fabric.beta.client;

import com.eqixiac.equinix.fabric.beta.Beta;
import com.eqixiac.equinix.core.http.request.PatchOperation;
import com.eqixiac.equinix.core.http.response.PaginatedFilteredList;
import com.eqixiac.equinix.fabric.beta.model.Asn;
import com.eqixiac.equinix.fabric.beta.model.CreateAsnRequest;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import java.util.List;

/**
 * Autonomous System Numbers (ASNs): public and private ASN records (5 operations).
 *
 * <p>Obtain it from {@code fabric.beta().asns()}. Every operation maps one Fabric v4 endpoint; the
 * Javadoc of each method names the path and the spec operation id.</p>
 * <p><strong>Beta</strong>: Fabric v4 4.34.0 (spec read 2026-11-11).</p>
 * <p>Generated from the Fabric v4 OpenAPI spec by the fabric-beta generator; regenerate rather than hand-edit.</p>
 */
@Beta
public interface Asns {

    /**
     * Create ASN: {@code POST /fabric/v4/asns} (operation {@code createAsn}).
     * <p>Create a new Public or Private ASN record</p>
     *
     * @param request the {@code CreateAsnRequest} request body
     * @return the {@code Asn} the API returns
     */
    Asn create(CreateAsnRequest request);

    /**
     * Search ASNs: {@code POST /fabric/v4/asns/search} (operation {@code searchAsns}).
     * <p>Search for ASN records based on criteria</p>
     *
     * @param filter the filter ({@code null} for none)
     * @param sort the sort order ({@code null} for the service default)
     * @return a lazily paged list of {@code Asn}
     */
    PaginatedFilteredList<Asn> search(FilterPropertyList filter, SortPropertyList sort);

    /**
     * Search ASNs: {@code POST /fabric/v4/asns/search} (operation {@code searchAsns}).
     * <p>Search for ASN records based on criteria</p>
     *
     * @param filter the filter ({@code null} for none)
     * @return a lazily paged list of {@code Asn}
     */
    PaginatedFilteredList<Asn> search(FilterPropertyList filter);

    /**
     * Search ASNs: {@code POST /fabric/v4/asns/search} (operation {@code searchAsns}).
     * <p>Search for ASN records based on criteria</p>
     *
     * @return a lazily paged list of {@code Asn}
     */
    PaginatedFilteredList<Asn> search();

    /**
     * Get ASN: {@code GET /fabric/v4/asns/{asnId}} (operation {@code getAsnById}).
     * <p>Retrieve ASN record by UUID</p>
     *
     * @param asnId the {@code asnId} path parameter
     * @return the {@code Asn} the API returns
     */
    Asn getByUuid(String asnId);

    /**
     * Update ASN: {@code PATCH /fabric/v4/asns/{asnId}} (operation {@code patchAsnById}).
     * <p>Update ASN record</p>
     * <p>Sent as {@code application/json}, per the spec. Patchable paths named by the spec: {@code /ownershipChallengeDetails/signature}.</p>
     *
     * @param asnId the {@code asnId} path parameter
     * @param operations the patch operations ({@code op}/{@code path}/{@code value})
     * @return the {@code Asn} the API returns
     */
    Asn update(String asnId, List<PatchOperation> operations);

    /**
     * Delete ASN: {@code DELETE /fabric/v4/asns/{asnId}} (operation {@code deleteAsnById}).
     * <p>Deprovision ASN record by UUID</p>
     *
     * @param asnId the {@code asnId} path parameter
     * @return the {@code Asn} the API returns
     */
    Asn delete(String asnId);
}
