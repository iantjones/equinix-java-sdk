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

package com.eqixiac.equinix.operations.enums;

/**
 * Fields an asset search free-text query ({@code q}) is matched against ({@code source} query
 * parameter of {@code POST /v1/assets/search}, Assets v1 spec). By default all fields are searched.
 */
public enum AssetSearchSource {
    LEGACY_CAGE,
    LEGACY_CABINET,
    LEGACY_PATCH_PANEL,
    LEGACY_SALES_ORDER,
    BILLING_AGREEMENT,
    CAGE,
    CABINET,
    PATCH_PANEL,
    ASSET_NUMBER,
    CUSTOMER_INTERNAL_REFERENCE,
    SALES_ORDER_NUMBER,
    CABLE_ID,
    CARRIER_CIRCUIT_ID,
    SERIAL_NUMBER,
    EXTERNAL_REFERENCE_NUMBER,
    ACCOUNT_NUMBER
}
