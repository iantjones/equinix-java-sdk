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

package com.eqixiac.equinix.fabric.model.json;

import com.eqixiac.equinix.fabric.model.CompanyProfile;
import com.eqixiac.equinix.fabric.model.implementation.ChangeLog;
import com.eqixiac.equinix.fabric.model.implementation.CompanyLogo;
import com.eqixiac.equinix.fabric.model.implementation.CompanyMetro;
import com.eqixiac.equinix.fabric.model.implementation.CompanyProfileAccount;
import com.eqixiac.equinix.fabric.model.implementation.CompanyProfileChange;
import com.eqixiac.equinix.fabric.model.implementation.CompanyServiceProfile;
import com.eqixiac.equinix.fabric.model.implementation.Notification;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyProfileJson {

    @Getter static TypeReference<List<CompanyProfileJson>> listTypeRef = new TypeReference<>() {};


    @JsonProperty("href")
    private String href;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("description")
    private String description;

    @JsonProperty("state")
    private String state;

    @JsonProperty("account")
    private CompanyProfileAccount account;

    @JsonProperty("metros")
    private List<CompanyMetro> metros;

    @JsonProperty("logo")
    private CompanyLogo logo;

    @JsonProperty("tags")
    private List<TagJson> tags;

    @JsonProperty("serviceProfiles")
    private List<CompanyServiceProfile> serviceProfiles;

    @JsonProperty("privateServices")
    private List<PrivateServiceJson> privateServices;

    @JsonProperty("notifications")
    private List<Notification> notifications;

    @JsonProperty("webUrl")
    private String webUrl;

    @JsonProperty("contactUrl")
    private String contactUrl;

    @JsonProperty("change")
    private CompanyProfileChange change;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;
}
