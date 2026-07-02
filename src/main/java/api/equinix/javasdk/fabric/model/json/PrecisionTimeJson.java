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

package api.equinix.javasdk.fabric.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.enums.PrecisionTimeState;
import api.equinix.javasdk.fabric.enums.PrecisionTimeType;
import api.equinix.javasdk.fabric.model.PrecisionTime;
import api.equinix.javasdk.fabric.model.Project;
import api.equinix.javasdk.fabric.model.TimeServiceConnection;
import api.equinix.javasdk.fabric.model.TimeServicePackage;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Md5;
import api.equinix.javasdk.fabric.model.implementation.PrecisionTimeIpv4;
import api.equinix.javasdk.fabric.model.implementation.PrecisionTimeOrder;
import api.equinix.javasdk.fabric.model.implementation.PrecisionTimePrice;
import api.equinix.javasdk.fabric.model.implementation.PtpAdvanceConfiguration;
import api.equinix.javasdk.fabric.model.implementation.TimeServiceOperation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrecisionTimeJson {

    @Getter static TypeReference<List<PrecisionTimeJson>> listTypeRef = new TypeReference<>() {};

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("href")
    private String href;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private PrecisionTimeType type;

    @JsonProperty("state")
    private PrecisionTimeState state;

    @JsonProperty("package")
    @JsonDeserialize(as = TimeServicePackageJson.class)
    private TimeServicePackage servicePackage;

    @JsonProperty("operation")
    private TimeServiceOperation operation;

    @JsonProperty("connections")
    @JsonDeserialize(contentAs = TimeServiceConnectionJson.class)
    private List<TimeServiceConnection> connections;

    @JsonProperty("ipv4")
    private PrecisionTimeIpv4 ipv4;

    @JsonProperty("ntpAdvancedConfiguration")
    private List<Md5> ntpAdvancedConfiguration;

    @JsonProperty("ptpAdvancedConfiguration")
    private PtpAdvanceConfiguration ptpAdvancedConfiguration;

    @JsonProperty("project")
    private Project project;

    @JsonProperty("account")
    private Account account;

    @JsonProperty("order")
    private PrecisionTimeOrder order;

    @JsonProperty("pricing")
    private PrecisionTimePrice pricing;

    @JsonProperty("changeLog")
    private ChangeLog changeLog;

    /**
     * Convenience accessor for the package code of the service's {@code package} member.
     *
     * @return the package code, or {@code null} when no package is present
     */
    public PrecisionTimePackageCode getPackageCode() {
        return this.servicePackage == null ? null : this.servicePackage.getCode();
    }
}
