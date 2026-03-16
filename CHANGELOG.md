# Arcus Platform Changelog

## v2026.3.3 (unreleased)

### Bug Fixes
- Fix Oculus `DefaultRowSorter` `ArrayIndexOutOfBoundsException` on column sort
- Fix place selection dialog not closing on "Login as a New User"
- Guard `ApiKeyController` and `SceneController` against null `placeId`
- Add confirmation dialogs to destructive Oculus actions

## v2026.3.2 (2026-03-15, RC)

### JDK 21 Support
- Add JDK 17 and JDK 21 build support with CI test matrix
- Upgrade Gradle 7.6.4 → 8.12, Guice 5.1.0 → 6.0.0
- Switch release build from JDK 8 to JDK 21
- Update Docker and runtime config for JDK 17+ (`--add-opens`, deprecated API fixes)

### Dependency Upgrades
- Groovy 2.5.15 → 4.0.30 (incremental: 2.5→3.0→4.0)
- EasyMock 3.3 → 5.5.0, Cucumber 2.x → 6.10.4
- Lucene 5.4.0 → 8.11.4, JAXB 2.2.7 → 2.3.9
- Mailgun SDK 1.0.0 → 1.0.9 for JDK 17+ compatibility
- Replace Jetty with Prometheus HTTPServer in metrics exporter

### New Device Drivers
- Zooz ZEN32 Scene Controller, ZEN34 Remote Switch, ZEN37 4-Button Remote, ZEN77 S2 Dimmer
- Zooz ZSE18, ZSE42, ZSE43 sensors
- Minoston MP22Z Outdoor Smart Plug, MP22ZD Outdoor Dimmer Plug
- Enerwave ZWN-RSM1-PLUS, GE 46203 ZW3010 dimmer
- UltraPro 39351 dimmer, simple Z-Wave and ZigBee generic drivers
- Add `DeviceSettings` capability for discoverable Z-Wave config parameters
- Add `deviceSettings` to existing drivers: ZEN26, ZEN27, ZEN34, ZEN37, ZSE29, Ecolink Tilt, Honeywell T6 Pro

### New Features
- Multi-button rule templates with instance selector and validation
- Expose device protocol fingerprint via `devadv:protocolAttrs`
- Upgrade Driver button in Oculus device toolbar
- Incremental builds for `generateDriversReflexDB`

### Bug Fixes
- Fix Groovy 4 bean property resolution for instanced attributes
- Fix Groovy 4 capability script method dispatch
- Fix `base:instances` not loaded from Cassandra device models
- Fix T6 Pro emergency heat attribute not updating on mode change
- Fix T6 Pro humidity precision and `productId` format
- Fix care behavior overlapping window validation
- Fix ZigBee local processing type mismatch
- Fix NPE in `getUserInSlot` when `DoorLock.slots` is null
- Fix basic report/set handlers calling wrong `GenericZWaveSwitch` method
- Update agent truststore: add ISRG Root X2, remove expired DST Root CA X3

## v2026.3.1 (2026-03-12)

### Cassandra Driver 4.x Migration
- Upgrade Cassandra Java Driver from 3.x to 4.x
- Fix `Date` → `Instant` conversions across all DAOs
- Fix partitioned reads broken by driver 4.x request timeout default
- Fix deadlock in `BaseCassandraCRUDDao` async entity loading

### ZigBee Controller
- Implement ZigBee controller with zsmartsystems 1.4.16
- ZigBee OTA firmware updates, factory reset, `zcl()`/`zclmsp()` commands

### Z-Wave Serial Engine
- Implement Z-Wave serial engine for direct UART communication

### Hub Agent
- ARM32 Netty native library builds with Gradle tasks and Dockerfiles
- Fix serial port I/O and ZigBee device discovery reliability
- Bump agent version to 2.13.26-SNAPSHOT

### Breaking Changes
- Remove Alexa, Google, and voice bridge modules

