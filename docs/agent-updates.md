# Agent-Only Updates (No hubOS)

The hub firmware update system supports two update types: full hubOS updates and
agent-only updates. Agent-only updates replace just the Java agent process without
reflashing the OS or rebooting the hub.

## Update Types

| Type | Capability Value | Exit Code | What Changes |
|------|-----------------|-----------|--------------|
| Full hubOS | `FIRMWARE` | 82 | Entire OS image, deletes agent, reboots |
| Agent-only | `AGENT` | 83 | Agent tarball extracted, agent restarts |

## How It Works

### 1. Platform sends FirmwareUpdateRequest

The platform sends a `hubadv:FirmwareUpdate` request to the hub with:

- `url` — download URL for the firmware file
- `type` — `FIRMWARE` or `AGENT`
- `priority` — `NORMAL`, `URGENT`, or `BELOW_MINIMUM`

### 2. Agent downloads the file

`FirmwareUpdateHandler` downloads the file via HTTP to a temp directory, then
renames it based on type:

- `FIRMWARE` → `/data/iris/data/tmp/hubOS.bin`
- `AGENT` → `/data/iris/data/tmp/hubAgent.bin`

Progress events (`hubadv:FirmwareUpgradeProcess`) are sent back to the platform
in 10% increments.

### 3. HAL triggers the install

`IrisHalImpl.installHubFirmware()` calls `System.exit()` with the appropriate
exit code. The `iris-agent` startup script handles the rest.

### 4. Startup script performs the install

For **hubOS** (exit 82):
```bash
/usr/bin/update -f 'file:///data/iris/data/tmp/hubOS.bin'
rm -rf '/data/agent'
hub_restart 0
```

For **agent-only** (exit 83):
```bash
mv '/data/agent' '/data/agent-backup'
mkdir '/data/agent'
cd /data/agent
tar xzf '/data/iris/data/tmp/hubAgent.bin'
rm -rf '/data/agent-backup'
exec iris-agent   # restart without reboot
```

Agent-only updates have a built-in rollback — if extraction fails, the backup is
restored and the old agent restarts.

## Agent Package Format

The agent tarball is built by `agent/arcus-agent/hub-v2/build.gradle` using the
Gradle `distributions` plugin. Contents:

```
bin/iris-agent          # startup script
libs/*.jar              # all agent JARs
conf/                   # logback.xml, agent.version, etc.
lib/                    # native libraries (.so)
```

The tarball is extracted directly into `/data/agent/` on the hub.

## Version Attributes

The hub reports both versions separately via `HubAdvanced` capability attributes:

- `hubadv:osver` — hubOS version (from `/tmp/version` on the hub)
- `hubadv:agentver` — agent version (from `conf/agent.version` in the agent install)

## Priority Handling

| Priority | Behavior |
|----------|----------|
| `URGENT` | Install immediately, even on 4G backup connection. `force=true`. |
| `NORMAL` | Skip if hub is on 4G backup connection. |
| `BELOW_MINIMUM` | Sets LED to `UPGRADE_ROOTFS` and plays firmware-update sounder. |

## Key Files

| File | Role |
|------|------|
| `agent/arcus-hub-controller/.../FirmwareUpdateHandler.java` | Download and install orchestration |
| `agent/arcus-hal/hub-v2/.../IrisHalImpl.java` | HAL — maps type to exit code |
| `agent/arcus-hal/hub-v2/src/dist/main/bin/iris-agent` | Startup script, handles exit codes |
| `agent/arcus-agent/hub-v2/build.gradle` | Agent tarball packaging |
| `common/arcus-model/.../capability/hubadvanced.xml` | Capability definition (types, priorities) |
| `platform/.../hub/registration/HubRegistrationRegistry.java` | Platform-side upgrade trigger |
| `platform/.../firmware/XMLFirmwareResolver.java` | Firmware version matching |
| `platform/.../hub-bridge/src/dist/conf/firmware.xml` | Firmware version definitions |

## Current Status

The agent side is fully implemented — `FirmwareUpdateHandler` and the `iris-agent`
script handle `TYPE_AGENT` correctly. However, the platform side has no code path
that sends agent-only updates:

- `HubRegistrationRegistry.upgrade()` hardcodes `TYPE_FIRMWARE`
- `firmware.xml` only defines hubOS targets (e.g. `IH200/hubOS_2.2.0.008`)
- `XMLFirmwareResolver` has no concept of agent version resolution

### What's needed to enable agent-only updates

1. **Agent firmware definitions** — either a separate `agent-firmware.xml` or an
   extension to the existing schema with a `type` attribute to distinguish OS vs
   agent targets.

2. **Platform trigger logic** — compare `hubadv:agentver` against a target agent
   version and send `FirmwareUpdateRequest` with `TYPE_AGENT` when only the agent
   is out of date.

3. **Hosted agent tarballs** — the agent `.tar.gz` needs to be hosted at a URL
   the hub can download from, using the same scheme as hubOS binaries.

## Benefits Over hubOS Updates

- No OS reflash or hub reboot — only the JVM restarts
- Built-in rollback on extraction failure
- Faster — smaller download (JARs only, no OS image)
- Hub stays on current network connection (no 4G reconnect cycle)
- Can iterate on agent code without building/signing a full hubOS image
