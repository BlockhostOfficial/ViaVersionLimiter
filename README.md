# ViaVersionLimiter

ViaVersionLimiter enforces a Minecraft protocol policy at the proxy login boundary. The same JAR supports Velocity and BungeeCord. Unsupported clients are rejected before the proxy connects them to a backend server, unless they use the configured bypass hostname.

The plugin reads the client protocol reported by the proxy. It does not require ViaVersion or ViaBackwards.

## Requirements

- Java 25 or newer
- Velocity 3.6 or BungeeCord 26.1

## Install the plugin

1. Download or build `ViaVersionLimiter-<version>.jar`.
2. Copy the same JAR into the `plugins` directory of the proxy.
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
- An unsupported protocol admitted by the bypass policy connects and receives the configured warnings.
- Every other unsupported connection is rejected during the proxy login event, before it reaches a backend server.

Exact hostname matching is case-insensitive and ignores a trailing DNS dot. The plugin does not perform a DNS lookup.

Set the bypass hostname to `*` to admit unsupported clients from every hostname. These clients receive the configured warnings.

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

`policy.versions` must contain at least one non-negative protocol ID. To translate game versions, use the [Minecraft protocol version table](https://minecraft.wiki/w/Java_Edition_protocol/Protocol_version_numbers).

Set `policy.bypass-domain` to an exact hostname for restricted bypass access. Set it to `*` to admit unsupported clients from every hostname. Set it to an empty string to disable bypass access.

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

Builds require Java 25 or newer. The Gradle wrapper downloads the expected Gradle version automatically.

```bash
./gradlew clean build
```

The build runs the test suite. It produces one deployable, shaded plugin at `build/libs/ViaVersionLimiter-<version>.jar`.

Do not install the `-unshaded.jar` from the same directory. This file is an intermediate artifact.

The project version is defined by `mavenVersion` in `gradle.properties`. Gradle expands that value into both proxy descriptors, so the Velocity and BungeeCord metadata always matches the artifact name.

## Build and release automation

The GitHub Actions workflows follow the same version-bump and release sequence used by PistonMOTD:

- `Build and upload JAR` runs tests and builds the shaded JAR for pushes and pull requests, then uploads it as a workflow artifact.
- `Set version` updates `mavenVersion` and commits the change to `main`. It can be run directly or called by another workflow.
- `Publish release` validates the requested versions and commits the release version. It builds the JAR and generates a categorized changelog.
- The same workflow creates the GitHub tag and release. It uploads the JAR and commits the next snapshot version.

To publish a release, run `Publish release` from the GitHub Actions page and provide:

- `version`: the release version without `-SNAPSHOT`, such as `2.0.0`.
- `after-version`: the next development version ending in `-SNAPSHOT`, such as `2.0.1-SNAPSHOT`.

Both version changes are normal commits on `main`. If the build or release fails, the workflow does not commit the next snapshot version.

The `./changelog.sh` command shows a compact list of commits since the latest tag.
