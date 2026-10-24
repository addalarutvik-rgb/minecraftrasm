package dev.rasm.flight;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightMotionTest {
    private static final double EPSILON = 1e-9;

    @Test void forwardFollowsCameraYaw() {
        var south = FlightMotion.calculate(0, 1, 0, false, false, false);
        assertEquals(0, south.x(), EPSILON);
        assertEquals(.30, south.z(), EPSILON);
        var west = FlightMotion.calculate(90, 1, 0, false, false, false);
        assertEquals(-.30, west.x(), EPSILON);
        assertEquals(0, west.z(), EPSILON);
    }

    @Test void strafingAndBackwardsHaveCorrectDirections() {
        assertEquals(.30, FlightMotion.calculate(0, 0, 1, false, false, false).x(), EPSILON);
        assertEquals(-.30, FlightMotion.calculate(0, 0, -1, false, false, false).x(), EPSILON);
        assertEquals(-.30, FlightMotion.calculate(0, -1, 0, false, false, false).z(), EPSILON);
    }

    @Test void diagonalsDoNotIncreaseHorizontalSpeed() {
        var motion = FlightMotion.calculate(37, 1, 1, false, false, false);
        assertEquals(.30, Math.hypot(motion.x(), motion.z()), EPSILON);
    }

    @Test void verticalKeysCancelEachOtherAndNoInputHovers() {
        assertEquals(.30, FlightMotion.calculate(0, 0, 0, true, false, false).y(), EPSILON);
        assertEquals(-.30, FlightMotion.calculate(0, 0, 0, false, true, false).y(), EPSILON);
        assertEquals(0, FlightMotion.calculate(0, 0, 0, true, true, false).y(), EPSILON);
        assertEquals(new FlightMotion.Motion(0, 0, 0), FlightMotion.calculate(0, 0, 0, false, false, false));
    }

    @Test void sprintIncreasesOnlyHorizontalSpeed() {
        var motion = FlightMotion.calculate(180, 1, -1, true, false, true);
        assertEquals(.45, Math.hypot(motion.x(), motion.z()), EPSILON);
        assertEquals(.30, motion.y(), EPSILON);
    }
}
