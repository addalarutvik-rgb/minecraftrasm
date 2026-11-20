package dev.rasm.client;

/** Render-time easing, with the midpoint held until the attack tick observes it. */
public final class TurnPulse {
    public static final long LEG_NANOS = 150_000_000L;
    private boolean active, returning;
    private long started;
    private float originalYaw;
    public boolean start(float yaw, long now) {
        if (active) return false;
        originalYaw = yaw; started = now; active = true; returning = false; return true;
    }
    public boolean active() { return active; }
    public float turnedYaw() { return originalYaw + 180F; }
    public float originalYaw() { return originalYaw; }
    public boolean atPeak(long now) { return active && !returning && now - started >= LEG_NANOS; }
    public void beginReturn(long now) { if (atPeak(now)) { returning = true; started = now; } }
    public float sample(long now) {
        if (!active) return originalYaw;
        double t = Math.clamp((double)(now - started) / LEG_NANOS, 0, 1);
        double eased = t * t * (3 - 2 * t);
        if (returning && t >= 1) active = false;
        return originalYaw + (float)(180 * (returning ? 1 - eased : eased));
    }
    public void cancel() { active = false; }
}
