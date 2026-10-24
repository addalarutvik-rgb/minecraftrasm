# Rasm Client

[Download Rasm Client 0.1.0](https://github.com/addalarutvik-rgb/minecraftrasm/raw/refs/heads/main/downloads/rasm-client-0.1.0.jar)

Install with Fabric Loader and Fabric API for Minecraft **1.21.11**. Save the JAR
in your Minecraft instance's `mods` folder without extracting it.

A Fabric mod for **Minecraft Java 1.21.11** using Java 21, with client-side
flight and an optional server-permitted flight mode. Client-side flight only
requires Fabric Loader, Fabric API, and Rasm on your own client.

## Controls

| Default key | Action |
| --- | --- |
| `[` | Open the Rasm Client menu |
| R | KB flick + immediate Stun Slam |
| G | Toggle client-side flight |
| N | Toggle Nofall |
| Z | One lunge swap (slots 1 / 3) |
| X | One mace swap (slots 1 / 2) |
| B | Add a ghost block under your feet |
| V | Stun Slam: slot 1 → axe 8 → mace 2 |
| H | Toggle ore X-ray |
| J | Shield Stun: slot 1 → axe 8 → slot 1 |

Change every action binding in **Options → Controls → Key Binds → Rasm Client**,
or click **Rebind keys** in the Rasm menu. Server-permitted flight is unbound by
default and can also be assigned a key. Minecraft saves these bindings.
If upgrading from an older build, Minecraft may retain the previous menu key;
set **Open Rasm menu** to **[** there (or reset that binding to its new default).
The KB flick rotates out by +180 degrees, synchronizes the current sprint state,
sends the new facing, and sends an **entity attack in the same client tick**. It
immediately starts Stun Slam against that same locked target: slot 1 → axe in
slot 8, then a mace in slot 2 on the following tick. The mace attack runs before
the camera returns by -180 degrees on that tick (about 50 ms). One R press or
**KB flick + Stun Slam** menu click runs the combination; you no longer need to
press both buttons. Put an axe in slot 8 and a mace in slot 2. Without those items,
the ordinary flick still runs and the swap is silently skipped.
Active swap sequences block a new flick. No block-click/mining action is sent.
This automates the user's reported working combination; multiplayer displacement
with the combined button has not been verified here.

It first locks the living entity under your crosshair **before** turning, then
attacks that same target with the reversed facing. Aim at your opponent and press R.
If there is no aimed target, it falls back to an in-reach player in the rear cone
or a living entity under the backward crosshair. Reach and line of sight are checked at the hit.
If no target qualifies, it still turns, swings, and runs the slot sequence. Pitch is preserved.

For bonus knockback, sprint before triggering R with a charged attack, or use a
Knockback-enchanted weapon. The action preserves your sprint state; it does not
force sprint or grant enchantments. Minecraft 1.21.11 uses the attack yaw for extra
sprint/enchantment knockback, while base knockback also depends on positions.
Consequently, sideways displacement depends on your angle, weapon, cooldown,
resistance, and server mechanics; a 180-degree turn alone cannot guarantee it.
The [reference video](https://www.youtube.com/watch?v=7t0PyqYsac8) was blocked here.

## Swap actions

**Z — Lunge swap once:** slot 1 holds your low-cooldown item (or empty hand),
slot 3 holds a Lunge-enchanted spear. Each press selects slot 1, allows one preparation tick, clicks it, waits for its short
cooldown, clicks with slot 3, and returns to slot 1. Press again to repeat; presses
during an unfinished sequence are ignored. There is no automatic loop. Hunger,
enchantments, and server mechanics still apply.

**X — Mace swap once:** slot 1 holds your initial weapon and **slot 2** holds the
mace. It attacks with slot 1, changes to slot 2 for a second attack, and returns
to slot 1. It does not create fall distance or guarantee extra damage.

Opening a menu, disconnecting, dying, or changing worlds cancels a pending swap.

## X-ray

Press **H** or click **X-ray** in the menu to hide ordinary block models and show
ore blocks and ancient debris at full brightness. Toggle it off to rebuild normal
terrain. H can be rebound, and disconnecting resets the feature. Fluids, entities,
and special block-entity models are not hidden. Collision and mining reach are
unchanged. Only blocks actually sent to your client can be revealed; server
anti-X-ray can conceal ores or send fake ore data. Third-party rendering mods
have not been tested.

## Shield Stun

Press **J** or click **Shield Stun 1 / 8 / 1** in the `[` menu while aiming at an
opponent within melee reach. Put an axe in slot 8. One press selects slot 1,
switches to slot 8, sends one entity attack, and returns to slot 1, all in the same
client tick. No axe-charge wait or mace is required. The key can be rebound.
Missing axes leave your slot unchanged; active
Lunge/Mace/Stun Slam sequences prevent this action from interfering with them.
The attack never mines blocks. The server still decides whether the hit disables
a shield, based on its combat rules and attack state. Live shield-disable behavior
has not been verified for this new action.

## Stun Slam

Put an **axe in hotbar slot 8** and a **mace in slot 2**. Aim at a living entity
within melee reach, then press **V** or click **Stun Slam 1 / 8 / 2** in the menu.
The action selects slot 1, switches to the axe in slot 8 and attacks in the same
client tick, then switches to the mace in slot 2 and attacks on the next tick
(about 50 ms). It restores your original slot one tick later. There is no axe-charge
wait. Slot 1 is selected without sending an attack, preserving existing attack
charge as far as the server permits. Each press runs one sequence.

Both attacks use the aimed living entity when it remains alive, visible, and in reach.
Without a valid target, the sequence continues with air swings. This never sends a block-mining click. Put a low-cooldown item
in slot 1. Fast switching does not guarantee bypassing server cooldowns, shield
disabling, or smash damage; falling distance and server combat rules still apply.
The revised fast sequence and front-target KB displacement need live PvP testing.

## Entity glow

Use `/glow Rasmore` to highlight one online player by username (case-insensitive).
Player names autocomplete from the current server's player list. Repeat with other
names to select multiple players; existing mob filters remain active. Selections
track player UUIDs and apply when those players are loaded, including after they
come into render distance. `/glow only Rasmore` replaces all filters with that
player. For names that collide with a filter or command, use `/glow player NAME`
(or `/glow only player NAME`). Offline/unknown names report an error without
clearing existing selections.

Filters stack: `/glow slime` followed by `/glow creeper` highlights both types.
`/glow @a` or `/glow players` adds all loaded players. `/glow hostile` adds mobs
in Minecraft's MONSTER category; `/glow passive` adds other mobs (including neutral
animals; this is a type category, not their current aggression state).

For an exclusive selection, use `/glow only hostile`, `/glow only passive`, or
`/glow only players`. `/glow only creeper` also works. `/glow clear` and `/glow off`
remove all custom filters. Vanilla glow is preserved. Filters reset on disconnect.
These commands run locally and only affect entities loaded by the client.

## Ghost blocks

Every press of **B** or **Place ghost** adds another full client-only glass block
under your feet, provided that cell is air. Move or jump to place more blocks;
existing ghost blocks stay in place. The menu displays the tracked block count.
**`/ghost clear` removes every tracked ghost block.** Respawning, disconnecting,
or changing worlds also clears them. Real blocks are never replaced, and server
block updates can remove the local illusion. The server still sees air.

The linked [Ghostblock project](https://modrinth.com/mod/ghostblock) could not be
fetched from this environment; this implements the requested behavior directly.

## Client-side flight

Press **G** to enable/disable local flight. Use your normal forward/back/strafe
keys to move relative to your camera, Jump to ascend, Sneak to descend, and Sprint
for faster horizontal movement. Releasing movement keys hovers locally. The
`[` menu shows whether client flight is ON or OFF. Menus stop directional
flight input; closing the menu resumes normal controls.

This mode does not require a mod, allowlist entry, or flight permission on the
server. It changes local gravity and velocity and sends normal movement updates.
It does not implement an anti-cheat bypass: a server may correct your position,
disconnect you, or flag the movement. It is not guaranteed to work on any given
server. Disabling flight restores local gravity; server fall damage still applies.

Client flight resets on disconnect, respawn, dimension changes, death, entering
a vehicle, or sleeping. It does not change your game mode or vanilla flight
permissions. The quick-turn hotkey is independent of flight.

## Nofall

Open the **[** menu and click **Nofall** to toggle it. When enabled, falling
movement packets report an on-ground state once the local fall distance exceeds
2.5 blocks. This attempts to reset server-side fall tracking without teleporting
or changing health. Jumping, hovering, riding, swimming, and vanilla flight are
excluded. The toggle resets after death, respawn, dimension changes, and disconnect.

There is no fall-height cap in this mode. However, damage is decided by the server;
a server can validate or ignore the reported ground state. Nofall cannot guarantee
zero damage from every height or on every server. It does not block other damage
such as lava, suffocation, or the void.

## Optional server-permitted flight

1. The server owner installs **Fabric Loader for 1.21.11**, **Fabric API for
   1.21.11**, and the Rasm mod JAR in the server's `mods` folder.
2. Start the server once. Rasm creates `config/rasm-flight-allowed-players.txt`
   in the server's working directory, initially granting no multiplayer access.
3. Stop the server. Add the UUID of each permitted player, one per line. You can
   find a connected player's UUID in the server's `usercache.json`. A denied
   flight request also displays the requesting player's UUID.
4. Restart the server to load the list. Each permitted player installs Fabric,
   Fabric API, and the same Rasm JAR on their client, then uses **Request server-permitted flight** in the `[` menu.

Example allowlist (replace this example UUID with your actual player UUID):

```text
# Comments and blank lines are supported
8ae4ca8d-e16b-4dac-a67f-6948b898a69b
```

The server checks the actual sender's UUID; clients cannot request flight for
other players or grant themselves permission. Invalid configuration disables
multiplayer flight and logs the error. Edit the file and restart the server to
change permissions. A server without Rasm displays an unsupported-server message
on the client instead of receiving an unsupported flight request.

The single-player world's owner is allowed automatically. LAN guests need to be
listed just like other multiplayer players. On a dedicated server, even operators
must be listed. The same JAR can be installed on either side.

Flight changes server-authoritative abilities while keeping survival mode.
Use Jump to ascend and Sneak to descend. Vanilla double-tap Jump also starts/stops
flying while permission is enabled. Use the same menu button to revoke Rasm's grant. Disabling in
midair can cause fall damage after you start falling. Flight is reset on respawn,
disconnect, and server shutdown. Creative/spectator abilities remain with vanilla.
Existing flight permission is restored when a Rasm grant ends.

This optional mode requires server support and permission. Client-side flight
remains available separately using G. The server-permitted mode does not require enabling
`allow-flight` in `server.properties`; it grants each permitted player vanilla
flight abilities on the server. Other server mods may impose additional rules.

## Build and install

1. Install **JDK 21** (a Java runtime alone is insufficient).
2. Run `./gradlew build` on Linux/macOS or `gradlew.bat build` on Windows.
3. Install Fabric Loader and Fabric API for Minecraft **1.21.11**.
4. Copy `build/libs/rasm-client-0.1.0.jar` into the instance's `mods` folder.
   Do not use the `-sources.jar` file.

Development client: `./gradlew runClient`. Dedicated development server:
`./gradlew runServer` (review and accept Minecraft's EULA in `run/eula.txt` first).
Tests: `./gradlew test`. The wrapper verifies Gradle's SHA-256 checksum.

## Validation

`./gradlew build` succeeded and produced the remapped client/server JAR. All
18 JUnit tests passed (five movement tests, four permission tests, three
turn-state tests, three NoFall selection tests, and three backward target-selection tests).
The dedicated-server launch loaded Rasm in the server environment and stopped
at Minecraft's EULA gate (`run/eula.txt` remains `eula=false`).

The client was also launched under a virtual display into a disposable survival
world. The redesigned menu and toggles were inspected in-game. A cow behind the
player lost one health point when R performed the turn/attack, and the player's
rotation returned to the starting heading with pitch preserved. With Nofall ON,
a 160-block fall left the player at 20 health; with it OFF, a 15-block fall caused
damage. These are integrated-server checks; public-server behavior is unverified.

## In-game validation checklist

Use a disposable world and a test Fabric server:

- Verify the compact menu, live Flight/Nofall ON/OFF states, and rebinding.
- Place a target behind the player within vanilla reach; confirm R turns, sends
  attacks in the flick tick, then returns. Confirm no ground blocks
  are clicked when there is no eligible target.
- Toggle Nofall and test short and long drops on a disposable world/server; compare
  damage with the toggle off. Check death/dimension/disconnect reset.
- With only the client mod installed, check G, camera-relative movement, Jump/
  Sneak, Sprint, idle hovering, GUI input, and toggle-off.
- Check automatic client-flight reset on respawn and dimension changes.
- On a new modded server, confirm an unlisted player's server-flight menu request is denied.
- Add that player's UUID, restart, and confirm the server-flight menu button enables flight.
- Confirm another, unlisted player remains unable to enable flight.
- Test toggle-off, respawn, disconnect/rejoin, server restart, and creative/
  spectator transitions. Verify pre-existing flight permission is preserved.
- Confirm single-player supports both modes without an allowlist entry.
- Join a server without Rasm and confirm client flight can be toggled locally,
  while the server-flight menu button displays the unsupported message.

Automated movement, permission, and turn tests do not replace these interactive checks.

### Latest local regression checks

- Placed two ghost blocks at different coordinates; `/ghost clear` reported two removed.
- Exercised additive slime/creeper filters, `@a`, and all three exclusive groups.
- One spear press produced a roughly three-block lunge, with no repeated loop.
- Mace selection used slot 2 and returned to slot 1.
- Same-tick flick/attack damaged a local test target and produced knockback with
  a Knockback-enchanted weapon; yaw returned on the next tick. No block-interaction
  method is called by this action.
- X-ray revealed a diamond ore enclosed in stone with visible textures; toggling
  it off restored the stone enclosure and normal terrain. Tested with the default
  Fabric renderer, without third-party rendering mods.
- Player selection has unit coverage for the rear cone, range, and height; live
  multiplayer PvP behavior has not been verified.

### Silent combat actions

KB Flick, Stun Slam, and Shield Stun work when aiming into empty space or at
blocks: they perform their turns, slot swaps, and swings without mining blocks.
A missing, dead, or out-of-reach target does not cancel the slot sequence.
The required axe/mace must still be in the documented slots. Combat actions
silently skip invalid equipment and do not print chat or action-bar messages.
