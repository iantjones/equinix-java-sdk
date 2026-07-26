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

/**
 * Customer Portal model interfaces for the Equinix Java SDK. Defines data access
 * contracts for portal resources including
 * {@link com.eqixiac.equinix.customerportal.model.TroubleTicket},
 * {@link com.eqixiac.equinix.customerportal.model.SmartHandResponse},
 * {@link com.eqixiac.equinix.customerportal.model.OrderResponse},
 * {@link com.eqixiac.equinix.customerportal.model.Order},
 * {@link com.eqixiac.equinix.customerportal.model.Quote},
 * {@link com.eqixiac.equinix.customerportal.model.InvoiceDetail}, and
 * {@link com.eqixiac.equinix.customerportal.model.SupportCase}. Order-submission resources
 * (cross-connects, shipments, work visits, secure cabinets) return an
 * {@link com.eqixiac.equinix.customerportal.model.OrderResponse}; the resulting order is then
 * tracked through {@link com.eqixiac.equinix.customerportal.model.Order}.
 *
 * @see com.eqixiac.equinix.customerportal.model.TroubleTicket
 * @see com.eqixiac.equinix.customerportal.model.Order
 */
package com.eqixiac.equinix.customerportal.model;
