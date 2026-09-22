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

package com.eqixiac.equinix.core.model.multicloud;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Type of a specification connection ({@code common.yaml#/ConnectionType}); carried by
 * {@code Connection.connectionType}. Constant names equal the wire values.
 *
 * <p>Named {@code MulticloudConnectionType} because {@code fabric.enums.ConnectionType} already
 * names the Fabric v4 virtual-connection type ({@code EVPL_VC}, {@code IP_VC}, ...). The two enums
 * share no values.</p>
 *
 * <p><b>Beta</b>: derived from the open specification at commit {@code bbfc763} (see the package
 * documentation). The enum was added to the specification between commits {@code 8173027} and
 * {@code bbfc763}.</p>
 *
 * <p>The specification defaults an absent {@code connectionType} to
 * {@link #CONNECTION_TYPE_STANDARD}; see {@link #SPEC_DEFAULT}. Jackson leaves an absent property
 * {@code null}; applying the default is the reader's responsibility.</p>
 *
 * <p>{@link #UNKNOWN} is a read-side fallback for values added after this SDK release; never send
 * it.</p>
 *
 * @author ianjones
 */
public enum MulticloudConnectionType {

    /** An ordinary customer connection. The only type a customer creates. */
    CONNECTION_TYPE_STANDARD,

    /**
     * Reserved for provider development teams (testing and canary workflows). May be provisioned
     * below the minimum supported size, for example 50 Mbps. Its bandwidth does not count against
     * interconnect or environment capacity totals. A provider may enforce a quota on active test
     * connections.
     */
    CONNECTION_TYPE_TEST,

    /**
     * Reserved for active monitoring sessions between providers. Does not count against
     * interconnect or environment capacity totals. Quota: two per interconnect, one created by
     * each provider. Has no customer resource and no activation key.
     */
    CONNECTION_TYPE_MONITORING,

    /** Read-side fallback for a value this enum does not list. */
    UNKNOWN;

    /** The value the specification applies when {@code connectionType} is absent. */
    public static final MulticloudConnectionType SPEC_DEFAULT = CONNECTION_TYPE_STANDARD;

    /**
     * Parses a wire value. Surrounding whitespace is trimmed and case is ignored.
     *
     * @param value the wire value, for example {@code CONNECTION_TYPE_STANDARD}; may be null
     * @return the matching constant, or {@link #UNKNOWN} when {@code value} is null, blank or not
     *         listed. A null input does not return {@link #SPEC_DEFAULT}: a direct caller passing
     *         null has not shown that the property was absent from a payload
     */
    @JsonCreator
    public static MulticloudConnectionType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return MulticloudConnectionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
