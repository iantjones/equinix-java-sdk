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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the {@link EquinixAutoConfiguration} contract using the Spring Boot
 * {@link ApplicationContextRunner}.
 */
class EquinixAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EquinixAutoConfiguration.class));

    @Test
    void createsBeansWhenCredentialsConfigured() {
        contextRunner
                .withPropertyValues(
                        "equinix.client-id=test-client-id",
                        "equinix.client-secret=test-client-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(BasicEquinixCredentials.class);
                    assertThat(context).hasSingleBean(Fabric.class);
                    assertThat(context).hasSingleBean(CustomerPortal.class);
                    assertThat(context).hasSingleBean(NetworkEdge.class);

                    BasicEquinixCredentials credentials = context.getBean(BasicEquinixCredentials.class);
                    assertThat(credentials.getAccessKey()).isEqualTo("test-client-id");
                    assertThat(credentials.getSecretKey()).isEqualTo("test-client-secret");

                    EquinixProperties properties = context.getBean(EquinixProperties.class);
                    assertThat(properties.isSandbox()).isFalse();
                });
    }

    @Test
    void honoursSandboxFlag() {
        contextRunner
                .withPropertyValues(
                        "equinix.client-id=test-client-id",
                        "equinix.client-secret=test-client-secret",
                        "equinix.sandbox=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Fabric.class);
                    assertThat(context.getBean(EquinixProperties.class).isSandbox()).isTrue();
                });
    }

    @Test
    void backsOffWhenClientIdMissing() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(BasicEquinixCredentials.class);
            assertThat(context).doesNotHaveBean(Fabric.class);
            assertThat(context).doesNotHaveBean(CustomerPortal.class);
            assertThat(context).doesNotHaveBean(NetworkEdge.class);
        });
    }

    @Test
    void backsOffForUserDefinedFabricBean() {
        contextRunner
                .withPropertyValues(
                        "equinix.client-id=test-client-id",
                        "equinix.client-secret=test-client-secret")
                .withUserConfiguration(CustomFabricConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Fabric.class);
                    assertThat(context.getBean(Fabric.class))
                            .isSameAs(context.getBean("customFabric"));
                });
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class CustomFabricConfiguration {
        @org.springframework.context.annotation.Bean
        Fabric customFabric() {
            return new Fabric(new BasicEquinixCredentials("override", "override"));
        }
    }
}
