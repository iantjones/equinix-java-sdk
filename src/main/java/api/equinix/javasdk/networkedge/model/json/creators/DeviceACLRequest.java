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

import api.equinix.javasdk.networkedge.enums.ACLInterfaceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Request body for associating ACL templates with a virtual device. Holds an array of
 * {@link api.equinix.javasdk.networkedge.model.json.creators.ACLDetail} entries, one per
 * interface ({@code MGMT}/{@code WAN}) the template should be provisioned on. Used by both the
 * add (POST) and update (PATCH) device-ACL operations.</p>
 *
 * <pre>{@code
 * DeviceACLRequest request = new DeviceACLRequest()
 *     .withAcl(ACLInterfaceType.WAN, "be7ef79e-31e7-4769-be5b-e192496f48aa")
 *     .withAcl(ACLInterfaceType.MGMT, "ce7ef79e-31e7-4769-be5b-e192496f48ab");
 * }</pre>
 *
 * @author ianjones
 */
@Getter
public class DeviceACLRequest {

    @JsonProperty("aclDetails")
    private final List<ACLDetail> aclDetails = new ArrayList<>();

    /**
     * <p>Adds an ACL association to this request.</p>
     *
     * @param interfaceType the {@link api.equinix.javasdk.networkedge.enums.ACLInterfaceType} the ACL applies to.
     * @param aclTemplateUuid the unique identifier of the ACL template to associate.
     * @return this {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest} for chaining.
     */
    public DeviceACLRequest withAcl(ACLInterfaceType interfaceType, String aclTemplateUuid) {
        this.aclDetails.add(new ACLDetail(interfaceType, aclTemplateUuid));
        return this;
    }

    /**
     * <p>Adds a pre-built ACL association to this request.</p>
     *
     * @param aclDetail the {@link api.equinix.javasdk.networkedge.model.json.creators.ACLDetail} to add.
     * @return this {@link api.equinix.javasdk.networkedge.model.json.creators.DeviceACLRequest} for chaining.
     */
    public DeviceACLRequest withAcl(ACLDetail aclDetail) {
        this.aclDetails.add(aclDetail);
        return this;
    }
}
