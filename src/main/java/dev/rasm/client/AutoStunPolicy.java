package dev.rasm.client;

/** Ground shield stun belongs to Auto Stun independently of the mace action. */
public final class AutoStunPolicy {
    private AutoStunPolicy() {}
    public static boolean shield(boolean shieldActionEnabled, boolean standaloneOn, boolean standaloneEnabled,
                                 boolean autoStunOn, boolean autoStunEnabled, boolean onGround) {
        return shieldActionEnabled && ((standaloneOn && standaloneEnabled)
            || (autoStunOn && autoStunEnabled && onGround));
    }
}
