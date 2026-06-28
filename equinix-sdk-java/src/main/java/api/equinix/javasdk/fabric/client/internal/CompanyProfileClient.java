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

package api.equinix.javasdk.fabric.client.internal;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.core.http.response.PageablePost;
import api.equinix.javasdk.fabric.model.CompanyProfile;
import api.equinix.javasdk.fabric.model.implementation.filter.FilterPropertyList;
import api.equinix.javasdk.fabric.model.implementation.sort.SortPropertyList;
import api.equinix.javasdk.fabric.model.json.CompanyProfileJson;
import api.equinix.javasdk.fabric.model.json.creators.CompanyProfileCreatorJson;

public interface CompanyProfileClient<T> extends PageablePost<T> {

    Page<CompanyProfile, CompanyProfileJson> search(FilterPropertyList filter, SortPropertyList sort);

    CompanyProfileJson getByUuid(String uuid);

    CompanyProfileJson create(CompanyProfileCreatorJson creatorJson);

    CompanyProfileJson delete(String uuid);

    void attachServiceProfile(String companyProfileId, String serviceProfileId);

    void detachServiceProfile(String companyProfileId, String serviceProfileId);

    void attachTag(String companyProfileId, String tagId);

    void detachTag(String companyProfileId, String tagId);

    CompanyProfileJson refresh(String uuid);
}
