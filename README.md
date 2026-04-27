# QuickHearth

A server-side homes, spawn, and teleport-request mod for Minecraft 26.1 on Fabric. Built for survival servers that want a friendly UX with rank-aware home limits and a clickable home picker, without the noise of a larger teleport command bundle.

QuickHearth covers `/sethome`, `/home`, `/delhome`, `/homes`, `/spawn`, `/tpa`, `/tpahere` (with a `/tpr` alias), `/tpaccept`, `/tpdeny`, and `/tpatoggle`. It deliberately does not include `/back`, `/rtp`, or `/warp`.

Vanilla clients work out of the box. No client-side mod is required.

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Commands](#commands)
  - [Homes](#homes)
  - [Spawn](#spawn)
  - [Teleport Requests](#teleport-requests)
- [Teleport Behavior](#teleport-behavior)
- [Home Limits](#home-limits)
- [Permission Nodes](#permission-nodes)
- [Data Storage](#data-storage)
- [Configuration](#configuration)
- [Building From Source](#building-from-source)
- [License](#license)
- [Credits](#credits)

## Requirements

- Minecraft 26.1, 26.1.1, or 26.1.2 (Fabric)
- Fabric Loader 0.18 or newer
- Fabric API
- Java 25
- [sgui](https://modrinth.com/mod/sgui) version `2.0.0+26.1` (required, drives the home picker GUI)
- [LuckPerms-Fabric](https://luckperms.net/) (optional, only needed for rank-based home limits and per-command permission gating)

## Installation

1. Download `quickhearth-X.Y.Z+26.1.jar` from the [releases page](https://github.com/nickisashkir/QuickHearth/releases).
2. Drop it into your server's `mods/` folder alongside Fabric API and sgui.
3. (Optional) Drop in LuckPerms-Fabric if you want rank-based home limits.
4. Start the server. QuickHearth auto-creates the `homes_bonus` scoreboard objective on first run.

No config file is required for default behavior.

## Quick Start

```
/sethome basecamp        save your current spot as "basecamp"
/home                    open the picker GUI
/home basecamp           teleport directly to basecamp
/spawn                   teleport to world spawn
/tpa SomePlayer          ask SomePlayer if you can teleport to them
```

> Screenshot placeholder: chat showing the welcome flow.

## Commands

### Homes

#### `/sethome [name]`

Saves your current location as a home. If you do not pass a name, it saves as `home`.

```
/sethome
/sethome basecamp
/sethome iron-farm
```

Names accept letters, numbers, underscores, and hyphens, up to 24 characters. The name `help` is reserved.

If you have already reached your home limit, the command tells you and asks you to delete one first. Setting a home with a name you already use overwrites it (does not count against the limit).

> Screenshot placeholder: chat output of `/sethome basecamp` showing the green "Set home basecamp" confirmation.

#### `/home`

With no argument, opens the home picker. This is a 27-slot chest GUI showing every home you have, each as a clickable banner. The window title shows your usage, like `Homes (3/5)`.

- **Click** a banner to teleport to that home.
- **Shift-click** a banner to delete the home.

If you have no homes yet, the picker shows a paper hint item with instructions for `/sethome`.

> Screenshot placeholder: the chest GUI with several home banners.

#### `/home <name>`

Teleports directly to the named home. Tab-completion suggests the names of homes you own.

```
/home basecamp
```

> Screenshot placeholder: tab-completion menu suggesting home names.

#### `/home help`

Prints the full QuickHearth command list to chat for the player who ran it. Use this if a teammate is unsure how the system works.

> Screenshot placeholder: chat output of `/home help`.

#### `/homes`

Equivalent to `/home` with no argument. Opens the picker GUI.

#### `/delhome <name>`

Deletes a home you own. Tab-completes from your homes.

```
/delhome iron-farm
```

You can also delete a home by shift-clicking it inside the picker GUI.

### Spawn

#### `/spawn`

Teleports you to the world spawn point (the position and direction set by the world's RespawnData).

> Screenshot placeholder: arriving at spawn after `/spawn`.

### Teleport Requests

QuickHearth supports two directions of teleport request: come to me, or send me to you.

#### `/tpa <player>`

You ask the target player if **you** can teleport **to them**. The target sees a chat prompt and can run `/tpaccept` or `/tpdeny`.

```
/tpa Steve
```

What Steve sees:

```
Nicky wants to teleport to you. /tpaccept or /tpdeny.
```

> Screenshot placeholder: chat showing the incoming `/tpa` prompt on the target player's screen.

#### `/tpahere <player>` (alias `/tpr`)

You ask the target player if **they** can teleport **to you**. Reverse direction of `/tpa`. The alias `/tpr` exists because some players prefer the shorter form.

```
/tpahere Steve
/tpr Steve
```

What Steve sees:

```
Nicky wants you to teleport to them. /tpaccept or /tpdeny.
```

#### `/tpaccept`

Accepts the most recent pending request directed at you. The mover (the player who needs to be teleported, depending on the request direction) goes through the standard teleport warmup.

#### `/tpdeny`

Declines the most recent pending request and notifies the requester.

Pending requests expire automatically after 60 seconds.

#### `/tpatoggle`

Toggles whether you receive teleport requests at all. When disabled, anyone trying to `/tpa` or `/tpahere` you sees a "not accepting requests" message. The toggle persists across server restarts.

```
/tpatoggle    -> Teleport requests will be blocked.
/tpatoggle    -> Teleport requests will be accepted.
```

> Screenshot placeholder: chat confirmation after toggling.

## Teleport Behavior

Every teleport in QuickHearth (homes, spawn, accepted TPA) shares the same rules:

- **3-second warmup.** A countdown message tells the player to stand still. Any meaningful XZ movement during the warmup cancels the teleport.
- **30-second cooldown** after a successful teleport, before the player can teleport again.
- A player with one teleport already queued cannot queue a second until the first finishes or is cancelled.

These keep the system survival-friendly: it is hard to abuse for combat escapes, and players cannot chain teleports to outpace mobs or other players.

All durations are constants in `Config.java`. Edit the file and rebuild to change them.

## Home Limits

Each player's maximum number of homes is calculated as:

```
max_homes = lp_meta("homes-max") + scoreboard("homes_bonus")
```

The two layers are independent. Rank changes do not reset bonus grants, and bonus grants do not depend on having LuckPerms set up.

### Layer 1: LuckPerms rank base (optional)

If LuckPerms-Fabric is installed, QuickHearth reads the integer meta value `homes-max` from each player's LuckPerms data. Set it per group:

```
/lp group newcomer meta set homes-max 2
/lp group member meta set homes-max 3
/lp group veteran meta set homes-max 5
```

Or per user, to override the rank:

```
/lp user Steve meta set homes-max 4
```

If LuckPerms is not installed, or no `homes-max` value is set, the rank base falls back to `1` (configurable in `Config.DEFAULT_MAX_HOMES`).

### Layer 2: Scoreboard bonus (always available)

QuickHearth auto-creates a dummy scoreboard objective named `homes_bonus` on server start. Admins grant extra homes with the vanilla `/scoreboard` command:

```
/scoreboard players add Steve homes_bonus 2
/scoreboard players set Steve homes_bonus 5
/scoreboard players reset Steve homes_bonus
```

The scoreboard value is added to the rank base. So a Veteran (rank base 5) with `homes_bonus = 2` can save up to 7 homes total.

### Worked example

| Player | Rank | LP `homes-max` | `homes_bonus` | Total |
|---|---|---|---|---|
| Alice | newcomer | 2 | 0 | 2 |
| Bob | member | 3 | 1 (event prize) | 4 |
| Carol | veteran | 5 | 2 (gifted) | 7 |
| Dave | (no LP) | (default 1) | 3 (granted) | 4 |

The home picker title shows usage as `Homes (used/max)` so players can see at a glance how many slots they have left.

## Permission Nodes

QuickHearth checks LuckPerms permission nodes when LuckPerms is installed. When LuckPerms is absent (or returns UNDEFINED for a node), it falls back to vanilla OP level (GAMEMASTERS, equivalent to `op-permission-level: 2` and above) for restricted commands. By default, every command listed below is allowed for everyone.

| Node | Default | Command |
|---|---|---|
| `quickhearth.command.home` | allow | `/home` |
| `quickhearth.command.sethome` | allow | `/sethome` |
| `quickhearth.command.delhome` | allow | `/delhome` |
| `quickhearth.command.homes` | allow | `/homes` |
| `quickhearth.command.spawn` | allow | `/spawn` |
| `quickhearth.command.tpa` | allow | `/tpa` |
| `quickhearth.command.tpahere` | allow | `/tpahere` and `/tpr` |
| `quickhearth.command.tpaccept` | allow | `/tpaccept` |
| `quickhearth.command.tpdeny` | allow | `/tpdeny` |
| `quickhearth.command.tpatoggle` | allow | `/tpatoggle` |

To revoke a command from a group:

```
/lp group default permission set quickhearth.command.tpa false
```

## Data Storage

QuickHearth stores per-player data under your world folder.

```
<world>/quickhearth/
  homes/
    <player-uuid>.json    one file per player, listing their saved homes
  toggles.json             players who have /tpatoggle disabled
```

A home file looks like this:

```json
{
  "homes": [
    {
      "name": "basecamp",
      "dim": "minecraft:overworld",
      "x": 123.5,
      "y": 64.0,
      "z": -45.5,
      "yaw": 90.0,
      "pitch": 0.0
    }
  ]
}
```

The format is plain JSON, so admins can edit files directly if needed. Stop the server first to avoid race conditions.

## Configuration

There is no runtime config file. Tunable values live as constants in `Config.java`:

| Constant | Default | Meaning |
|---|---|---|
| `WARMUP_TICKS` | 60 (3 seconds) | Time the player must stand still before a teleport completes |
| `HOME_COOLDOWN_TICKS` | 600 (30 seconds) | Cooldown after a successful `/home` |
| `SPAWN_COOLDOWN_TICKS` | 600 (30 seconds) | Cooldown after a successful `/spawn` |
| `TPA_REQUEST_TIMEOUT_TICKS` | 1200 (60 seconds) | How long a TPA request stays pending |
| `DEFAULT_MAX_HOMES` | 1 | Fallback rank base when LuckPerms is absent or `homes-max` is not set |
| `LP_META_KEY` | `"homes-max"` | LuckPerms meta key consulted for the rank base |

Edit and rebuild from source to change these. A future release may externalize them to a JSON config.

## Building From Source

```bash
git clone https://github.com/nickisashkir/QuickHearth.git
cd QuickHearth
./gradlew build
```

The compiled jar lands in `build/libs/`. Java 25 is required.

QuickHearth targets the Fabric 26.1 deobfuscated build setup (no remap, no Yarn). See `build.gradle` for the loom configuration. LuckPerms-API is used as `compileOnly` so the mod jar does not bundle it; LuckPerms is detected at runtime via `Class.forName`.

## License

MIT. See [LICENSE](LICENSE). Editing, redistributing, and forking are all allowed; please retain the copyright notice.

## Credits

- [sgui](https://github.com/Patbox/sgui) by Patbox: drives the chest-style home picker GUI.
- [LuckPerms](https://luckperms.net/) by lucko: provides the permissions layer and the rank-base meta lookup.
