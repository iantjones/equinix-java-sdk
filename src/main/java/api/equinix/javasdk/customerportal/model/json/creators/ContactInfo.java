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

import api.equinix.javasdk.customerportal.enums.PhonePreferenceToCall;
import api.equinix.javasdk.customerportal.enums.SmartHandsContactType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A contact on a smart hands order. Ordering and notification contacts are always registered
 * Equinix customer-portal users referenced by {@code userName}; when a userName is supplied
 * all other attributes apart from {@code contactType} are ignored. Technical contacts are
 * specified inline with name and phone details.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactInfo {

    @JsonProperty("contactType")
    private SmartHandsContactType contactType;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("workPhoneCountryCode")
    private String workPhoneCountryCode;

    @JsonProperty("workPhone")
    private String workPhone;

    @JsonProperty("workPhonePrefToCall")
    private PhonePreferenceToCall workPhonePrefToCall;

    @JsonProperty("mobilePhoneCountryCode")
    private String mobilePhoneCountryCode;

    @JsonProperty("mobilePhone")
    private String mobilePhone;

    @JsonProperty("mobilePhonePrefToCall")
    private PhonePreferenceToCall mobilePhonePrefToCall;

    @JsonProperty("workPhoneTimeZone")
    private String workPhoneTimeZone;

    @JsonProperty("mobilePhoneTimeZone")
    private String mobilePhoneTimeZone;

    private ContactInfo(SmartHandsContactType contactType) {
        this.contactType = contactType;
    }

    /**
     * Builds a registered-user contact (ORDERING or NOTIFICATION), referenced by user name.
     *
     * @param contactType the contact type
     * @param userName    the registered customer-portal user name
     * @return the contact
     */
    public static ContactInfo registered(SmartHandsContactType contactType, String userName) {
        ContactInfo contact = new ContactInfo(contactType);
        contact.userName = userName;
        return contact;
    }

    /**
     * Builds a technical contact specified inline with name and work phone details.
     *
     * @param name                full name of the contact
     * @param workPhone           primary phone of the contact
     * @param workPhonePrefToCall preferred time window to call
     * @return the contact
     */
    public static ContactInfo technical(String name, String workPhone, PhonePreferenceToCall workPhonePrefToCall) {
        ContactInfo contact = new ContactInfo(SmartHandsContactType.TECHNICAL);
        contact.name = name;
        contact.workPhone = workPhone;
        contact.workPhonePrefToCall = workPhonePrefToCall;
        return contact;
    }

    public ContactInfo email(String email) {
        this.email = email;
        return this;
    }

    public ContactInfo workPhoneCountryCode(String workPhoneCountryCode) {
        this.workPhoneCountryCode = workPhoneCountryCode;
        return this;
    }

    public ContactInfo mobilePhone(String mobilePhoneCountryCode, String mobilePhone, PhonePreferenceToCall mobilePhonePrefToCall) {
        this.mobilePhoneCountryCode = mobilePhoneCountryCode;
        this.mobilePhone = mobilePhone;
        this.mobilePhonePrefToCall = mobilePhonePrefToCall;
        return this;
    }

    public ContactInfo workPhoneTimeZone(String workPhoneTimeZone) {
        this.workPhoneTimeZone = workPhoneTimeZone;
        return this;
    }

    public ContactInfo mobilePhoneTimeZone(String mobilePhoneTimeZone) {
        this.mobilePhoneTimeZone = mobilePhoneTimeZone;
        return this;
    }
}
