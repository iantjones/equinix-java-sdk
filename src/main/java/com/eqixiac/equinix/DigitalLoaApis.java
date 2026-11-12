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
package com.eqixiac.equinix;


/**
 * The two API families for the Digital LOA product, from one place. Obtained from
 * {@link Equinix#digitalLoas()}.
 *
 * <p>Equinix serves Digital Letters of Authorization through two unrelated APIs. This type only groups
 * the SDK's two clients; it adds no behaviour and neither client moves.</p>
 *
 * @param diloaApi  the Digital LOA v1 API; the same client as {@link Operations#digitalLoas()}
 * @param fabricApi the Fabric v4 LOA API (Beta): LOAs with their actions, notes and consumers; the same
 *                  client as {@code fabric.beta().loas()}
 */
public record DigitalLoaApis(com.eqixiac.equinix.operations.client.DigitalLoas diloaApi,
                             com.eqixiac.equinix.fabric.beta.client.Loas fabricApi) {
}
