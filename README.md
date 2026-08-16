# ViaVersionLimiter

ViaVersionLimiter enforces a Minecraft protocol policy at the Velocity login boundary. Unsupported clients are rejected before Velocity connects them to a backend server. You can optionally expose one exact hostname that admits unsupported clients with persistent warnings.

The plugin reads the protocol reported by Velocity. It does not require ViaVersion or ViaBackwards.

## Requirements

- Java 21 or newer
- Velocity 3.6

## Install the plugin

1. Download or build `ViaVersionLimiter-2.0.0-SNAPSHOT.jar`.
2. Copy the JAR into the proxy's `plugins` directory.
3. Start the proxy once to create `plugins/viaversionlimitervelocity/config.yml`.
4. Configure the version policy and bypass hostname.
5. Set `enabled: true`.
6. Run `/viaversionlimiter reload` or restart the proxy.

The default configuration is disabled so a new installation cannot reject players before its policy is reviewed.

## Connection behavior

The policy has three outcomes:

- A supported protocol connects normally.
- An unsupported protocol using the exact bypass hostname connects and receives the configured warnings.
- Every other unsupported connection is rejected during `LoginEvent`, before it reaches a backend server.

Hostname matching is case-insensitive, ignores a trailing DNS dot, and requires an exact match. A missing virtual hostname does not qualify for bypass access.

Reloading an enabled configuration applies it to connected players. Unsupported players on the bypass hostname remain connected. Unsupported players on other hostnames are disconnected.

## Upgrade from v1

Version 2 uses a new configuration structure and requires `config-version: 2`. It refuses to load the legacy format instead of inferring a policy that could accidentally allow or reject players.

Move the old values as follows:

| v1 setting | v2 setting |
| --- | --- |
| `whitelist: true` | `policy.mode: ALLOWLIST` |
| `whitelist: false` | `policy.mode: BLOCKLIST` |
| `versions` | `policy.versions` |
| `allowed-domain` | `policy.bypass-domain` |
| `kick-message` | `kick-message` |
| `enable-message` | `notifications.message.enabled` |
| `on-join` | `notifications.message.on-join` |
| `on-server-change` | `notifications.message.on-server-change` |
| `message` | `notifications.message.lines` |
| `broadcast` | `notifications.broadcast.enabled` |
| `broadcast-delay` | `notifications.broadcast.interval-seconds` |
| `bossbar` | `notifications.bossbar.enabled` |
| `bossbar-message` | `notifications.bossbar.message` |
| `bossbar-color` | `notifications.bossbar.color` |
| `actionbar` | `notifications.actionbar.enabled` |
| `actionbar-message` | `notifications.actionbar.message` |

Replace the old file with the bundled [v2 configuration](src/main/resources/config.yml), then apply the relevant values from this table.

## Configuration reference

### Version policy

`policy.mode` controls how `policy.versions` is interpreted:

- `ALLOWLIST` supports only the listed protocol IDs.
- `BLOCKLIST` supports every protocol ID except those listed.

`policy.versions` must contain at least one non-negative protocol ID. Use the [Minecraft protocol version table](https://minecraft.wiki/w/Java_Edition_protocol/Protocol_version_numbers) when translating game versions to protocol IDs.

Set `policy.bypass-domain` to the exact hostname reserved for unsupported clients. Set it to an empty string to disable bypass access.

### Messages

`kick-message` is sent to rejected clients. Values under `notifications` are sent only to unsupported clients admitted through the bypass hostname.

Message strings use legacy ampersand color codes, such as `&c` for red and `&e` for yellow.

The available boss bar colors are `BLUE`, `GREEN`, `PINK`, `PURPLE`, `RED`, `WHITE`, and `YELLOW`.

Both periodic intervals are measured in seconds and must be greater than zero.

## Commands

| Command | Purpose |
| --- | --- |
| `/viaversionlimiter status` | Show the active mode, protocol count, and bypass hostname. |
| `/viaversionlimiter reload` | Validate and atomically activate the configuration. |
| `/vvl` | Short alias for `/viaversionlimiter`. |

Commands require the `viaversionlimiter.admin` permission.

If reload validation fails, the previous valid configuration remains active and the proxy log reports the invalid setting.

## Build from source

Builds require Java 21 or newer and Maven 3.9 or newer.

```bash
mvn clean verify
```

The shaded plugin JAR is written to `target/ViaVersionLimiter-2.0.0-SNAPSHOT.jar`. The verification build runs the policy and configuration tests before packaging.
