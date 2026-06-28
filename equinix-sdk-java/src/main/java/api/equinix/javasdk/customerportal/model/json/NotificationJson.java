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

package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.customerportal.model.Notification;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

/**
 * JSON model for an IBX or network notification (Notifications v1 {@code ibx-notification} /
 * {@code network-notification}).
 *
 * <p>One model carries the union of both notification shapes: {@code productTypes} is populated
 * only for network notifications and {@code emails} only when a notification is fetched by id, so
 * the same class deserializes the IBX get, the network get, and both search-response summaries.</p>
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationJson implements Notification {

    @Getter static TypeReference<List<NotificationJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("startTimestamp")
    private String startTimestamp;

    @JsonProperty("endTimestamp")
    private String endTimestamp;

    @JsonProperty("ibxs")
    private List<String> ibxs;

    @JsonProperty("status")
    private String status;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("productTypes")
    private List<String> productTypes;

    @JsonProperty("emails")
    private List<NotificationEmailJson> emails;
}
