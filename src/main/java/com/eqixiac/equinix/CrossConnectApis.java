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
 * The two API families for the Cross Connect product, from one place. Obtained from
 * {@link Equinix#crossConnects()}.
 *
 * <p>Equinix serves cross connects through two unrelated APIs. This type only groups the SDK's two
 * clients; it adds no behaviour and neither client moves.</p>
 *
 * @param orderApi  the cross connect order API (crossconnects v2): order, update and deinstall; the same
 *                  client as {@link Operations#crossConnects()}
 * @param fabricApi the Fabric v4 cross connect API (Beta): cross connect records and install schedules;
 *                  the same client as {@code fabric.beta().crossConnects()}
 */
public record CrossConnectApis(com.eqixiac.equinix.operations.client.CrossConnects orderApi,
                               com.eqixiac.equinix.fabric.beta.client.CrossConnects fabricApi) {
}
