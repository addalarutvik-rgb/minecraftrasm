package dev.rasm.client;

/** Selects falling movement packets without changing jump/ascent packets. */
public final class NoFallRule {
    private NoFallRule() {}

    public static boolean shouldReportGround(boolean enabled, boolean alive, boolean riding,
                                            boolean flying, double fallDistance, double verticalSpeed) {
        return enabled && alive && !riding && !flying && fallDistance > 2.5F && verticalSpeed < 0;
    }
}
