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
import api.equinix.javasdk.core.auth.EquinixCredentialsProvider;
import api.equinix.javasdk.core.auth.EquinixStaticCredentialsProvider;
import api.equinix.javasdk.core.model.Service;
import api.equinix.javasdk.ibxsmartview.client.Environmentals;
import api.equinix.javasdk.ibxsmartview.client.Hierarchy;
import api.equinix.javasdk.ibxsmartview.client.IBXSmartViewConfig;
import api.equinix.javasdk.ibxsmartview.client.LegacyEnvironmentals;
import api.equinix.javasdk.ibxsmartview.client.LegacyPower;
import api.equinix.javasdk.ibxsmartview.client.PowerEvents;
import api.equinix.javasdk.ibxsmartview.client.SmartViewAssets;
import api.equinix.javasdk.ibxsmartview.client.StreamingSubscriptions;
import api.equinix.javasdk.ibxsmartview.client.SystemAlerts;
import api.equinix.javasdk.ibxsmartview.client.implementation.EnvironmentalsImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.HierarchyImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.IBXSmartViewConfigImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.LegacyEnvironmentalsImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.LegacyPowerImpl;
import api.equinix.javasdk.ibxsmartview.client.implementation.PowerEventsImpl;
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
 * // Search recent power events for one or more IBX data centers
 * PaginatedList<PowerEvent> events = smartView.powerEvents().search(List.of("SV5"), null, null, 0, 100);
 *
 * // Create a streaming subscription for real-time alerts
 * StreamingSubscription sub = smartView.streamingSubscriptions()
 *     .define()
 *     .withChannel(Channel.builder()
 *         .channelType(ChannelType.WEBHOOK)
 *         .webhookChannelConfiguration(WebhookChannelConfiguration.builder()
 *             .url("https://example.com/webhook").build())
 *         .build())
 *     .withMessageType(MessageType.builder()
 *         .environmental(List.of(EnvironmentalMessageType.builder()
 *             .accountNumber("123456").ibx(List.of("SV5")).build()))
 *         .build())
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
    private PowerEvents powerEvents;
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
        this(new EquinixStaticCredentialsProvider(equinixCredentials), isSandBoxed);
    }

    /**
     * Creates a new IBX SmartView client whose credentials are resolved through the given provider.
     * Authentication occurs automatically on the first API call.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     */
    public IBXSmartView(EquinixCredentialsProvider credentialsProvider) {
        this(credentialsProvider, false);
    }

    /**
     * Creates a new IBX SmartView client over a custom credentials provider, with optional sandbox mode.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param isSandBoxed {@code true} to use the sandbox environment for testing; {@code false} for production
     */
    public IBXSmartView(EquinixCredentialsProvider credentialsProvider, boolean isSandBoxed) {
        super(credentialsProvider, isSandBoxed);

        String paramFile = "json/apiParams_IBXSmartView.json";
        equinixClient.appendApiParams(paramFile);

        this.ibxSmartViewConfig = new IBXSmartViewConfigImpl(equinixClient);
    }

    /**
     * Creates a new IBXSmartView client with explicit {@link EquinixConfig} options.
     *
     * @param equinixCredentials the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public IBXSmartView(EquinixCredentials equinixCredentials, EquinixConfig config) {
        this(new EquinixStaticCredentialsProvider(equinixCredentials), config);
    }

    /**
     * Creates a new IBXSmartView client over a custom credentials provider with explicit
     * {@link EquinixConfig} options.
     *
     * @param credentialsProvider supplies the OAuth2 credentials for authenticating with Equinix APIs
     * @param config the construction-time options
     */
    public IBXSmartView(EquinixCredentialsProvider credentialsProvider, EquinixConfig config) {
        super(credentialsProvider, config);

        String paramFile = "json/apiParams_IBXSmartView.json";
        equinixClient.appendApiParams(paramFile);

        this.ibxSmartViewConfig = new IBXSmartViewConfigImpl(equinixClient);
    }

    /**
     * Package-private constructor for {@link Equinix} sessions: builds this domain client over a
     * shared core client (one OAuth token + connection pool across domains).
     */
    IBXSmartView(api.equinix.javasdk.core.client.EquinixClient sharedCore) {
        super(sharedCore);
        equinixClient.appendApiParams("json/apiParams_IBXSmartView.json");
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
     * Returns the client for accessing IBX SmartView power events and power alert configurations.
     * Provides power-event search plus create, search, pause, resume, and delete operations for
     * power alert configurations.
     *
     * @return the {@link PowerEvents} client for querying power events and managing power alert configurations
     */
    public PowerEvents powerEvents() {
        if (this.powerEvents == null) {
            this.powerEvents = new PowerEventsImpl(this.ibxSmartViewConfig.getPowerEventClient(), this);
        }
        return powerEvents;
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
