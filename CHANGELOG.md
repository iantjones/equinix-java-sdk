# Changelog

All notable changes to the Equinix Java SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-02-19

### Added - New Domains
- **Internet Access**: Services, Ports, and Routing Configurations
- **Projects**: Project management
- **Messaging**: Notification subscriptions and events

### Added - Fabric Expansion
- Cloud Routers with router packages
- Streams and Stream Subscriptions for real-time data
- Route Filters and Route Filter Rules
- Networks (EVPLAN, EPLAN, IPWAN)
- Route Aggregations and Route Aggregation Rules
- Routing Protocols
- Precision Time services
- Marketplace Subscriptions
- Fabric Gateway packages
- Cloud Events
- Health endpoint
- **Dry Run** validation on Connection and ServiceToken builders (`dryRun()`)
- Prices with filtering
- Connection and Port statistics

### Added - Customer Portal Expansion
- Invoices (summaries and details)
- Resellers and reseller customers
- Cross-Connects (full CRUD)
- Orders (full CRUD)
- Trouble Tickets (full CRUD)
- Work Visits (full CRUD)
- Smart Hands requests (full CRUD)
- Shipments (full CRUD)
- Notifications
- Assets
- Support Cases (full CRUD)
- Quotes (full CRUD)
- Support Plans
- Order History
- Lookups (locations, patch panels)
- Attachments (full CRUD)
- Reports
- Digital LOAs (full CRUD)
- Secure Cabinets (full CRUD)
- Unified Notifications
- Billing Credits

### Added - IBX SmartView Expansion
- Streaming Subscriptions (full CRUD with AWS IoT, Azure Event Hub, Webhook, REST channels)
- System Alerts (search with filtering)
- Hierarchy APIs (location and power hierarchy)
- Smart View Assets (list, details, search, tag points)
- Legacy Environmental APIs (current readings, trending data)
- Legacy Power APIs (current readings, trending data)
- New enums: LevelType, AssetClassification, ChannelType, StreamingMessageType, AlertStatus, DataPoint

### Added - Core Improvements
- Fluent `EquinixRequestBuilder` replacing verbose request construction
- Comprehensive exception hierarchy (401/403/404/409/429/5xx)
- `@Delegate` pattern on all wrapper classes
- Per-resource `defaultVersion` override in apiParams JSON
- `overrideUriFormat` for legacy SmartView URI patterns
- Rich-object builder overloads (22 methods across 13 operator files)
- `ResourceRef` base class eliminating model duplication

### Changed
- Upgraded Jackson from 2.9.9 to 2.17.2
- Upgraded JUnit from 5.5 to 5.10.3
- Upgraded HttpClient to 4.5.14
- Upgraded Lombok to 1.18.34
- Upgraded maven-javadoc-plugin from 2.9.1 to 3.6.3
- Removed Guava and Ebean dependencies
- Moved domain-specific enums from core to fabric package
- Added Mockito 5.11.0 and WireMock 3.5.4 for unit testing

### Fixed
- `Utils.addRequestParams()` now respects per-resource-group `defaultVersion` in apiParams JSON
- CustomerPortal v1 endpoints (SmartHands, Assets, OrderHistory, etc.) now correctly resolve to `/v1/` instead of `/v2/`
- `Account.accountNumber` getter shadowing bug in NetworkEdge
- `DeviceJson` field shadowing from parent class

### Removed
- Removed dead code from core module
- Removed unused enums from core package (6 deleted)

## [1.0.0] - 2021-01-01

### Added
- Initial release with Fabric and Network Edge support
- Fabric: Connections, Ports, Service Tokens, Service Profiles, Metros
- Network Edge: Devices, SSH Users, ACL Templates, VPNs, BGP Peerings, Device Links, Public Keys, Backups, Setup
- IBX SmartView: Environmental sensor readings, Power readings
- Core: OAuth2 authentication, pagination, exception handling
