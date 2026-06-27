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

package api.equinix.javasdk.networkedge.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.networkedge.model.ACLTemplate;
import api.equinix.javasdk.networkedge.model.SSHUser;
import api.equinix.javasdk.networkedge.model.VPN;
import api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator;
import api.equinix.javasdk.networkedge.model.json.creators.VPNOperator;

/**
 * Client interface for managing Access Control List (ACL) templates on Network Edge devices.
 * Provides operations to list, retrieve, and create ACL templates that define inbound and
 * outbound traffic rules.
 *
 * @author ianjones
 * @version $Id: $Id
 */
public interface ACLTemplates {

    /**
     * Lists available ACL Templates for the provided accountUcmId.
     *
     * @param accountUcmId the unique account identifier.
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<ACLTemplate> list(String accountUcmId);

    /**
     * Lists available ACL Templates.
     *
     * @return {@link api.equinix.javasdk.core.http.response.PaginatedList}
     */
    PaginatedList<ACLTemplate> list();

    /**
     * Gets the specified ACL Template for the provided accountUcmId.
     *
     * @param uuid the unique identifier of the ACL Template.
     * @param accountUcmId the unique account identifier.
     * @return {@link api.equinix.javasdk.networkedge.model.ACLTemplate}
     */
    ACLTemplate getByUuid(String uuid, String accountUcmId);

    /**
     * Gets the specified ACL Template.
     *
     * @param uuid the unique identifier of the ACL Template.
     * @return {@link api.equinix.javasdk.networkedge.model.ACLTemplate}
     */
    ACLTemplate getByUuid(String uuid);

    /**
     * Returns an instance of ACLTemplateBuilder for defining a new ACL Template.
     *
     * @param aclTemplateName the name of the new ACL Template.
     * @return {@link api.equinix.javasdk.networkedge.model.json.creators.ACLTemplateOperator.ACLTemplateBuilder}
     */
    ACLTemplateOperator.ACLTemplateBuilder define(String aclTemplateName);
}
