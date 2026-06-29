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

package api.equinix.javasdk.internetaccess.enums;

import api.equinix.javasdk.core.model.APIParam;

/**
 * Signatory of the order signature attached to an Equinix Internet Access (EIA) v2 service order.
 *
 * <ul>
 *   <li>{@code SELF} — the requesting user signs the order.</li>
 *   <li>{@code DELEGATE} — the signature request is sent to a delegate.</li>
 *   <li>{@code SUPPORT} — Equinix support handles signing.</li>
 * </ul>
 */
public enum OrderSignatory implements APIParam {
    SELF,
    DELEGATE,
    SUPPORT
}
