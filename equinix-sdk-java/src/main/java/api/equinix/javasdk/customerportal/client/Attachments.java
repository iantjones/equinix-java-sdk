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

package api.equinix.javasdk.customerportal.client;

import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.model.Attachment;
import api.equinix.javasdk.customerportal.model.json.creators.AttachmentOperator;

/**
 * Client interface for managing file attachments in the Equinix Customer Portal.
 * Provides operations to list, retrieve, and upload file attachments associated
 * with support cases, orders, and other portal resources.
 */
public interface Attachments {

    /**
     * Lists all attachments for the current account.
     *
     * @return a paginated list of attachments
     */
    PaginatedList<Attachment> list();

    /**
     * Retrieves a specific attachment by its unique identifier.
     *
     * @param uuid the unique identifier of the attachment
     * @return the matching attachment
     */
    Attachment getByUuid(String uuid);

    /**
     * Returns a builder for defining a new attachment upload.
     *
     * @return a new AttachmentBuilder instance
     */
    AttachmentOperator.AttachmentBuilder define();
}
