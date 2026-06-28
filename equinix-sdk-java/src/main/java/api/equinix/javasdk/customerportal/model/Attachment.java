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

package api.equinix.javasdk.customerportal.model;

/**
 * Metadata for a file attachment in the Equinix Customer Portal (Attachments v1 {@code attachment}).
 */
public interface Attachment {

    /**
     * Returns the unique identifier of the attachment.
     *
     * @return the attachment id (UUID)
     */
    String getAttachmentId();

    /**
     * Returns the file name of the attachment.
     *
     * @return the attachment name
     */
    String getAttachmentName();

    /**
     * Returns the document extension / content type of the attachment.
     *
     * @return the attachment type
     */
    String getAttachmentType();

    /**
     * Returns the size of the attachment in bytes.
     *
     * @return the attachment size
     */
    Long getAttachmentSize();

    /**
     * Returns the created date of the attachment.
     *
     * @return the created date
     */
    String getCreatedDate();

    /**
     * Returns the user key of the author of the attachment.
     *
     * @return the creator
     */
    String getCreatedBy();

    /**
     * Returns the last accessed/updated date of the attachment.
     *
     * @return the last updated date
     */
    String getLastUpdatedDate();

    /**
     * Deletes this attachment.
     *
     * @return {@code true} if the attachment was deleted
     */
    Boolean delete();

    /**
     * Refreshes this attachment's metadata from the API.
     */
    void refresh();
}
