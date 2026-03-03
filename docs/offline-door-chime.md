# Offline Door Chime

## Problem

The hub chimes when a door opens, but only when connected to the platform. The cloud-side `DoorsNLocksSubsystem` detects `Contact.contact = OPENED`, checks per-device `DoorChimeConfig`, and sends `HubChimeCapability.chimeRequest` to the hub. When the hub is offline, the attribute change never reaches the subsystem, so no chime plays.

Contact sensors already have hub-local reflexes that set `Contact.contact = OPENED` immediately. The missing piece is triggering the hub speaker locally after that reflex fires.

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

### Backward Compatibility

A new `hubchime:supportsLocalChime` boolean attribute on `HubChimeCapability` signals whether the hub handles chime locally. Updated hubs set it to `true` on startup.

**Platform behavior in `DoorsNLocksContextAdapter.chime()`:**
- **Keypads**: always send `KeyPadCapability.ChimeRequest` (unchanged)
- **Hub with `supportsLocalChime = true`**: skip cloud chime (hub handles it locally)
- **Hub without attribute / `false`** (old firmware): send `HubChimeCapability.chimeRequest` via cloud (existing behavior)

This eliminates double-chime for updated hubs and preserves existing behavior for old hubs.

### Contact-Open Detection

In `ReflexController.recv(ProtocolMessage)`, around the `processor.handle()` call:

1. Before handling: capture `processor.getAttribute("cont:contact")`
2. After handling: read again
3. If transitioned from non-OPENED to OPENED, and device is in the chime-enabled set, call `IrisHal.setSounderMode(SounderMode.CHIME)`

The `processor.getCapabilities().contains(ContactCapability.NAME)` check avoids unnecessary attribute reads for non-contact devices.

## Files Changed

| File | Change |
|------|--------|
| `common/arcus-model/.../capability/hubchime.xml` | Add `supportsLocalChime` attribute, `SyncChimeConfig` method |
| `agent/arcus-reflex-controller/.../ReflexDao.java` | Add `getChimeEnabledDevices()` / `putChimeEnabledDevices()` |
| `agent/arcus-hub-controller/.../SoundHandler.java` | Handle `SyncChimeConfigRequest`, persist to ReflexDao |
| `agent/arcus-reflex-controller/.../ReflexController.java` | Cache chime config, detect contact-open, trigger chime, set `supportsLocalChime` on startup |
| `platform/arcus-subsystems/.../DoorsNLocksContextAdapter.java` | Add `pushChimeConfigToHub()`, gate hub chime on `supportsLocalChime` |
| `platform/arcus-subsystems/.../DoorsNLocksSubsystem.java` | Push chime config on hub reconnect, startup, and config change |

## Testing

1. Run model code generator: `./gradlew :common:arcus-model:platform-messages:build`
2. Build hub agent modules: `./gradlew :agent:arcus-reflex-controller:build :agent:arcus-hub-controller:build`
3. Build subsystem module: `./gradlew :platform:arcus-subsystems:build`
4. Run existing DoorsNLocks tests: `./gradlew :platform:arcus-subsystems:test --tests '*DoorsNLocks*'`
5. Manual test: pair a contact sensor, enable chime, disconnect hub from platform, trigger sensor — hub should chime
