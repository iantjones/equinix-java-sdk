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

package api.equinix.javasdk.core.http.request;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.core.util.ModelUtils;
import lombok.Getter;

/**
 * Request carrier for offset/limit-paginated GET list operations. Holds the raw paging state
 * ({@code offset}/{@code pageSize}) as plain fields; {@link #setPagination()} writes them onto
 * the wire as {@code offset}/{@code limit} query parameters at dispatch.
 *
 * @param <T> the operation's model type
 * @author ianjones
 */
@Getter
public final class PaginatedRequest<T> extends EquinixRequest<T> {
    private int offset = 0;
    private int pageSize = Constants.PAGE_LIMIT;

    /**
     * Points the request's paging window at the page starting at {@code offset}, sized
     * {@code limit}. The paging pipeline calls this with the <em>server-reported</em> pagination
     * (next page: {@code serverOffset + serverLimit}; rollback after a failed fetch:
     * {@code serverOffset}). Advancing from the server-reported window — rather than by the
     * caller-requested page size — is what keeps paging correct when the server clamps the
     * requested limit (e.g. Fabric caps {@code limit} at 100): a caller-side
     * {@code offset += requestedPageSize} advance would skip every record between the clamped
     * and the requested page size.
     *
     * @param offset the zero-based index of the first record of the requested page (floored at 0)
     * @param limit the page size; non-positive values are ignored and the current page size kept
     */
    public void seekPage(long offset, long limit) {
        this.offset = Math.toIntExact(Math.max(0, offset));
        if (limit > 0) {
            this.pageSize = Math.toIntExact(limit);
        }
    }

    /**
     * Seeds {@link #offset}/{@link #pageSize} from any caller-supplied {@code offset}/{@code limit}
     * query parameters, so {@link #setPagination()} (invoked at dispatch) preserves the requested
     * window on the first page (subsequent pages are seeked from the server-reported pagination via
     * {@link #seekPage(long, long)}). Without this, {@code setPagination()} would overwrite
     * caller-supplied {@code offset}/{@code limit} with the defaults (offset 0,
     * limit {@code PAGE_LIMIT}).
     *
     * <p>The caller's offset is carried verbatim: paging is modelled as a raw offset, not a page
     * number, so offsets that are not a multiple of the limit (e.g. {@code offset=5, limit=100})
     * are preserved exactly rather than being quantized to the nearest page boundary.</p>
     */
    public void seedPagingFromQueryParams() {
        java.util.List<String> limitVals = getQueryParameters().get("limit");
        if (limitVals != null && !limitVals.isEmpty()) {
            try {
                int l = Integer.parseInt(limitVals.get(0));
                if (l > 0) {
                    this.pageSize = l;
                }
            } catch (NumberFormatException ignored) {
                // keep default page size
            }
        }
        java.util.List<String> offsetVals = getQueryParameters().get("offset");
        if (offsetVals != null && !offsetVals.isEmpty()) {
            try {
                int o = Integer.parseInt(offsetVals.get(0));
                if (o >= 0) {
                    this.offset = o;
                }
            } catch (NumberFormatException ignored) {
                // keep default offset
            }
        }
    }

    public void setPagination() {
        replaceQueryParameter("offset", ModelUtils.process(Integer.toString(this.offset)));
        replaceQueryParameter("limit", ModelUtils.process(Integer.toString(this.pageSize)));
    }

}
