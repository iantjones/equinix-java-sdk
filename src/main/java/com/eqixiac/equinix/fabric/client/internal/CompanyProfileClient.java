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

package com.eqixiac.equinix.fabric.client.internal;

import com.eqixiac.equinix.core.http.response.Page;
import com.eqixiac.equinix.core.http.response.PageablePost;
import com.eqixiac.equinix.fabric.model.CompanyProfile;
import com.eqixiac.equinix.fabric.model.CompanyServiceProfile;
import com.eqixiac.equinix.fabric.model.PrivateService;
import com.eqixiac.equinix.fabric.model.Tag;
import com.eqixiac.equinix.fabric.model.implementation.filter.FilterPropertyList;
import com.eqixiac.equinix.fabric.model.implementation.sort.SortPropertyList;
import com.eqixiac.equinix.fabric.model.json.CompanyProfileJson;
import com.eqixiac.equinix.fabric.model.json.creators.CompanyProfileCreatorJson;

import java.util.List;

public interface CompanyProfileClient<T> extends PageablePost<T> {

    Page<CompanyProfileJson> search(FilterPropertyList filter, SortPropertyList sort);

    CompanyProfileJson getByUuid(String uuid);

    CompanyProfileJson create(CompanyProfileCreatorJson creatorJson);

    CompanyProfileJson delete(String uuid);

    void attachServiceProfile(String companyProfileId, String serviceProfileId);

    void detachServiceProfile(String companyProfileId, String serviceProfileId);

    void attachTag(String companyProfileId, String tagId);

    void detachTag(String companyProfileId, String tagId);

    List<CompanyServiceProfile> getServiceProfiles(String companyProfileId);

    List<Tag> getTags(String companyProfileId);

    List<PrivateService> getPrivateServices(String companyProfileId);

    void attachPrivateService(String companyProfileId, String privateServiceId);

    void detachPrivateService(String companyProfileId, String privateServiceId);

    byte[] getLogo(String uuid);

    void deleteLogo(String uuid);

    CompanyProfileJson refresh(String uuid);
}
