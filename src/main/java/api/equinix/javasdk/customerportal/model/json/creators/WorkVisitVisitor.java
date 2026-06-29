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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * A work-visit visitor ({@code anyOf} {@code RegisteredUser} / {@code NonRegisteredUser_Visitor}
 * in the work-visits v2 spec). A visitor is either:
 *
 * <ul>
 *     <li>a set of registered users referenced by username
 *     ({@link #registered(List)}), or</li>
 *     <li>a non-registered visitor specified inline with name and company
 *     ({@link #nonRegistered(String, String, String)}).</li>
 * </ul>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkVisitVisitor {

    // RegisteredUser variant
    @JsonProperty("registeredUsers")
    private List<String> registeredUsers;

    // NonRegisteredUser_Visitor variant
    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("companyName")
    private String companyName;

    @JsonProperty("details")
    private List<ContactDetail> details;

    private WorkVisitVisitor() {
    }

    /**
     * A visitor entry that references registered users by username.
     *
     * @param registeredUsers the usernames of registered users (max 10)
     * @return the visitor
     */
    public static WorkVisitVisitor registered(List<String> registeredUsers) {
        WorkVisitVisitor visitor = new WorkVisitVisitor();
        visitor.registeredUsers = registeredUsers;
        return visitor;
    }

    /**
     * A non-registered visitor specified inline.
     *
     * @param firstName   the visitor's first name (required)
     * @param lastName    the visitor's last name (required)
     * @param companyName the visitor's company name (required)
     * @return the visitor
     */
    public static WorkVisitVisitor nonRegistered(String firstName, String lastName, String companyName) {
        WorkVisitVisitor visitor = new WorkVisitVisitor();
        visitor.firstName = firstName;
        visitor.lastName = lastName;
        visitor.companyName = companyName;
        return visitor;
    }

    /**
     * Sets the contact details for a non-registered visitor (up to two).
     *
     * @param details the contact details
     * @return this visitor
     */
    public WorkVisitVisitor details(List<ContactDetail> details) {
        this.details = details;
        return this;
    }

    /**
     * A contact detail for a non-registered visitor
     * ({@code VisitorContactsRequestDetails} in the work-visits v2 spec).
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactDetail {

        /**
         * Communications type of the contact value.
         */
        public enum Type {
            MOBILE,
            EMAIL
        }

        @JsonProperty("type")
        private final Type type;

        @JsonProperty("value")
        private final String value;

        /**
         * Creates a visitor contact detail.
         *
         * @param type  the communications type (required)
         * @param value the email address or mobile number for the type (required)
         */
        public ContactDetail(Type type, String value) {
            this.type = type;
            this.value = value;
        }
    }
}
