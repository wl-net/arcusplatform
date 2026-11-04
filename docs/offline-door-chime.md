# Offline Door Chime

## Problem

The hub chimes when a door opens, but only when connected to the platform. The cloud-side `DoorsNLocksSubsystem` detects `Contact.contact = OPENED`, checks per-device `DoorChimeConfig`, and sends `HubChimeCapability.chimeRequest` to the hub. When the hub is offline, the attribute change never reaches the subsystem, so no chime plays.

Some contact sensor drivers have hub-local reflexes that set `Contact.contact = OPENED` immediately: the ZigBee contact drivers and the Ring Z-Wave contact sensor. Most other Z-Wave contact drivers and IPCD do not, so their state only changes on the platform. The missing piece is triggering the hub speaker locally after the reflex fires, without losing the platform chime for sensors that have no reflex.

## Design

Push chime config to hub via platform messages; detect contact-open locally in `ReflexController`.

### Data Flow

```
PLATFORM                                    HUB

DoorsNLocksSubsystem                        ReflexController
  │                                           │
  ├─ On hub reconnect / config change:        │
  │   Build set of protocol addresses         │
  │   for chime-enabled contact sensors       │
  │                                           │
  ├─ SyncChimeConfig(enabledDevices) ───────> │
  │                                           ├─ Store in SQLite (ReflexDao)
  │                                           ├─ Cache in memory
  │                                           │
  │                                      (later, device message arrives)
  │                                           │
  │                                           ├─ Reflex fires:
  │                                           │   Contact.contact = OPENED
  │                                           ├─ Check chime config
  │                                           ├─ Device in enabled set?
  │                                           │   YES → IrisHal.setSounderMode(CHIME)
  │                                           │   NO  → skip
```

### One Chime Per Opening

The platform keeps sending `hubchime:chime` for every chime-enabled sensor that opens, because it cannot tell which drivers chime on the hub. The request now carries an optional `device` parameter: the sensor's `protocol:protocolid`, the same name the config push uses.

The hub pairs requests with its own chimes (`LocalChimeTracker`):

- When the hub chimes for a sensor, it records one pending entry for that sensor. Open, close, open records two.
- When a `hubchime:chime` names a sensor with a pending entry, the hub removes one entry and does not play. Otherwise it plays as before.
- Time never decides whether a request is a duplicate. It only clears entries the platform will never answer, such as a door that opened and closed while the hub was offline, which the platform never sees. An entry expires 60 seconds after it is made while the hub is connected. While the hub is disconnected (not `AUTHORIZED` in `LifeCycleService`) entries do not age, and they get the full 60 seconds from the reconnect, so the platform catching up after reconnect does not chime twice.

### Backward Compatibility

`hubchime:supportsLocalChime` is reported by hubs with this change. The platform only sends `SyncChimeConfig` to hubs that report it, and pushes on subsystem start, on chime config changes, when the hub reconnects, and when the hub first reports the attribute.

- **Old hub, new platform:** the hub never gets `SyncChimeConfig`, never chimes on its own, and ignores the `device` parameter, because the hub decodes message bodies as untyped maps and its chime handler never reads the body. It chimes from the platform as before.
- **New hub, old platform:** the hub never gets `SyncChimeConfig` and the requests name no sensor, so it chimes from the platform as before.
- **Keypads:** always sent `KeyPadCapability.ChimeRequest`, unchanged.

### Contact-Open Detection

In `ReflexController.recv(ProtocolMessage)`, around the `processor.handle()` call:

1. Before handling: capture `processor.getAttribute("cont:contact")`
2. After handling: read again
3. If transitioned from non-OPENED to OPENED, and device is in the chime-enabled set, call `IrisHal.setSounderMode(SounderMode.CHIME)`

The `processor.getCapabilities().contains(ContactCapability.NAME)` check avoids unnecessary attribute reads for non-contact devices.

## Files Changed

| File | Change |
|------|--------|
| `common/arcus-model/.../capability/hubchime.xml` | Add `supportsLocalChime` attribute, `SyncChimeConfig` method, optional `device` on `chime` |
| `agent/arcus-reflex-controller/.../ReflexDao.java` | Add `getChimeEnabledDevices()` / `putChimeEnabledDevices()` |
| `agent/arcus-hub-controller/.../SoundHandler.java` | Handle `SyncChimeConfigRequest`, persist to ReflexDao; drop a `chime` request the hub already played |
| `agent/arcus-reflex-controller/.../LocalChimeTracker.java` | Pair local chimes with platform requests per sensor |
| `agent/arcus-reflex-controller/.../ReflexController.java` | Cache chime config, detect contact-open, trigger chime, set `supportsLocalChime` on startup |
| `platform/arcus-subsystems/.../DoorsNLocksContextAdapter.java` | Add `pushChimeConfigToHub()` for hubs reporting `supportsLocalChime`; name the sensor on `hubchime:chime` |
| `platform/arcus-subsystems/.../DoorsNLocksSubsystem.java` | Push chime config on startup, config change, hub reconnect, and when the hub reports `supportsLocalChime` |

## Testing

1. Run model code generator: `./gradlew :common:arcus-model:platform-messages:build`
2. Build hub agent modules: `./gradlew :agent:arcus-reflex-controller:build :agent:arcus-hub-controller:build`
3. Build subsystem module: `./gradlew :platform:arcus-subsystems:build`
4. Run the DoorsNLocks tests: `./gradlew :platform:arcus-subsystems:test --tests '*DoorsNLocks*'`
5. Run the pairing tests: `./gradlew :agent:arcus-reflex-controller:test`
6. Manual test: pair a contact sensor with a hub reflex, enable chime, disconnect the hub from the platform, open the sensor. The hub should chime once, and not again when it reconnects.
