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

package com.eqixiac.equinix.operations.model;

import com.eqixiac.equinix.operations.enums.OrderHistoryLinkRel;
import com.eqixiac.equinix.operations.model.implementation.OrderHistoryLink;

import java.util.List;

/**
 * One page of an order history search ({@code order-history-response}): the {@code content}
 * records, the response-level {@code links} and the {@code page} metadata.
 *
 * <p>The envelope {@code {links, content, page{size, totalElements, totalPages, number}}} is the
 * spec shape and the live shape (observed 2026-11-11). To read the next page, repeat the search
 * with {@code PageRequest(getNumber() + 1, getSize())} while {@link #hasNext()} is true.</p>
 */
public interface OrderHistoryPage {

    /**
     * Returns the order records on this page.
     *
     * @return the records, never {@code null}
     */
    List<? extends OrderHistoryItem> getContent();

    /**
     * Returns the response-level links ({@code self}, {@code prev}, {@code next}).
     *
     * @return the links, never {@code null}
     */
    List<OrderHistoryLink> getLinks();

    /**
     * Returns the page size the service applied.
     *
     * @return the page size, or {@code null} when the response carried no page block
     */
    Integer getSize();

    /**
     * Returns the zero-based number of this page.
     *
     * @return the page number, or {@code null} when the response carried no page block
     */
    Integer getNumber();

    /**
     * Returns the total number of matching records.
     *
     * @return the total, or {@code null} when the response carried no page block
     */
    Long getTotalElements();

    /**
     * Returns the total number of pages.
     *
     * @return the total, or {@code null} when the response carried no page block
     */
    Integer getTotalPages();

    /**
     * Returns whether a later page exists: a {@code next} link is present, or the page number is
     * below {@code totalPages - 1}.
     *
     * @return {@code true} when another page can be requested
     */
    default boolean hasNext() {
        for (OrderHistoryLink link : getLinks()) {
            if (link != null && link.getRel() == OrderHistoryLinkRel.NEXT) {
                return true;
            }
        }
        return getNumber() != null && getTotalPages() != null && getNumber() < getTotalPages() - 1;
    }
}
