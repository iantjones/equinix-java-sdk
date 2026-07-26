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

package com.eqixiac.equinix.customerportal.client.internal;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.Pageable;
import com.eqixiac.equinix.customerportal.enums.AttachmentPurpose;
import com.eqixiac.equinix.customerportal.model.Attachment;
import com.eqixiac.equinix.customerportal.model.json.AttachmentJson;

import java.util.List;

public interface AttachmentClient<T> extends Pageable<T> {

    Page<AttachmentJson> list();

    Page<AttachmentJson> list(List<String> attachmentIds);

    AttachmentJson getByUuid(String uuid);

    AttachmentJson upload(byte[] fileBytes, String fileName, AttachmentPurpose purpose);

    byte[] download(String attachmentId);

    AttachmentJson delete(String uuid);

    AttachmentJson refresh(String uuid);
}