### Bug Fixes
- Fix subsystem queue overflow during place creation
- Fix NPEs in device sync and hub connected handlers when hub is missing
- Fix `ClassCastException` when removing lost devices with transient protocols
- Fix scheduler NPE, cross-place validation, subsystem save/dispatch bugs
- Fix ZooKeeper cluster service bugs and improve robustness
- Fix firmware overlap check to scope by model, not just population
- Fix `ClassCastException` in Oculus pairing SearchingPage
- Fix spurious PAIRED beep from AlertMe devices on hub startup
- Fix SSL handshake failure log reporting raw nanoseconds instead of milliseconds
- Speed up CI: parallel builds and test execution

## v2026.3.0 (2026-03-04)

### API Bridge
- New `api-bridge` container for service account access via API keys
- Bearer token auth on WebSocket upgrade
- API key CRUD with expiration, 10-key limit, owner-only create/revoke
- Permission calculator and API key management UI in Oculus
- History attribution for API key actions

### Documentation
- Comprehensive documentation overhaul: platform services, agent internals, subsystems, rules, scheduler, testing, tools, build system, khakis infrastructure
- Client-bridge WebSocket protocol, API bridge, alarm state machine, access control model
- MkDocs Material + ReadTheDocs configuration

### Bug Fixes
- Harden client-bridge request handlers
- Fix simulated agent startup and auto-setup
- Add noop address updater (no SmartyStreets dependency)

## v2026.2.1 (2026-02-28)

### New Features
- Add TypeScript capability generator

### Build
- Gate Java 11 build changes on JDK version for backward compatibility
- Upgrade arcus-java base image from Debian Bullseye to Bookworm

## v2026.2.0 (2026-02-23)

### Major Dependency Upgrades
- Gradle 6.9 → 7.6.4
- Guava 19.0 → 33.4.0-jre
- Guice 4.0 → 5.1.0
- Netty 4.1.48 → 4.1.128 (tcnative 1.1.33 → 2.0.75)
- Jackson 2.5.1 → 2.18.6
- Kafka 2.4.0 → 2.8.2
- SLF4J 1.7.36 → 2.0.17, Logback 1.2.13 → 1.3.14
- Metrics 3.2.6 → 4.2.30
- ZooKeeper 3.5.7 → 3.8.4
- Bouncy Castle 1.65 → 1.78 (jdk15on → jdk18on)
- Cassandra driver 3.9.0 → 3.11.5
- SendGrid 4.4.1 → 4.10.3
- Mockito 1.10.19 → 4.11.0 (PowerMock removed)
- Apache Shiro 1.3.2 → 1.13.0

### Security
- Remove Apache Commons Collections 3.2.2 (CVE remediation)
- Upgrade Shiro to fix auth bypass CVEs
- Remove 31 unused dependency declarations
- Pin dynamic dependency versions, remove Codehaus Jackson

### Infrastructure
- Remove Netflix Governator, replace with plain Guice + custom lifecycle
- Upgrade Cassandra 3.11.11 → 4.0.15 on Debian Bookworm
- Upgrade ZooKeeper 3.8.4 → 3.8.6
- Docker 25+ compatibility (replace gradle-docker-plugin with Exec)
- Cassandra health checks: `CassandraHealth` listener, TCP heartbeat, `/check` endpoint
- Return 503 instead of 401 when Cassandra is unavailable
- Cap Cassandra reconnection backoff at 30 seconds
- Kafka log level configurable, protocol versions updated to 2.8
- JVM version guard to fail fast on unsupported Java versions
- Build commit and timestamp in startup banner
- Scheduler OOM fix: fast-forward stale partition offsets

### Email
- Add Mailgun email provider as alternative to SendGrid

### New Device Drivers
- Ring Alarm Flood & Freeze Sensor (Z-Wave)
- Innr SP224 Smart Plug
- CentraLite 3315-G

### Bug Fixes
- Fix `HttpPostRequestDecoder` consuming content buffer in Netty 4.1.128
- Trim CORS header values to fix 502 with Origin header after Netty upgrade
- Send 500 instead of closing channel on uncaught HTTP exceptions
- Downgrade 404 HTTP errors to debug level logging
- Fix stale build timestamp, SLF4J no-provider warnings

### Oculus
- Center all popup dialogs, add hub delete confirmation
- Fix login dialog NPE and window centering
- Populate per-hub dropdown menu with actions and tone controls
- Set PlayTone duration to 0 so tones play once instead of looping
