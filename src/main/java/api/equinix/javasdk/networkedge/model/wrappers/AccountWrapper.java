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

package api.equinix.javasdk.networkedge.model.wrappers;

import api.equinix.javasdk.core.http.response.Pageable;
import api.equinix.javasdk.core.model.ResourceImpl;
import api.equinix.javasdk.core.enums.MetroCode;
import api.equinix.javasdk.networkedge.model.Account;
import api.equinix.javasdk.networkedge.model.json.AccountJson;
import lombok.Getter;

import java.util.ArrayList;

/**
 * <p>AccountWrapper class.</p>
 *
 * @author ianjones
 * @version $Id: $Id
 */
public class AccountWrapper extends ResourceImpl<Account> implements Account {

    private AccountJson jsonObject;
    @Getter
    private final Pageable<Account> serviceClient;

    /**
     * <p>Constructor for AccountWrapper.</p>
     *
     * @param accountJson a {@link api.equinix.javasdk.networkedge.model.json.AccountJson} object.
     * @param serviceClient a {@link api.equinix.javasdk.core.http.response.Pageable} object.
     */
    public AccountWrapper(AccountJson accountJson, Pageable<Account> serviceClient) {
        this.jsonObject = accountJson;
        this.serviceClient = serviceClient;
    }

    /**
     * <p>getAccountName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAccountName() {
        return  this.jsonObject.getAccountName();
    }

    /**
     * <p>getAccountNumber.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getAccountNumber() {
        return  this.jsonObject.getAccountNumber();
    }

    /**
     * <p>getAccountUcmId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAccountUcmId() {
        return  this.jsonObject.getAccountUcmId();
    }

    /**
     * <p>getAccountStatus.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAccountStatus() {
        return  this.jsonObject.getAccountStatus();
    }

    /**
     * <p>getPortalOrgId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPortalOrgId() {
        return  this.jsonObject.getPortalOrgId();
    }

    /**
     * <p>getPortalOrgName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPortalOrgName() {
        return  this.jsonObject.getPortalOrgName();
    }

    /**
     * <p>getReferenceId.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReferenceId() {
        return  this.jsonObject.getReferenceId();
    }

    /**
     * <p>getMetros.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<MetroCode> getMetros() {
        return  this.jsonObject.getMetros();
    }

    /**
     * <p>getSiblingCustOrgFlag.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getSiblingCustOrgFlag() {
        return  this.jsonObject.getSiblingCustOrgFlag();
    }

    /**
     * <p>getCreditHold.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getCreditHold() {
        return  this.jsonObject.getCreditHold();
    }
}
