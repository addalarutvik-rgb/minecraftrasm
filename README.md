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
| R | Turn 180 degrees, left-click once, then return along the same arc |
| G | Toggle client-side flight |

Change bindings in **Options → Controls → Key Binds → Rasm Client**.
If upgrading from an older build, Minecraft may retain the previous menu key;
set **Open Rasm menu** to **[** there (or reset that binding to its new default).
The quick turn sends the backward facing to the server, refreshes the crosshair
target, and performs one ordinary left click (attack/block click/air swing). It
uses vanilla reach and attack cooldown rules. The quick turn lasts two client ticks (about 100 ms at normal speed), affects
horizontal facing, and leaves pitch unchanged. Mouse yaw during the pulse is
replaced by the original heading on return. It does not trigger while sleeping
or riding. GUI screens consume hotkeys without triggering actions.

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
15 JUnit tests passed (five movement tests, four permission tests, three
turn-state tests, and three NoFall selection tests).
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
  exactly one left click at that target, and returns. Also check air and block clicks.
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
