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

package api.equinix.javasdk.core.http.response;

import api.equinix.javasdk.core.http.request.EquinixRequest;
import api.equinix.javasdk.core.http.request.PaginatedRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

/**
 * <p>PaginatedList class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@Setter
@NoArgsConstructor
public class PaginatedList<T> extends ArrayList<T> {

    private Pageable<T> pageableClient;
    private EquinixRequest<T> equinixRequest;
    private EquinixResponse<T> equinixResponse;
    private Pagination pagination;

    /**
     * <p>Constructor for PaginatedList.</p>
     *
     * @param initialItems a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     * @param pageableClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     * @param equinixRequest a {@link api.equinix.javasdk.core.http.request.EquinixRequest} object.
     * @param equinixResponse a {@link api.equinix.javasdk.core.http.response.EquinixResponse} object.
     * @param pagination a {@link api.equinix.javasdk.core.http.response.Pagination} object.
     */
    public PaginatedList(PaginatedList<T> initialItems, Pageable<T> pageableClient,
                         EquinixRequest<T> equinixRequest, EquinixResponse<T> equinixResponse, Pagination pagination) {

        this.addAll(initialItems);
        this.pageableClient = pageableClient;
        this.equinixRequest = equinixRequest;
        this.equinixResponse = equinixResponse;
        this.pagination = pagination;
    }

    private PaginatedList<T> fetchNextPage() {
        ((PaginatedRequest<T>)equinixRequest).nextPage();
        return (PaginatedList<T>) this.pageableClient.nextPage((PaginatedRequest<T>)equinixRequest);
    }

    private void loadNextPage() {
        PaginatedList<T> primaryObjectList = fetchNextPage();
        this.addAll(primaryObjectList);
        this.equinixRequest = primaryObjectList.getEquinixRequest();
        this.equinixResponse = primaryObjectList.getEquinixResponse();
        this.pagination = primaryObjectList.getPagination();
    }

    /**
     * <p>hasNextPage.</p>
     *
     * @return a boolean.
     */
    public boolean hasNextPage() {
        return !this.pagination.getIsLastPage();
    }

    /**
     * <p>next.</p>
     */
    public void next() {
        if (hasNextPage()) {
            loadNextPage();
        }
    }

    /**
     * <p>loadAll.</p>
     *
     * @return a {@link api.equinix.javasdk.core.http.response.PaginatedList} object.
     */
    public PaginatedList<T> loadAll() {
        while (hasNextPage()) {
            loadNextPage();
        }

        return this;
    }
}
