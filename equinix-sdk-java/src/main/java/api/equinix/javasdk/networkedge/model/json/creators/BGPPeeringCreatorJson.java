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

package api.equinix.javasdk.networkedge.model.json.creators;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * <p>BGPPeeringCreatorJson class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
@Getter
public class BGPPeeringCreatorJson {

    @JsonProperty("connectionUuid")
    private String connectionUuid;

    @JsonProperty("localIpAddress")
    private String localIpAddress;

    @JsonProperty("remoteIpAddress")
    private String remoteIpAddress;

    @JsonProperty("localAsn")
    private Integer localAsn;

    @JsonProperty("remoteAsn")
    private Integer remoteAsn;

    @JsonProperty("authenticationKey")
    private String authenticationKey;

    BGPPeeringCreatorJson(BGPPeeringOperator.BGPPeeringBuilder bgpPeeringBuilder) {
        this.connectionUuid = bgpPeeringBuilder.getConnectionUuid();
        this.localIpAddress = bgpPeeringBuilder.getLocalIpAddress();
        this.remoteIpAddress = bgpPeeringBuilder.getRemoteIpAddress();
        this.localAsn = bgpPeeringBuilder.getLocalAsn();
        this.remoteAsn = bgpPeeringBuilder.getRemoteAsn();
        this.authenticationKey = bgpPeeringBuilder.getAuthenticationKey();
    }
}
