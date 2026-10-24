package dev.rasm.client;

/** Player targeting uses horizontal facing, independent of camera pitch. */
public final class BackwardTargetRule {
    private BackwardTargetRule() {}
    public static boolean eligible(double dx, double dy, double dz, float turnedYaw, double reach) {
        double horizontal = Math.hypot(dx, dz);
        if (horizontal < 1e-6 || dx * dx + dy * dy + dz * dz > reach * reach) return false;
        double yaw = Math.toRadians(turnedYaw);
        return (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / horizontal > 0.5;
    }
}
