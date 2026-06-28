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

package api.equinix.javasdk.customerportal.model.json.creators;

import api.equinix.javasdk.customerportal.enums.AttachmentSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Reference to a previously uploaded attachment ({@code Attachment}) on a support case create or
 * note request. Upload the file first via the Attachments API and reference the returned id and
 * name here. The {@code source} (whether the attachment came from the customer or Equinix) is
 * optional.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportCaseAttachment {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("source")
    private final AttachmentSource source;

    public SupportCaseAttachment(String id, String name) {
        this(id, name, null);
    }

    public SupportCaseAttachment(String id, String name, AttachmentSource source) {
        this.id = id;
        this.name = name;
        this.source = source;
    }
}
