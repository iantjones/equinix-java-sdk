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

package com.eqixiac.equinix.iam.model;

/**
 * A grant associating a grantee (a principal, group or project) with an IAM access policy, as
 * returned by the grant operations ({@code GET .../accessPolicies/{accessPolicyId}/grants},
 * operationId {@code listGrants}; {@code POST .../grants}, operationId {@code addGrant}).
 *
 * <p>This is a read-only response view (spec schema {@code AccessPolicyGrant}).</p>
 */
public interface AccessPolicyGrant {

    /**
     * @return the unique identifier of this grant (e.g. {@code grant:ABCDEFGHIJ123})
     */
    String getGrantId();

    /**
     * @return the identifier of the access policy this grant references
     */
    String getAccessPolicyId();

    /**
     * @return the Equinix Resource Name (ERN) of the access policy this grant references
     */
    String getAccessPolicyErn();

    /**
     * @return the grantee (a principal, group or project identifier)
     */
    String getGrantee();

    /**
     * @return the principal that created this grant
     */
    String getCreatedBy();

    /**
     * @return the creation timestamp of this grant
     */
    String getCreatedAt();
}
