# Equinix Java SDK

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-14%2B-orange.svg)](https://openjdk.java.net/)
[![Javadoc](https://img.shields.io/badge/Javadoc-API%20Reference-blue.svg)](https://iantjones.github.io/equinix-java-sdk/)

> A comprehensive Java SDK for the Equinix Platform APIs, providing typed access to Fabric, Network Edge, Customer Portal, IBX SmartView, Internet Access, Projects, and Messaging services.

## Features

- **7 API domains** with 58 resource types and 310+ API methods
- **Fluent builder pattern** for creating and updating resources
- **Cloud provider SDK interoperability** via adapter pattern (AWS Direct Connect, Azure ExpressRoute, Google Cloud Interconnect, Oracle FastConnect)
- **Automatic pagination** with `PaginatedList<T>` and `PaginatedFilteredList<T>`
- **Built-in OAuth2** authentication with automatic token management
- **Dry Run validation** for Fabric connections and service tokens
- **Jackson-based** JSON serialization with full type safety
- **Comprehensive exception hierarchy** mapping HTTP status codes to typed exceptions
- **Real-time streaming** support via IBX SmartView subscriptions (AWS IoT, Azure Event Hub, Webhook, REST)

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.eqixiac.equinix</groupId>
    <artifactId>equinix-sdk-java</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Authentication

All domain clients use OAuth2 client credentials. Obtain your Client ID and Client Secret from the [Equinix Developer Portal](https://developer.equinix.com/).

```java
import api.equinix.javasdk.Fabric;
import api.equinix.javasdk.core.auth.BasicEquinixCredentials;

BasicEquinixCredentials credentials = new BasicEquinixCredentials("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET");
Fabric fabric = new Fabric(credentials);
// Authentication happens automatically on first API call
```

## Domain Overview

| Domain | Entry Point | Resources | Description |
|--------|-------------|-----------|-------------|
| **Fabric** | `new Fabric(creds)` | 21 | Connections, Ports, Service Tokens, Cloud Routers, Streams, Networks, Route Filters, Routing Protocols, Prices, Health |
| **Network Edge** | `new NetworkEdge(creds)` | 10 | Virtual Devices, SSH Users, ACL Templates, VPNs, BGP Peerings, Device Links, Public Keys, Backups |
| **Customer Portal** | `new CustomerPortal(creds)` | 21 | Cross-Connects, Trouble Tickets, Work Visits, Smart Hands, Shipments, Invoices, Orders, Assets, Reports |
| **IBX SmartView** | `new IBXSmartView(creds)` | 8 | Environmental Sensors, Power Readings, System Alerts, Streaming Subscriptions, Asset Management, Hierarchy |
| **Internet Access** | `new InternetAccess(creds)` | 3 | Internet Access Services, Ports, Routing Configurations |
| **Projects** | `new Projects(creds)` | 1 | Project Management |
| **Messaging** | `new Messaging(creds)` | 2 | Notification Subscriptions, Events |

## Usage Examples

### Fabric: Working with Connections

```java
Fabric fabric = new Fabric(credentials);

// List all connections
PaginatedFilteredList<Connection> connections = fabric.connections().search();
for (Connection conn : connections) {
    System.out.println(conn.getName() + " - " + conn.getState());
}

// Get a specific connection
Connection connection = fabric.connections().getByUuid("your-connection-uuid");

// Get connection statistics
ConnectionStatistic stats = fabric.connections().getStatistics(
    connection.getUuid(),
    LocalDateTime.now().minusDays(1),
    LocalDateTime.now()
);
```

### Fabric: Creating a Connection with the Fluent Builder

```java
Connection newConnection = fabric.connections()
    .define(ConnectionType.EVPL_VC)
    .name("My-AWS-Connection")
    .bandwidth(50)
    .redundancy("my-group", RedundancyPriority.PRIMARY)
    .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1000).create())
    .zSideAccessPointServiceProfile(profileUuid, LinkProtocol.dot1q().vlanTag(1000).create())
    .notification("ops@example.com")
    .create();

System.out.println("Created: " + newConnection.getUuid());
```

### Fabric: Dry Run Validation

Validate a connection without actually creating it:

```java
Connection validated = fabric.connections()
    .define(ConnectionType.EVPL_VC)
    .name("Test-Connection")
    .bandwidth(100)
    .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(2000).create())
    .zSideAccessPointServiceProfile(profileUuid, LinkProtocol.dot1q().vlanTag(2000).create())
    .dryRun()    // Validates without creating
    .create();
// Returns validation result without side effects
```

### Fabric: Port Discovery and Statistics

```java
// List all ports and find DOT1Q ports
PaginatedList<Port> ports = fabric.ports().list();
Port dot1qPort = ports.stream()
    .filter(p -> p.getEncapsulation().getType() == EncapsulationType.DOT1Q)
    .findFirst()
    .orElseThrow();

// Get port statistics
PortStatistic stats = fabric.ports().getStatistics(
    dot1qPort.getUuid(),
    LocalDateTime.now().minusHours(24),
    LocalDateTime.now()
);
```

### Fabric: Cloud Routers and Routing

```java
// List cloud routers
PaginatedFilteredList<CloudRouter> routers = fabric.cloudRouters().search();

// Get available router packages
PaginatedList<CloudRouterPackage> packages = fabric.cloudRouters().routerPackages();

// List routing protocols for a connection
PaginatedList<RoutingProtocol> protocols = fabric.routingProtocols().list(connectionId);

// Route filters
PaginatedFilteredList<RouteFilter> filters = fabric.routeFilters().search();
```

### Fabric: Real-Time Streams

```java
// List streams
PaginatedList<Stream> streams = fabric.streams().list();

// Create a stream subscription
StreamSubscription sub = fabric.streamSubscriptions()
    .define(streamId)
    .withName("My-Subscription")
    .create();

// List subscriptions for a stream
PaginatedList<StreamSubscription> subs = fabric.streamSubscriptions().list(streamId);
```

### Fabric: Cloud Provider SDK Interoperability

The SDK includes a **Cloud Provider Adapter** framework that bridges cloud provider SDK objects (AWS, Azure, Google Cloud, Oracle) with Equinix Fabric connection creation. This lets you pass cloud provider objects directly into the Fabric connection builder.

#### Built-in Adapters

| Cloud Provider | Adapter Class | Authentication Key | Region Format |
|---------------|--------------|-------------------|---------------|
| **AWS** | `AwsDirectConnectAdapter` | 12-digit AWS Account ID | `us-east-1` |
| **Azure** | `AzureExpressRouteAdapter` | ExpressRoute Service Key (GUID) | `Silicon Valley` |
| **Google Cloud** | `GoogleCloudInterconnectAdapter` | GCP Pairing Key | `us-east1` |
| **Oracle Cloud** | `OracleFastConnectAdapter` | Virtual Circuit OCID | `us-ashburn-1` |

#### AWS Direct Connect

```java
// Option 1: Wrap an AWS SDK object
AwsDirectConnectAdapter<Connection> adapter = new AwsDirectConnectAdapter<>(
    awsConnection,                              // AWS SDK Connection object
    awsConnection.getOwnerAccount(),            // 12-digit AWS Account ID
    awsConnection.getRegion(),                  // e.g., "us-east-1"
    "equinix-aws-service-profile-uuid"          // Equinix service profile for AWS
);

// Option 2: Manual construction (no AWS SDK dependency)
AwsDirectConnectAdapter<?> adapter = AwsDirectConnectAdapter.of(
    "123456789012", "us-east-1", "equinix-aws-profile-uuid");

// Use the adapter in the connection builder
Connection conn = fabric.connections()
    .define(ConnectionType.EVPL_VC)
    .name("Port-to-AWS-DirectConnect")
    .bandwidth(100)
    .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1000).create())
    .zSideCloudProvider(adapter, LinkProtocol.dot1q().vlanTag(1000).create())
    .notification("ops@example.com")
    .create();
```

#### Azure ExpressRoute

```java
// Azure requires a peering type (PRIVATE or MICROSOFT)
AzureExpressRouteAdapter<?> adapter = AzureExpressRouteAdapter.of(
    "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",     // ExpressRoute Service Key
    "Silicon Valley",                            // Peering location
    "equinix-azure-profile-uuid",                // Equinix service profile
    PeeringType.PRIVATE                          // Private or Microsoft peering
);

Connection conn = fabric.connections()
    .define(ConnectionType.EVPL_VC)
    .name("Port-to-Azure-ExpressRoute")
    .bandwidth(200)
    .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1500).create())
    .zSideCloudProvider(adapter, LinkProtocol.dot1q().vlanTag(1500).create())
    .notification("ops@example.com")
    .create();
```

#### Google Cloud Interconnect

```java
GoogleCloudInterconnectAdapter<?> adapter = GoogleCloudInterconnectAdapter.of(
    "xxxx/xxxx/xxxx/xxxx",                      // GCP Pairing Key
    "us-east1",                                  // GCP region
    "equinix-gcp-profile-uuid"                   // Equinix service profile
);

Connection conn = fabric.connections()
    .define(ConnectionType.EVPL_VC)
    .name("Port-to-GCP-Interconnect")
    .bandwidth(100)
    .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(2000).create())
    .zSideCloudProvider(adapter, LinkProtocol.dot1q().vlanTag(2000).create())
    .notification("ops@example.com")
    .create();
```

#### Custom Adapter

Implement `CloudProviderConnectionAdapter<T>` for any cloud provider not covered by the built-in adapters:

```java
public class IbmDirectLinkAdapter implements CloudProviderConnectionAdapter<Void> {

    private final String serviceKey;
    private final String region;
    private final String profileUuid;

    public IbmDirectLinkAdapter(String serviceKey, String region, String profileUuid) {
        this.serviceKey = serviceKey;
        this.region = region;
        this.profileUuid = profileUuid;
    }

    @Override public String getServiceProfileUuid()  { return profileUuid; }
    @Override public String getAuthenticationKey()   { return serviceKey; }
    @Override public String getSellerRegion()        { return region; }
    @Override public Void getSource()                { return null; }
    @Override public CloudProviderType getProviderType() { return CloudProviderType.IBM_CLOUD; }
}

// Use exactly like the built-in adapters
Connection conn = fabric.connections()
    .define(ConnectionType.EVPL_VC)
    .name("IBM-DirectLink")
    .bandwidth(50)
    .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(500).create())
    .zSideCloudProvider(new IbmDirectLinkAdapter(serviceKey, "us-south", profileUuid),
                        LinkProtocol.dot1q().vlanTag(500).create())
    .notification("ops@example.com")
    .create();
```

#### Multi-Cloud with Dry Run Validation

Combine cloud provider adapters with Dry Run to validate multi-cloud connections before creating them:

```java
AwsDirectConnectAdapter<?> awsAdapter = AwsDirectConnectAdapter.of(
    "123456789012", "us-east-1", awsProfileUuid);

AzureExpressRouteAdapter<?> azureAdapter = AzureExpressRouteAdapter.of(
    "service-key", "Silicon Valley", azureProfileUuid, PeeringType.PRIVATE);

// Validate AWS connection (dry run - no resources created)
Connection awsValidated = fabric.connections()
    .define(ConnectionType.EVPL_VC)
    .name("AWS-Primary")
    .bandwidth(100)
    .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1000).create())
    .zSideCloudProvider(awsAdapter, LinkProtocol.dot1q().vlanTag(1000).create())
    .notification("ops@example.com")
    .dryRun()
    .create();

// Validate Azure connection (dry run)
Connection azureValidated = fabric.connections()
    .define(ConnectionType.EVPL_VC)
    .name("Azure-Secondary")
    .bandwidth(200)
    .aSideAccessPointPort(portUuid, LinkProtocol.dot1q().vlanTag(1500).create())
    .zSideCloudProvider(azureAdapter, LinkProtocol.dot1q().vlanTag(1500).create())
    .notification("ops@example.com")
    .dryRun()
    .create();

// Both validated successfully - now create for real (remove .dryRun())
```

### Network Edge: Virtual Devices

```java
NetworkEdge networkEdge = new NetworkEdge(credentials);

// List all devices
PaginatedList<Device> devices = networkEdge.devices().list();

// List available device types
PaginatedList<DeviceType> deviceTypes = networkEdge.devices().listDeviceTypes();

// Create a device with the fluent builder
Device device = networkEdge.devices()
    .define("my-router")
    .withAccountNumber(accountNumber)
    .inMetro(MetroCode.SV)
    .withDeviceType("CSR1000V")
    .withLicenseType(LicenseType.SUB)
    .withThroughput(500)
    .withThroughputUnit(BandwidthUnit.MBPS)
    .withCore(4)
    .withManagementType(DeviceManagementType.EQUINIX_CONFIGURED)
    .withPackageCode(PackageCode.IPBASE)
    .create();
```

### Network Edge: SSH Users and Security

```java
// Manage SSH users
PaginatedList<SSHUser> sshUsers = networkEdge.sshUsers().list();

// Manage ACL templates
PaginatedList<ACLTemplate> templates = networkEdge.aclTemplates().list();

// BGP peerings
PaginatedList<BGPPeering> peerings = networkEdge.bgpPeerings().list();

// VPN connections
PaginatedList<VPN> vpns = networkEdge.vpns().list();
```

### IBX SmartView: Environmental Monitoring

```java
IBXSmartView smartView = new IBXSmartView(credentials);

// Get environmental sensor readings for a data center
PaginatedList<SensorReading> readings = smartView.environmentals().list("DC2");
for (SensorReading reading : readings) {
    System.out.println(reading.getSensorId() + ": " + reading.getTemperature() + "C");
}

// Get power readings
PaginatedList<PowerReading> power = smartView.power().list("DC2");

// Search system alerts
PaginatedList<SystemAlert> alerts = smartView.systemAlerts()
    .search("ACTIVE", null, null, 0, 50);
```

### IBX SmartView: Streaming Subscriptions

```java
// Create a streaming subscription for real-time data
StreamingSubscription subscription = smartView.streamingSubscriptions()
    .define()
    .withName("My-Alerts-Stream")
    .withChannelType(ChannelType.WEBHOOK)
    .withWebhookUrl("https://my-app.example.com/webhook")
    .addMessage(StreamingMessageType.ALERT, accountNumbers, ibxCodes)
    .addMessage(StreamingMessageType.ENVIRONMENTAL, accountNumbers, ibxCodes)
    .create();

// List existing subscriptions
List<StreamingSubscription> subs = smartView.streamingSubscriptions().list();

// Get subscription data
SubscriptionData data = smartView.streamingSubscriptions()
    .getSubscriptionData(subscription.getUuid());

// Clean up
subscription.delete();
```

### IBX SmartView: Legacy APIs and Hierarchy

```java
// Location hierarchy for an IBX
LocationHierarchy hierarchy = smartView.hierarchy()
    .getLocationHierarchy(accountNo, "DC2");

// Legacy environment data
EnvironmentData envData = smartView.legacyEnvironmentals()
    .getCurrent(accountNo, "DC2", "IBX", "DC2");

// Trending environment data
TrendingEnvironmentData trending = smartView.legacyEnvironmentals()
    .getTrending(accountNo, "DC2", "temperature", "IBX", "DC2",
                 "15min", "2024-01-01", "2024-01-02");
```

### Customer Portal: Operations Management

```java
CustomerPortal portal = new CustomerPortal(credentials);

// List cross-connects
PaginatedList<CrossConnect> crossConnects = portal.crossConnects().list();

// List trouble tickets
PaginatedList<TroubleTicket> tickets = portal.troubleTickets().list();

// List invoices
PaginatedList<InvoiceSummary> invoices = portal.invoices().summaries();

// List work visits
PaginatedList<WorkVisit> visits = portal.workVisits().list();

// List smart hands requests
PaginatedList<SmartHands> smartHands = portal.smartHandsRequests().list();
```

### Observability: Chaining Resources

The SDK enables powerful resource chaining patterns:

```java
Fabric fabric = new Fabric(credentials);

// Port -> Connection -> Statistics pipeline
Port port = fabric.ports().list().get(0);
PaginatedFilteredList<Connection> connections = fabric.connections().search();
Connection conn = connections.stream()
    .filter(c -> c.getASide() != null)
    .findFirst()
    .orElseThrow();

ConnectionStatistic stats = fabric.connections().getStatistics(
    conn.getUuid(),
    LocalDateTime.now().minusDays(7),
    LocalDateTime.now()
);

// Cloud Router -> Routing Protocol -> Route Filter chain
PaginatedFilteredList<CloudRouter> routers = fabric.cloudRouters().search();
PaginatedList<RoutingProtocol> protocols = fabric.routingProtocols().list(connectionId);
PaginatedFilteredList<RouteFilter> filters = fabric.routeFilters().search();
```

### Error Handling

The SDK provides a typed exception hierarchy:

```java
try {
    Connection conn = fabric.connections().getByUuid("non-existent-uuid");
} catch (EquinixNotFoundException e) {
    // 404 - Resource not found
    System.err.println("Connection not found: " + e.getErrorMessage());
} catch (EquinixAuthenticationException e) {
    // 401 - Invalid credentials
    System.err.println("Authentication failed");
} catch (EquinixRateLimitException e) {
    // 429 - Rate limited
    System.err.println("Rate limited, retry after: " + e.getMessage());
} catch (EquinixServiceException e) {
    // Any other API error
    System.err.println("API error " + e.getHttpCode() + ": " + e.getErrorMessage());
}
```

| HTTP Status | Exception Class |
|-------------|----------------|
| 401 | `EquinixAuthenticationException` |
| 403 | `EquinixAuthorizationException` |
| 404 | `EquinixNotFoundException` |
| 409 | `EquinixConflictException` |
| 429 | `EquinixRateLimitException` |
| 5xx | `EquinixServerException` |

### Pagination

All list operations return `PaginatedList<T>` which extends `ArrayList<T>` with pagination metadata:

```java
PaginatedList<Port> ports = fabric.ports().list();

// Access pagination info
Pagination pagination = ports.getPagination();
int pageNumber = pagination.getPageNumber();
int pageSize = pagination.getPageSize();
boolean isFirst = pagination.getIsFirstPage();
boolean isLast = pagination.getIsLastPage();
int total = pagination.getTotal();
```

## Testing

### Unit Tests (No Credentials Required)
```bash
mvn test
```
Unit tests validate JSON deserialization, pagination logic, exception mapping, and builder patterns using Mockito and WireMock.

### Integration Tests (Credentials Required)
```bash
mvn test -Pintegration \
    -Dauth.access=YOUR_CLIENT_ID \
    -Dauth.secret=YOUR_CLIENT_SECRET
```

By default, integration tests run in read-only mode. To enable create/update/delete operations:
```bash
mvn test -Pintegration \
    -Dauth.access=YOUR_CLIENT_ID \
    -Dauth.secret=YOUR_CLIENT_SECRET \
    -DskipCreateUpdateOperations=false
```

## API Reference

Full Javadoc documentation is available at **[iantjones.github.io/equinix-java-sdk](https://iantjones.github.io/equinix-java-sdk/)**.

## Building

```bash
# Compile
mvn clean compile

# Package (includes source and Javadoc JARs)
mvn clean package -DskipTests

# Generate Javadoc site
mvn javadoc:javadoc
```

## Publishing to Maven Central

The SDK is published to Maven Central via the [Sonatype Central Portal](https://central.sonatype.com).

### Prerequisites

1. **Sonatype Central Portal account**: Register at [central.sonatype.com](https://central.sonatype.com)
2. **User token**: Generate a user token from your Central Portal account (Account → Generate User Token)
3. **GPG key**: Generate a GPG key pair for artifact signing (`gpg --full-generate-key`)
4. **Maven settings**: Configure `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>YOUR_TOKEN_USERNAME</username>
            <password>YOUR_TOKEN_PASSWORD</password>
        </server>
    </servers>
    <profiles>
        <profile>
            <id>gpg</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.executable>/path/to/gpg</gpg.executable>
            </properties>
        </profile>
    </profiles>
</settings>
```

### Deploy

```bash
# Deploy release to Maven Central (auto-publishes after validation)
mvn clean deploy -DskipTests -Dgpg.keyname=YOUR_GPG_KEY_ID
```

The `central-publishing-maven-plugin` handles bundling, uploading, and publishing automatically. Artifacts typically appear on Maven Central within 30 minutes of a successful deploy.

### Release Script

A PowerShell release script is included for the full workflow (test, build, tag, GitHub release, Maven Central deploy):

```powershell
# Full release with Maven Central deploy
.\scripts\release.ps1 -Version "1.2.0" -GpgKeyId "YOUR_KEY_ID" -DeployMavenCentral

# GitHub release only (no Maven Central)
.\scripts\release.ps1 -Version "1.2.0"

# Preview without executing
.\scripts\release.ps1 -Version "1.2.0" -DryRun
```

> **Note:** You must commit and push your changes to Git before running the release script.

### GitHub Releases

Pre-built JARs are also available as [GitHub Releases](https://github.com/iantjones/equinix-java-sdk/releases). Each release includes:
- `equinix-sdk-java-X.Y.jar` - Compiled SDK
- `equinix-sdk-java-X.Y-sources.jar` - Source code
- `equinix-sdk-java-X.Y-javadoc.jar` - Javadoc documentation

## Architecture

The SDK follows a layered architecture:

```
Entry Point (Fabric.java)
  -> Public Client Interface (Connections.java)
    -> Public Client Impl (ConnectionsImpl.java)
      -> Internal Client Interface (ConnectionClient.java)
        -> Internal Client Impl (ConnectionClientImpl.java)
          -> HTTP Layer (ClientBase -> EquinixHttpClient)
```

Resources use either a **full pattern** (with Wrapper + Operator for mutable CRUD) or a **read-only pattern** (JSON model implements the interface directly).

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed architecture documentation.

## Requirements

- **Java 14** or later
- **Maven 3.6+** for building

### Dependencies
- Jackson 2.17.2 (JSON serialization)
- Apache HttpClient 4.5.14 (HTTP communication)
- Lombok 1.18.42 (compile-time code generation)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.
