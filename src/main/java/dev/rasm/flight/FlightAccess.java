package dev.rasm.flight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Server-owned UUID allowlist, loaded once per server start. */
public final class FlightAccess {
    private final Set<UUID> allowed;

    private FlightAccess(Set<UUID> allowed) { this.allowed = Set.copyOf(allowed); }

    public static FlightAccess load(Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, "# Rasm multiplayer flight: one player UUID per line.\n"
                + "# Only listed players may toggle flight. Restart the server after editing.\n");
        }
        Set<UUID> allowed = new HashSet<>();
        for (String line : Files.readAllLines(file)) {
            String value = line.split("#", 2)[0].trim();
            if (value.isEmpty()) continue;
            try {
                UUID id = UUID.fromString(value);
                if (!id.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
                allowed.add(id);
            } catch (IllegalArgumentException error) {
                throw new IOException("Invalid UUID in " + file + "; multiplayer flight disabled", error);
            }
        }
        return new FlightAccess(allowed);
    }

    public static FlightAccess denyMultiplayer() { return new FlightAccess(Set.of()); }

    public boolean permits(UUID player, boolean singleplayerOwner) {
        return singleplayerOwner || allowed.contains(player);
    }
}
