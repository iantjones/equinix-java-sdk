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


package com.eqixiac.equinix.design.plan;

/**
 * A routing protocol as read from Fabric.
 *
 * @param uuid            the Fabric uuid
 * @param name            the name
 * @param type            {@code DIRECT} or {@code BGP}
 * @param state           the state name, or {@code null}
 * @param connectionUuid  the parent connection uuid
 * @param equinixIfaceIp  DIRECT: the Equinix interface address
 * @param customerPeerIp  BGP: the customer peer address
 * @param customerAsn     BGP: the customer ASN
 * @param bfdEnabled      BGP: whether BFD is on, or {@code null} when not returned
 */
public record LiveRoutingProtocol(String uuid, String name, String type, String state, String connectionUuid,
                                  String equinixIfaceIp, String customerPeerIp, Long customerAsn,
                                  Boolean bfdEnabled) {
}
