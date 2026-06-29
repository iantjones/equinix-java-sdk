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
 * <p>PaginatedRequest class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@NoArgsConstructor
public class PaginatedRequest<T> extends EquinixRequest<T> {
    protected Integer pageNumber = 0;
    protected Integer pageSize = Constants.PAGE_LIMIT;

    /**
     * <p>nextPage.</p>
     */
    public void nextPage() {
        this.pageNumber++;
    }

    /**
     * Seeds {@link #pageNumber}/{@link #pageSize} from any caller-supplied {@code offset}/{@code limit}
     * query parameters, so {@link #setPagination()} (invoked at dispatch) preserves the requested window
     * and {@link #nextPage()} advances by the requested page size. Without this, {@code setPagination()}
     * would overwrite caller-supplied {@code offset}/{@code limit} with the defaults (offset 0,
     * limit {@code PAGE_LIMIT}).
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
        if (offsetVals != null && !offsetVals.isEmpty() && this.pageSize > 0) {
            try {
                int o = Integer.parseInt(offsetVals.get(0));
                if (o >= 0) {
                    this.pageNumber = o / this.pageSize;
                }
            } catch (NumberFormatException ignored) {
                // keep default page number
            }
        }
    }

    /**
     * <p>setPagination.</p>
     */
    public void setPagination() {
        replaceQueryParameter("offset", ModelUtils.process(Integer.toString(this.pageNumber * this.pageSize)));
        replaceQueryParameter("limit", ModelUtils.process(this.pageSize.toString()));
    }

}
