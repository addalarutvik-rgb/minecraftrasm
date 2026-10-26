package dev.rasm.client;

/** One activation per continuous airborne descent; landing rearms it. */
public final class FallTrigger {
    private boolean used;
    public void reset() { used = false; }
    public boolean update(boolean grounded, boolean eligible) {
        if (grounded) { reset(); return false; }
        if (!eligible || used) return false;
        used = true;
        return true;
    }
}
