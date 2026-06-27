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

package api.equinix.javasdk.spring;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.NetworkEdge;
import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for the Equinix SDK.
 *
 * <p>When the SDK is on the classpath and an {@code equinix.client-id} property is present,
 * this configuration contributes a {@link BasicEquinixCredentials} bean and the Equinix
 * entry-point beans ({@link Fabric}, {@link CustomerPortal}, {@link NetworkEdge}) built from
 * the bound {@link EquinixProperties}. Each bean is {@link ConditionalOnMissingBean}, so an
 * application may override any of them by declaring its own.</p>
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * equinix.client-id=your-client-id
 * equinix.client-secret=your-client-secret
 * equinix.sandbox=false
 * }</pre>
 *
 * @author ianjones
 * @see EquinixProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(EquinixProperties.class)
@ConditionalOnClass(Fabric.class)
@ConditionalOnProperty(prefix = "equinix", name = "client-id")
public class EquinixAutoConfiguration {

    /**
     * Builds the SDK credentials from the configured client id and secret.
     *
     * @param properties the bound Equinix configuration properties
     * @return a {@link BasicEquinixCredentials} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public BasicEquinixCredentials equinixCredentials(EquinixProperties properties) {
        return new BasicEquinixCredentials(properties.getClientId(), properties.getClientSecret());
    }

    /**
     * Builds the Fabric entry-point client.
     *
     * @param credentials the SDK credentials
     * @param properties  the bound Equinix configuration properties (for the sandbox flag)
     * @return a {@link Fabric} client
     */
    @Bean
    @ConditionalOnMissingBean
    public Fabric equinixFabric(BasicEquinixCredentials credentials, EquinixProperties properties) {
        return new Fabric(credentials, properties.isSandbox());
    }

    /**
     * Builds the Customer Portal entry-point client.
     *
     * @param credentials the SDK credentials
     * @param properties  the bound Equinix configuration properties (for the sandbox flag)
     * @return a {@link CustomerPortal} client
     */
    @Bean
    @ConditionalOnMissingBean
    public CustomerPortal equinixCustomerPortal(BasicEquinixCredentials credentials, EquinixProperties properties) {
        return new CustomerPortal(credentials, properties.isSandbox());
    }

    /**
     * Builds the Network Edge entry-point client.
     *
     * @param credentials the SDK credentials
     * @param properties  the bound Equinix configuration properties (for the sandbox flag)
     * @return a {@link NetworkEdge} client
     */
    @Bean
    @ConditionalOnMissingBean
    public NetworkEdge equinixNetworkEdge(BasicEquinixCredentials credentials, EquinixProperties properties) {
        return new NetworkEdge(credentials, properties.isSandbox());
    }
}
