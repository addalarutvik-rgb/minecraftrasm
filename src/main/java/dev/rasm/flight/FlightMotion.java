package dev.rasm.flight;

/** Camera-relative movement for the client flight mode, in blocks per tick. */
public final class FlightMotion {
    public record Motion(double x, double y, double z) {}
    private FlightMotion() {}

    public static Motion calculate(float yawDegrees, int forward, int strafe,
                                   boolean ascend, boolean descend, boolean sprint) {
        double length = Math.hypot(forward, strafe);
        double speed = sprint ? 0.45 : 0.30;
        double f = length == 0 ? 0 : forward / length;
        double s = length == 0 ? 0 : strafe / length;
        double yaw = Math.toRadians(yawDegrees);
        return new Motion((-Math.sin(yaw) * f + Math.cos(yaw) * s) * speed,
            ((ascend ? 1 : 0) - (descend ? 1 : 0)) * 0.30,
            (Math.cos(yaw) * f + Math.sin(yaw) * s) * speed);
    }
}
