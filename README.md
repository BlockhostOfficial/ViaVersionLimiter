# ViaVersionLimiter

ViaVersionLimiter enforces a Minecraft protocol policy at the proxy login boundary. The same JAR supports Velocity and BungeeCord. Unsupported clients are rejected before the proxy connects them to a backend server, unless they use the configured bypass hostname.

The plugin reads the client protocol reported by the proxy. It does not require ViaVersion or ViaBackwards.

## Requirements

- Java 25 or newer
- Velocity 3.6 or BungeeCord 26.1

## Install the plugin

1. Download or build `ViaVersionLimiter-2.0.0-SNAPSHOT.jar`.
2. Copy the same JAR into the proxy's `plugins` directory.
3. Start the proxy once to generate `config.yml`.
4. Configure the version policy and bypass hostname.
5. Set `enabled: true`.
6. Run `/viaversionlimiter reload` or restart the proxy.

The configuration is stored at:

- Velocity: `plugins/viaversionlimitervelocity/config.yml`
- BungeeCord: `plugins/ViaVersionLimiter/config.yml`

The generated configuration is disabled by default so a new installation cannot reject players before its policy is reviewed.

## Connection behavior

The policy has three outcomes:

- A supported protocol connects normally.
- An unsupported protocol using the exact bypass hostname connects and receives the configured warnings.
- Every other unsupported connection is rejected during the proxy's login event, before it reaches a backend server.

Hostname matching is case-insensitive, ignores a trailing DNS dot, and requires an exact match. A missing or malformed virtual hostname does not qualify for bypass access. The plugin never performs a DNS lookup to decide whether a connection used the bypass hostname.

Reloading an enabled configuration applies it to connected players. Unsupported players on the bypass hostname remain connected. Unsupported players on other hostnames are disconnected.

## Configuration and migrations

Configuration loading, atomic reloads, backups, environment overrides, and schema migrations use [6b6t Commons](https://github.com/6b6t/6b6t-commons). The current schema uses `version: 2` and is shared by both proxy adapters.

Existing configurations are migrated automatically:

- Legacy v1 flat configurations are converted to the nested v2 structure.
- The earlier `config-version: 2` format is converted to the Commons-managed `version: 2` field.
- A timestamped backup is created before migration.

Environment variables use the `CONFIG_VIAVERSIONLIMITER` prefix supported by 6b6t Commons.

### Version policy

`policy.mode` controls how `policy.versions` is interpreted:

- `ALLOWLIST` supports only the listed protocol IDs.
- `BLOCKLIST` supports every protocol ID except those listed.

`policy.versions` must contain at least one non-negative protocol ID. Use the [Minecraft protocol version table](https://minecraft.wiki/w/Java_Edition_protocol/Protocol_version_numbers) when translating game versions to protocol IDs.

Set `policy.bypass-domain` to the exact hostname reserved for unsupported clients. Set it to an empty string to disable bypass access.

### Messages

`kick-message` is sent to rejected clients. Values under `notifications` are sent only to unsupported clients admitted through the bypass hostname.

Message strings use legacy ampersand color codes, such as `&c` for red and `&e` for yellow.

The available boss bar colors are `BLUE`, `GREEN`, `PINK`, `PURPLE`, `RED`, `WHITE`, and `YELLOW`. BungeeCord skips boss bars for clients older than Minecraft 1.9 because those protocols do not support them.

Both periodic intervals are measured in seconds and must be greater than zero.

## Commands

| Command | Purpose |
| --- | --- |
| `/viaversionlimiter status` | Show the active mode, protocol count, and bypass hostname. |
| `/viaversionlimiter reload` | Validate and atomically activate the configuration. |
| `/vvl` | Short alias for `/viaversionlimiter`. |

Commands require the `viaversionlimiter.admin` permission. Velocity command registration and help use 6b6t Commons with StrokkCommands. BungeeCord uses a native adapter over the same configuration and policy services because the Commons command module does not provide a BungeeCord registrar.

If reload validation fails, the previous valid configuration remains active and the proxy log reports the invalid setting.

## Build from source

Builds require Java 25 or newer and Maven 3.9 or newer.

```bash
mvn clean verify
```

The build produces one shaded plugin at `target/ViaVersionLimiter-2.0.0-SNAPSHOT.jar`. It contains both `velocity-plugin.json` and `bungee.yml`, along with relocated 6b6t Commons configuration dependencies.
