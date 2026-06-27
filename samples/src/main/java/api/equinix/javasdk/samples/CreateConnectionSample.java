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

package api.equinix.javasdk.samples;

import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.auth.BasicEquinixCredentials;
import api.equinix.javasdk.fabric.enums.ConnectionType;
import api.equinix.javasdk.fabric.enums.NotificationType;
import api.equinix.javasdk.fabric.model.Connection;
import api.equinix.javasdk.fabric.model.implementation.LinkProtocol;

/**
 * Defines an Equinix Fabric connection and validates it with the server-side
 * {@code dryRun()} flag instead of actually provisioning it.
 *
 * <p>A dry run sends the exact create payload the SDK would normally POST, but the
 * platform only validates it (auth keys, VLAN availability, bandwidth, profile shape)
 * and returns without creating anything billable. It is the safe way to confirm a
 * connection definition is well-formed before committing to {@code create()}.</p>
 *
 * <h3>Running</h3>
 * <pre>{@code
 * export EQUINIX_CLIENT_ID=...      # OAuth2 Client ID from the Equinix Developer Portal
 * export EQUINIX_CLIENT_SECRET=...  # OAuth2 Client Secret
 * export EQUINIX_A_SIDE_PORT_UUID=... # a colocation port UUID you own
 * export EQUINIX_Z_SIDE_PROFILE_UUID=... # the target service profile UUID
 * }</pre>
 *
 * <p>This program is illustrative; it is not executed by CI.</p>
 */
public final class CreateConnectionSample {

    private CreateConnectionSample() {
    }

    public static void main(String[] args) {
        BasicEquinixCredentials credentials = new BasicEquinixCredentials(
                requireEnv("EQUINIX_CLIENT_ID"),
                requireEnv("EQUINIX_CLIENT_SECRET"));

        String aSidePortUuid = requireEnv("EQUINIX_A_SIDE_PORT_UUID");
        String zSideProfileUuid = requireEnv("EQUINIX_Z_SIDE_PROFILE_UUID");

        // try-with-resources closes the underlying HTTP client when we are done.
        try (Fabric fabric = new Fabric(credentials)) {

            // Build a single-port EVPL virtual connection from a colocation port (A-side)
            // to a service profile (Z-side), then validate it with dryRun() before creating.
            Connection validated = fabric.connections()
                    .define(ConnectionType.EVPL_VC)
                    .name("Sample-DryRun-Connection")
                    .bandwidth(100)
                    .aSideAccessPointPort(
                            aSidePortUuid,
                            LinkProtocol.dot1q().vlanTag(1000).create())
                    .zSideAccessPointServiceProfile(
                            zSideProfileUuid,
                            LinkProtocol.dot1q().vlanTag(1000).create())
                    .notification(NotificationType.ALL, "noc@example.com")
                    .dryRun()
                    .create();

            System.out.println("Dry-run validation succeeded.");
            System.out.println("  name:      " + validated.getName());
            System.out.println("  type:      " + validated.getType());
            System.out.println("  bandwidth: " + validated.getBandwidth() + " Mbps");
            System.out.println("  state:     " + validated.getState());
            System.out.println();
            System.out.println("Remove .dryRun() above to actually provision this connection.");
        } catch (Exception e) {
            System.err.println("Connection dry-run failed: " + e.getMessage());
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
