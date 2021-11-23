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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>Pagination class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
@Setter(AccessLevel.PRIVATE)
public class Pagination {

    private Integer offset;

    private Integer limit;

    private Integer total;

    private String previous;

    private String next;

    /**
     * <p>getPageNumber.</p>
     *
     * @return a int.
     */
    public int getPageNumber() {
        return getOffset() / getLimit();
    }

    /**
     * <p>getPageSize.</p>
     *
     * @return a int.
     */
    public int getPageSize() {
        return getLimit();
    }

    /**
     * <p>getIsFirstPage.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getIsFirstPage() {
        return getPageNumber() == 0;
    }

    /**
     * <p>getIsLastPage.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getIsLastPage() {
        return (getPageNumber() + 1) * getLimit() >= getTotal();
    }
}
