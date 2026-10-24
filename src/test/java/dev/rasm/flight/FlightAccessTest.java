package dev.rasm.flight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FlightAccessTest {
    @TempDir Path directory;
    private static final UUID ALLOWED = UUID.fromString("8ae4ca8d-e16b-4dac-a67f-6948b898a69b");
    private static final UUID OTHER = UUID.fromString("87558446-39f9-494c-9d7b-8af27f40dc4c");

    @Test void freshServerDeniesMultiplayerButAllowsTheSingleplayerOwner() throws IOException {
        Path config = directory.resolve("config/flight.txt");
        FlightAccess access = FlightAccess.load(config);
        assertTrue(Files.exists(config));
        assertFalse(access.permits(ALLOWED, false));
        assertTrue(access.permits(ALLOWED, true));
    }

    @Test void onlyListedUUIDsGainPermissionAndExistingFileIsPreserved() throws IOException {
        Path config = directory.resolve("flight.txt");
        String contents = "# permitted players\n" + ALLOWED + " # test player\n\n";
        Files.writeString(config, contents);
        FlightAccess access = FlightAccess.load(config);
        assertTrue(access.permits(ALLOWED, false));
        assertFalse(access.permits(OTHER, false));
        assertEquals(contents, Files.readString(config));
    }

    @Test void malformedConfigFailsClosedRatherThanPartiallyAllowingPlayers() throws IOException {
        Path config = directory.resolve("flight.txt");
        Files.writeString(config, ALLOWED + "\nnot-a-uuid\n");
        assertThrows(IOException.class, () -> FlightAccess.load(config));
        assertFalse(FlightAccess.denyMultiplayer().permits(ALLOWED, false));
    }

    @Test void shortUuidSpellingsAreRejected() throws IOException {
        Path config = directory.resolve("flight.txt");
        Files.writeString(config, "1-1-1-1-1\n");
        assertThrows(IOException.class, () -> FlightAccess.load(config));
    }
}
