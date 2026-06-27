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

package api.equinix.javasdk;

import api.equinix.javasdk.core.auth.EquinixCredentials;
import api.equinix.javasdk.core.model.Service;
import api.equinix.javasdk.ibxsmartview.client.Environmentals;
import api.equinix.javasdk.ibxsmartview.client.Hierarchy;
import api.equinix.javasdk.ibxsmartview.client.IBXSmartViewConfig;
import api.equinix.javasdk.ibxsmartview.client.LegacyEnvironmentals;
import api.equinix.javasdk.ibxsmartview.client.LegacyPower;
import api.equinix.javasdk.ibxsmartview.client.Power;
import api.equinix.javasdk.ibxsmartview.client.SmartViewAssets;
import api.equinix.javasdk.ibxsmartview.client.StreamingSubscriptions;
import api.equinix.javasdk.ibxsmartview.client.SystemAlerts;
import api.equinix.javasdk.ibxsmartview.client.implementation.EnvironmentalsImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.HierarchyImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.LegacyEnvironmentalsImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.LegacyPowerImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.PowerImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.SmartViewAssetsImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.StreamingSubscriptionsImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.SystemAlertsImpl;

/**
 * The primary entry point for accessing Equinix IBX SmartView APIs.
 *
 * <p>IBX SmartView provides real-time environmental monitoring, power usage data,
 * system alerts, and streaming subscriptions for Equinix IBX data centers. This class
 * offers typed access to current and legacy sensor data, power readings, location
 * hierarchy information, and asset management.</p>
 *
 * <p>All resource accessors use lazy initialization — internal clients are created on first access
 * and reused for subsequent calls.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * BasicEquinixCredentials credentials = new BasicEquinixCredentials("clientId", "clientSecret");
 * IBXSmartView smartView = new IBXSmartView(credentials);
 *
 * // Get environmental readings for a data center
 * PaginatedList<SensorReading> readings = smartView.environmentals().list("DC2");
 *
 * // Get power readings
 * PaginatedList<PowerReading> power = smartView.power().list("DC2");
 *
 * // Create a streaming subscription for real-time alerts
 * StreamingSubscription sub = smartView.streamingSubscriptions()
 *     .define()
 *     .withName("My-Alerts")
 *     .withChannelType(ChannelType.WEBHOOK)
 *     .withWebhookUrl("https://example.com/webhook")
 *     .create();
 * }</pre>
 *
 * @author ianjones
 * @see api.equinix.javasdk.core.auth.BasicEquinixCredentials
 * @see api.equinix.javasdk.ibxsmartview.client.Environmentals
 * @see api.equinix.javasdk.ibxsmartview.client.StreamingSubscriptions
 */
public final class IBXSmartView extends EquinixClient implements Service {

    private Environmentals environmentals;
    private Power power;
    private StreamingSubscriptions streamingSubscriptions;
    private SystemAlerts systemAlerts;
    private Hierarchy hierarchy;
    private SmartViewAssets smartViewAssets;
    private LegacyEnvironmentals legacyEnvironmentals;
    private LegacyPower legacyPower;

    final private IBXSmartViewConfig ibxSmartViewConfig;

    /**
     * Creates a new IBX SmartView client using the provided credentials.
     * Authentication occurs automatically on the first API call.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     */
    public IBXSmartView(EquinixCredentials equinixCredentials) {
        this(equinixCredentials, false);
    }

    /**
     * Creates a new IBX SmartView client with optional sandbox mode.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public IBXSmartView(EquinixCredentials equinixCredentials, boolean isSandBoxed) {
        super(equinixCredentials, isSandBoxed);

        String paramFile = "json/apiParams_IBXSmartView.json";
        equinixClient.appendApiParams(paramFile);

        this.ibxSmartViewConfig = new IBXSmartViewConfigImpl(equinixClient);
    }

    /**
     * Returns the client for accessing current environmental sensor data from IBX data centers.
     * Provides temperature, humidity, and other environmental readings.
     *
     * @return the {@link Environmentals} client for querying environmental sensor data
     */
    public Environmentals environmentals() {
        if (this.environmentals == null) {
            this.environmentals = new EnvironmentalsImpl(this.ibxSmartViewConfig.getEnvironmentalClient(), this);
        }
        return environmentals;
    }

    /**
     * Returns the client for accessing current power usage data from IBX data centers.
     * Provides real-time power consumption and capacity metrics.
     *
     * @return the {@link Power} client for querying power usage data
     */
    public Power power() {
        if (this.power == null) {
            this.power = new PowerImpl(this.ibxSmartViewConfig.getPowerClient(), this);
        }
        return power;
    }

    /**
     * Returns the client for managing real-time streaming subscriptions.
     * Supports AWS IoT, Azure Event Hub, Webhook, and REST delivery channels
     * for environmental, power, and alert data.
     *
     * @return the {@link StreamingSubscriptions} client for creating and managing streaming subscriptions
     */
    public StreamingSubscriptions streamingSubscriptions() {
        if (this.streamingSubscriptions == null) {
            this.streamingSubscriptions = new StreamingSubscriptionsImpl(this.ibxSmartViewConfig.getStreamingSubscriptionClient(), this);
        }
        return streamingSubscriptions;
    }

    /**
     * Returns the client for accessing system alerts from IBX facilities.
     * Provides active and historical alerts for environmental and power anomalies.
     *
     * @return the {@link SystemAlerts} client for querying system alerts
     */
    public SystemAlerts systemAlerts() {
        if (this.systemAlerts == null) {
            this.systemAlerts = new SystemAlertsImpl(this.ibxSmartViewConfig.getSystemAlertClient(), this);
        }
        return systemAlerts;
    }

    /**
     * Returns the client for querying IBX location hierarchy data.
     * Provides the organizational structure of accounts, IBX locations, floors, cages, and cabinets.
     *
     * @return the {@link Hierarchy} client for querying location hierarchy
     */
    public Hierarchy hierarchy() {
        if (this.hierarchy == null) {
            this.hierarchy = new HierarchyImpl(this.ibxSmartViewConfig.getHierarchyClient(), this);
        }
        return hierarchy;
    }

    /**
     * Returns the client for managing SmartView asset information within IBX facilities.
     *
     * @return the {@link SmartViewAssets} client for managing SmartView assets
     */
    public SmartViewAssets smartViewAssets() {
        if (this.smartViewAssets == null) {
            this.smartViewAssets = new SmartViewAssetsImpl(this.ibxSmartViewConfig.getSmartViewAssetClient(), this);
        }
        return smartViewAssets;
    }

    /**
     * Returns the client for accessing legacy (v1) environmental data APIs.
     * Provides historical trending data for temperature and humidity with configurable time intervals.
     *
     * @return the {@link LegacyEnvironmentals} client for querying legacy environmental data
     */
    public LegacyEnvironmentals legacyEnvironmentals() {
        if (this.legacyEnvironmentals == null) {
            this.legacyEnvironmentals = new LegacyEnvironmentalsImpl(this.ibxSmartViewConfig.getLegacyEnvironmentalClient(), this);
        }
        return legacyEnvironmentals;
    }

    /**
     * Returns the client for accessing legacy (v1) power data APIs.
     * Provides historical trending data for power consumption with configurable time intervals.
     *
     * @return the {@link LegacyPower} client for querying legacy power data
     */
    public LegacyPower legacyPower() {
        if (this.legacyPower == null) {
            this.legacyPower = new LegacyPowerImpl(this.ibxSmartViewConfig.getLegacyPowerClient(), this);
        }
        return legacyPower;
    }
}
