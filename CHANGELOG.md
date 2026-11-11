# Arcus Platform Changelog

Pull request numbers refer to [arcus-smart-home/arcusplatform](https://github.com/arcus-smart-home/arcusplatform/pulls?q=is%3Apr+is%3Amerged).

## v2026.11.1 (unreleased)

### Security
- Check who sends a device add, and the place it lands in (#518)
- Let an instance switch off hub backups (#515)

### Hub
- Start hub reflexes when a radio registers late (#513)
- Pass a repeated pairing request on to ZigBee too (#517)
- Keep Z-Wave ids unsigned when decoding them (#516)
- Reduce Hub Agent shutdown time (#511)
- Stop building a class data sharing archive before the agent starts (#512)

### Device Drivers
- Ring Alarm Keypad (2nd gen) (#505)
- Give device settings short labels and units (#510)

## v2026.11.0 (2026-11-08)

### Security
- Passkey (WebAuthn) login in client-bridge (#479)
- Remove the deprecated `ChangePin` REST handler (#480)
- Escape user-entered names in email HTML (#482)
- Tests for place checks no test exercised (#497), and JaCoCo 0.8.15 (#478)

### Z-Wave and ZigBee
- Enter the S2 PIN when pairing a Z-Wave device (#502)
- Put Z-Wave leak sensors in the hub's water alarm (#507)
- Start a ZigBee device's hub reflexes over when it is added again (#493)
- Keep every message in `ZigbeeContext` `sendOrdered` and `sendDelayed` (#494)
- Add an `onStarted` driver event, and stop Z-Wave dimmers resending a reported ON (#492)

### Device Drivers
- New: Minoston MP21ZD plug-in dimmer (#483), Zooz ZEN72 dimmer (#490)
- Device settings for the BE469, the 40A outdoor switch, the Ring extender and the plug-in dimmer; signed parameters decoded (#485)
- Fix Minoston MP22ZD settings, and stop it writing configuration on connect (#486)
- Stop forcing scene control on for ZEN77 dimmers (#481), and fix its swapped paddle buttons (#488)
- Stop the ZEN32, ZEN27 and ZEN34 writing configuration on connect (#489)
- Let a T6 Pro moved from another driver turn the fan on (#487)

### Oculus
- Sort the device list by name (#484)

### Removed
- The leftover Sercomm camera code (#504)
- The Travis and Jenkins CI configs (#503)

### Build and Dependencies
- Groovy 4.0.30 → 5.1.3, keeping drivers behaving the same (#506)
- Netty group (#499) and Google group (#500) updates
- Retry infrastructure downloads, and stop the khakis build at the first failure (#496)
- Correct the serialization guidance and stale build docs (#495)

## v2026.10.5 (2026-11-02)

### Security
- Scope client requests to the objects' places (#476)
- Add a switch to disable risky operations (#475)
- Apache Shiro 1.13.0 → 2.2.1 (#467)
- kafka-clients 2.8.2 → 3.9.2, with producer `acks` pinned to 1 (#466)

### Features
- Device settings editor in Oculus (#472)
- Replace SmartyStreets address validation with a local ZIP code check (#468)

### Bug Fixes
- Use the configured Cassandra datacenter in modelmanager (#474)
- Fix the JDK setup instructions for macOS (#473)

### Dependencies
- Gson 2.15.0 (#464), joda-time 2.15.1 (#471), logback 1.6.6 (#470)

## v2026.10.4 (2026-10-28)

### Changes
- Merge alarm-service into subsystem-service (#446)

### Dependencies
- Lucene 8.11.4 → 9.12.3, following the artifact rename (#462)
- Move versions hardcoded in subprojects into `ext.libraries` (#463)
- validation-api 2.0.1 (#459), hibernate-validator 8.0.6 (#453), curator-test 5.9.0 (#452), xbean-finder 5.0.0 (#450), servlet-api 4.0.1 (#457)

### Bug Fixes
- Stop `TestActionList` depending on the wall clock (#465)

## v2026.10.3 (2026-10-25)

### Removed
- Video services and camera code paths (#438)
- The unused Azure resource factory and azure-storage (#439)
- The analytics tagging pipeline (#440)

### Bug Fixes
- Decode the full Z-Wave meter scale in the energy drivers (#443), and accept power reports whatever their rate type (#444)
- Fix Kafka client properties the client never recognized (#447)

### Build and Dependencies
- Build releases on a schedule, not only on a tag (#383)
- handlebars 2.2.2 → 4.3.1 and antlr4 4.13.2 (#448, #460)
- pushy 0.15.6 (#427), oshi-core 7.7.2 (#445)

## v2026.10.2 (2026-10-24)

### Security and Reliability
- Alertable security and reliability metrics (#397)
- Log `RoleAuthorizer` denials at WARN (#436)
- Report hub firmware upgrade failures that used to go silent (#381)
- Record thermostat fan mode, emergency heat and API key changes in history (#379)

### Bug Fixes
- Send a device's whole pairing state so its pairing device can't get stuck (#375)
- Log the new active place in client-bridge's `place.setactive` session line (#437)

### Removed
- Billing infrastructure (Recurly) (#378)
- The unused MediaPlayer and HubAV capability stubs (#377)
- The GCM XMPP transport, keeping the provider as a skeleton (#432)

### Build and Infrastructure
- Infrastructure images on Debian trixie (#373)
- Dependabot for Gradle, GitHub Actions and Docker base images (#382, #396, #414)
- Ignore the root project's build directory (#420)

### Dependencies
- Netty 4.1.139 (#399) → 4.2.19 (#434), dropping two pins that never took effect (#398)
- Jackson 2.22.4 (#394), Twilio SDK 13.1.0 (#395), JAXB 4.0 / `jakarta.xml.bind` (#428), Mailgun 2.4.2 (#429)
- jna 5.19.1 (#426), oshi-core 7.7.1 (#430), jdt.annotation 2.4.100 (#425)
- Guava 33.7.2 and Gson 2.14.0 (#411), Dropwizard metrics 4.2.40 (#410), easymock 5.7.0 (#408)
- Apache Commons (#409), minor and patch (#412) and logging (#387) groups, GitHub Actions (#393)
- Depend on kafka-clients instead of the Scala broker jar (#417), and port kafka-stream to the modern consumer (#423)
- Drop jctools and guice-servlet, neither of which was used (#413)

## v2026.10.1 (release candidates only, 2026-10-16)

Never released on its own; these changes shipped in v2026.10.2.

### Security
- Bump dependencies to patch known CVEs (#356)
- Randomize the low bits of session ids (#357)

### Hub
- Write reflex driver state in order so the hub database can't keep an old value (#366)
- Pass literal baud rates to termios on glibc 2.42+ (#369)
- Drop JDK 8 and target Java 11 for hub code (#368)

### Bug Fixes
- Fix `hubconn:state` sticking at HANDSHAKE after a hub reconnects (#362)
- Fix the bridge session gauge and counters drifting upwards (#361)
- Clear mispaired state when a device moves from a fallback driver to a real one (#371)
- Stop Poll Control check-ins on water leak sensors (#358)
- Fix the battery type for the Iris v3 security keypad (IL021) (#365)
- Fix flaky `TestAES.testAesGcmManipulated` (#355)

### Dependencies
- ZooKeeper 3.9.6 and Cassandra 4.0.21 (#359)

## v2026.3.3 (2026-03-31)

### Features
- TLS 1.3 for hub-bridge, with TLS 1.2 fallback (#343)
- Let client-bridge REST handlers be disabled by configuration (#340)
- Lazy driver loading to reduce driver-services heap use, as an experiment (#341)
- Leave `devadv:protocolAttrs` out of internal device model loading (#345)

### Device Drivers
- Minoston MR40Z 4-button remote (#344), EnergyHub Socket SK-ZP ZigBee plug (#352)
- Samjin water leak sensor in the SmartThings leak driver (#348)
- Instanced (multi-outlet) Zooz devices (#353)

### Bug Fixes
- Periodically re-assert hub connected state to fix stale writes (#350)
- Fix Oculus `DefaultRowSorter` `ArrayIndexOutOfBoundsException` on column sort (#337)
- Fix place selection dialog not closing on "Login as a New User" (#337)
- Guard `ApiKeyController` and `SceneController` against null `placeId` (#337)
- Add confirmation dialogs to destructive Oculus actions (#337)
- Fix flaky scheduler tests (#349, #351)

### Build
- CI caching with setup-gradle v4 and the Gradle build cache (#346)
- Driver `setAttributes` smoke test (#347)
- OpenCSV 3.4 → 5.10 (#339)

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
