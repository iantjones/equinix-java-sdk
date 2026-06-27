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
     * <p>setPagination.</p>
     */
    public void setPagination() {
        replaceQueryParameter("offset", ModelUtils.process(Integer.toString(this.pageNumber * this.pageSize)));
        replaceQueryParameter("limit", ModelUtils.process(this.pageSize.toString()));
    }

}
