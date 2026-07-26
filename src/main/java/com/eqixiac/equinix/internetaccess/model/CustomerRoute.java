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

package com.eqixiac.equinix.internetaccess.model;

import com.eqixiac.equinix.internetaccess.enums.ImportPolicy;

/**
 * A customer-route allowance on an Equinix Internet Access (EIA) v1 default configuration: an
 * import policy plus the advertised prefix length. The dedicated-port and virtual-connection
 * routing configurations carry the same shape; this is the common read-only view of it.
 *
 * @author ianjones
 */
public interface CustomerRoute {

    /**
     * @return the import policy applied to advertised customer routes
     */
    ImportPolicy getImportPolicy();

    /**
     * @return the advertised prefix length
     */
    Integer getPrefixLength();
}
