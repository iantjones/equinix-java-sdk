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
import lombok.NoArgsConstructor;

/**
 *
 * @author ianjones
 */
@Getter
@NoArgsConstructor
public class PaginatedRequest<T> extends EquinixRequest<T> {
    protected Integer offset = 0;
    protected Integer pageSize = Constants.PAGE_LIMIT;

    public void nextPage() {
        this.offset += this.pageSize;
    }

    /**
     * Seeds {@link #offset}/{@link #pageSize} from any caller-supplied {@code offset}/{@code limit}
     * query parameters, so {@link #setPagination()} (invoked at dispatch) preserves the requested window
     * and {@link #nextPage()} advances by the requested page size. Without this, {@code setPagination()}
     * would overwrite caller-supplied {@code offset}/{@code limit} with the defaults (offset 0,
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
        replaceQueryParameter("offset", ModelUtils.process(this.offset.toString()));
        replaceQueryParameter("limit", ModelUtils.process(this.pageSize.toString()));
    }

}
