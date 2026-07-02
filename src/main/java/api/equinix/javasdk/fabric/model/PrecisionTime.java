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

package api.equinix.javasdk.fabric.model;

import api.equinix.javasdk.fabric.enums.PrecisionTimePackageCode;
import api.equinix.javasdk.fabric.enums.PrecisionTimeState;
import api.equinix.javasdk.fabric.enums.PrecisionTimeType;
import api.equinix.javasdk.fabric.model.implementation.Account;
import api.equinix.javasdk.fabric.model.implementation.ChangeLog;
import api.equinix.javasdk.fabric.model.implementation.Md5;
import api.equinix.javasdk.fabric.model.implementation.PrecisionTimeIpv4;
import api.equinix.javasdk.fabric.model.implementation.PrecisionTimeOrder;
import api.equinix.javasdk.fabric.model.implementation.PrecisionTimePrice;
import api.equinix.javasdk.fabric.model.implementation.PtpAdvanceConfiguration;
import api.equinix.javasdk.fabric.model.implementation.TimeServiceOperation;
import api.equinix.javasdk.fabric.model.json.creators.PrecisionTimeOperator;

import java.util.List;

public interface PrecisionTime {

    String getUuid();

    String getHref();

    String getName();

    PrecisionTimeType getType();

    PrecisionTimeState getState();

    /**
     * The service package ({@code NTP_STANDARD}, {@code NTP_ENTERPRISE}, {@code PTP_STANDARD}
     * or {@code PTP_ENTERPRISE}) with its link.
     *
     * @return the service package
     */
    TimeServicePackage getServicePackage();

    /**
     * Convenience accessor for the code of {@code getServicePackage()}.
     *
     * @return the package code, or {@code null} when no package is present
     */
    PrecisionTimePackageCode getPackageCode();

    TimeServiceOperation getOperation();

    List<TimeServiceConnection> getConnections();

    PrecisionTimeIpv4 getIpv4();

    List<Md5> getNtpAdvancedConfiguration();

    PtpAdvanceConfiguration getPtpAdvancedConfiguration();

    Project getProject();

    Account getAccount();

    PrecisionTimeOrder getOrder();

    PrecisionTimePrice getPricing();

    ChangeLog getChangeLog();

    /**
     * Begins a fluent JSON Patch update of this precision time service, e.g.
     * {@code timeService.update().name("New-Name").save()}.
     *
     * @return a {@link api.equinix.javasdk.fabric.model.json.creators.PrecisionTimeOperator.PrecisionTimeUpdater}
     */
    PrecisionTimeOperator.PrecisionTimeUpdater update();

    Boolean delete();

    void refresh();
}
