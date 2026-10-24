package dev.rasm.client;

/** A two-tick turn that returns along the same arc, without yaw wrapping. */
public final class TurnPulse {
    private int remainingTicks;
    private float originalYaw;

    public boolean start(float yaw) {
        if (active()) return false;
        originalYaw = yaw;
        remainingTicks = 2;
        return true;
    }

    public boolean active() { return remainingTicks > 0; }
    public float turnedYaw() { return originalYaw + 180.0F; }
    public float originalYaw() { return originalYaw; }

    public boolean tick() {
        return active() && --remainingTicks == 0;
    }

    public void cancel() { remainingTicks = 0; }
}
